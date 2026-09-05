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
import java.nio.file.Files;
import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class FlushTargetTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private void process(String templateContent) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        cfg.setOutputTargetResolver(new FileOutputTargetResolver(tmp.getRoot()));
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        t.process(new HashMap<String, Object>(), new StringWriter());
    }

    @Test
    public void testFlushBareStillWorks() throws Exception {
        // Backward compatibility — no-arg flush still flushes default writer
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        cfg.setOutputTargetResolver(new FileOutputTargetResolver(tmp.getRoot()));
        Template t = new Template("test.ftl",
                new StringReader("emit \"hello\"\nflush\n"), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        assertEquals("hello", sw.toString());
    }

    @Test
    public void testFlushToFileMakesContentVisibleMidTemplate() throws Exception {
        // Verifies flush actually causes file content to be visible before template ends.
        // We do this via a round-trip: write to file, flush, then read back in same template.
        File f = new File(tmp.getRoot(), "flushed.txt");
        String path = f.getAbsolutePath();
        String tmpl =
                "into \"" + path + "\"\n  emit \"first\\n\"\nend\n" +
                "flush to \"" + path + "\"\n" +
                "assign x = read from \"" + path + "\"\n" +
                "into \"" + path + "_check.txt\"\n  emit \"got: \" + x\nend\n";
        process(tmpl);
        // The content of the check file should reflect the flushed first line
        File checkFile = new File(path + "_check.txt");
        assertEquals("got: first\n", new String(Files.readAllBytes(checkFile.toPath())));
    }

    @Test
    public void testFlushAll() throws Exception {
        File f1 = new File(tmp.getRoot(), "a.txt");
        File f2 = new File(tmp.getRoot(), "b.txt");
        String tmpl =
                "into \"" + f1.getAbsolutePath() + "\"\n  emit \"alpha\\n\"\nend\n" +
                "into \"" + f2.getAbsolutePath() + "\"\n  emit \"beta\\n\"\nend\n" +
                "flush all\n" +
                // After 'flush all', both files should have content readable from another reader
                "assign a = read from \"" + f1.getAbsolutePath() + "\"\n" +
                "assign b = read from \"" + f2.getAbsolutePath() + "\"\n" +
                "into \"" + tmp.getRoot().getAbsolutePath() + "/out.txt\"\n  emit a + b\nend\n";
        process(tmpl);
        assertEquals("alpha\nbeta\n",
                new String(Files.readAllBytes(new File(tmp.getRoot(), "out.txt").toPath())));
    }

    @Test
    public void testFlushToStdoutAndStderr() throws Exception {
        // Just verify they parse and run; can't easily capture in test
        process("into stdout\n  emit \"x\"\n  flush to stdout\nend\n"
                + "into stderr\n  emit \"y\"\n  flush to stderr\nend\n");
    }

    @Test
    public void testFlushUnopenedTargetOpensIt() throws Exception {
        // Calling 'flush to <path>' before any 'emit to <path>' should still work
        // (the writer gets opened lazily by getWriterForTarget, even just to flush it)
        File f = new File(tmp.getRoot(), "preflush.txt");
        String tmpl = "flush to \"" + f.getAbsolutePath() + "\"\n";
        process(tmpl);
        // File should exist (truncated empty) since flush opens the writer
        assertTrue(f.exists());
        assertEquals("", new String(Files.readAllBytes(f.toPath())));
    }

    @Test
    public void testFlushToVariableTarget() throws Exception {
        File f = new File(tmp.getRoot(), "viavar.txt");
        String tmpl =
                "assign p = \"" + f.getAbsolutePath() + "\"\n" +
                "into p\n  emit \"hello\\n\"\nend\n" +
                "flush to p\n" +
                "assign x = read from p\n" +
                "into \"" + tmp.getRoot().getAbsolutePath() + "/result.txt\"\n  emit x\nend\n";
        process(tmpl);
        assertEquals("hello\n",
                new String(Files.readAllBytes(new File(tmp.getRoot(), "result.txt").toPath())));
    }
}
