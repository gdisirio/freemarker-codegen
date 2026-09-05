/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package freemarker.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Tests of the code-first {@code into <target>} ... {@code end} block and of {@code discard}.
 */
public class IntoDirectiveTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Configuration cfg(boolean withFileTargets) {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        if (withFileTargets) {
            cfg.setOutputTargetResolver(new FileOutputTargetResolver(tmp.getRoot()));
        }
        return cfg;
    }

    private String run(Configuration cfg, String tpl) throws Exception {
        Template t = new Template("test.ftlc", new StringReader(tpl), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    private String run(String tpl) throws Exception {
        return run(cfg(true), tpl);
    }

    private String fileContent(String name) throws Exception {
        File f = new File(tmp.getRoot(), name);
        return new String(Files.readAllBytes(f.toPath()), Charset.forName("UTF-8"));
    }

    private boolean fileExists(String name) {
        return new File(tmp.getRoot(), name).exists();
    }

    private String errorOf(Configuration cfg, String tpl) {
        try {
            run(cfg, tpl);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // ---- what goes where ----

    @Test
    public void testOutputOutsideBlocksGoesToMainOutput() throws Exception {
        assertEquals("hello", run("emit \"hello\"\n"));
    }

    @Test
    public void testOutputInsideBlockGoesToTarget() throws Exception {
        assertEquals("", run("into \"a.txt\"\n  emit \"x\"\nend\n"));
        assertEquals("x", fileContent("a.txt"));
    }

    @Test
    public void testMainOutputAndTargetsAreSeparate() throws Exception {
        String main = run("emit \"m\"\ninto \"a.txt\"\n  emit \"a\"\nend\nemit \"m2\"\n");
        assertEquals("mm2", main);
        assertEquals("a", fileContent("a.txt"));
    }

    @Test
    public void testSeveralTargetsInOneRun() throws Exception {
        run("into \"h.txt\"\n  emit \"header\"\nend\ninto \"c.txt\"\n  emit \"source\"\nend\n");
        assertEquals("header", fileContent("h.txt"));
        assertEquals("source", fileContent("c.txt"));
    }

    @Test
    public void testReenteringATargetAppends() throws Exception {
        // A template can go back and forth between the files it's generating.
        run("into \"a.txt\"\n  emit \"1\"\nend\ninto \"b.txt\"\n  emit \"x\"\nend\n"
                + "into \"a.txt\"\n  emit \"2\"\nend\n");
        assertEquals("12", fileContent("a.txt"));
        assertEquals("x", fileContent("b.txt"));
    }

    @Test
    public void testNestedBlocks() throws Exception {
        run("into \"outer.txt\"\n  emit \"o1\"\n  into \"inner.txt\"\n    emit \"i\"\n  end\n  emit \"o2\"\nend\n");
        assertEquals("o1o2", fileContent("outer.txt"));
        assertEquals("i", fileContent("inner.txt"));
    }

    @Test
    public void testTargetNameCanBeAnExpression() throws Exception {
        run("assign name = \"gen\"\ninto name + \".txt\"\n  emit \"v\"\nend\n");
        assertEquals("v", fileContent("gen.txt"));
    }

    @Test
    public void testTargetInSubdirectoryIsCreated() throws Exception {
        run("into \"sub/dir/a.txt\"\n  emit \"v\"\nend\n");
        assertEquals("v", fileContent("sub/dir/a.txt"));
    }

    @Test
    public void testDirectivesWorkInsideBlock() throws Exception {
        run("into \"a.txt\"\n  list [1, 2, 3] as i\n    emit i?c\n  end\nend\n");
        assertEquals("123", fileContent("a.txt"));
    }

    @Test
    public void testTextBlockInsideBlock() throws Exception {
        run("into \"a.txt\"\n  emit \"\"\"\nline1\nline2\n\"\"\"\nend\n");
        assertEquals("line1\nline2\n", fileContent("a.txt"));
    }

    /**
     * A template that puts all of its output into blocks writes nothing to the main output — which is what FMPP's
     * dropOutputFile is for, without needing a call.
     */
    @Test
    public void testAllOutputInBlocksLeavesMainOutputEmpty() throws Exception {
        assertEquals("", run("into \"a.txt\"\n  emit \"x\"\nend\n"));
        assertEquals("x", fileContent("a.txt"));
    }

    // ---- discard ----

    @Test
    public void testDiscardRemovesTheFile() throws Exception {
        run("into \"gone.txt\"\n  emit \"x\"\n  discard\nend\n");
        assertFalse(fileExists("gone.txt"));
    }

    @Test
    public void testDiscardOnlyAffectsItsOwnTarget() throws Exception {
        run("into \"kept.txt\"\n  emit \"k\"\nend\ninto \"gone.txt\"\n  emit \"g\"\n  discard\nend\n");
        assertEquals("k", fileContent("kept.txt"));
        assertFalse(fileExists("gone.txt"));
    }

    @Test
    public void testDiscardCanBeConditional() throws Exception {
        run("assign wanted = false\ninto \"maybe.txt\"\n  emit \"x\"\n  if !wanted\n    discard\n  end\nend\n");
        assertFalse(fileExists("maybe.txt"));
        run("assign wanted = true\ninto \"maybe2.txt\"\n  emit \"x\"\n  if !wanted\n    discard\n  end\nend\n");
        assertEquals("x", fileContent("maybe2.txt"));
    }

    @Test
    public void testDiscardOutsideABlockIsAnError() {
        assertTrue(errorOf(cfg(true), "discard\n").contains("isn't inside an \"into\" block"));
    }

    // ---- targets that need no resolver, and errors ----

    @Test
    public void testStdoutAndStderrNeedNoResolver() throws Exception {
        // They always exist, so they work without the setting being configured.
        assertEquals("", run(cfg(false), "into stderr\n  emit \"\"\nend\n"));
        assertEquals("", run(cfg(false), "into stdout\n  emit \"\"\nend\n"));
    }

    @Test
    public void testUnknownTargetWithoutResolverIsAnError() {
        String msg = errorOf(cfg(false), "into \"a.txt\"\n  emit \"x\"\nend\n");
        assertTrue(msg, msg.contains("no output target called"));
        assertTrue(msg, msg.contains("output_target_resolver"));
    }

    @Test
    public void testExplicitCloserWorks() throws Exception {
        run("into \"a.txt\"\n  emit \"x\"\n/into\n");
        assertEquals("x", fileContent("a.txt"));
    }

    @Test
    public void testEmitNoLongerTakesATarget() {
        // Routing is only via "into" now; "emit ... to" is gone.
        assertNotNull(errorOf(cfg(true), "emit \"x\" to \"a.txt\"\n"));
    }
}
