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
import freemarker.template.TemplateException;

public class IOEdgeCasesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String process(String templateContent) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    @Test
    public void testRoundTripWriteThenRead() throws Exception {
        File f = new File(tmp.getRoot(), "rt.txt");
        String tmpl =
                "p = \"" + f.getAbsolutePath() + "\"\n" +
                "emit \"hello world\\n\" to p\n" +
                "x = read from p\n" +
                "emit x\n";
        // Note: emit-to writes to the file, then read pulls it back
        // The file is closed at template end, but read can still open it fresh
        assertEquals("hello world\n", process(tmpl));
    }

    @Test
    public void testPathNormalizationDeduplicates() throws Exception {
        File f = new File(tmp.getRoot(), "norm.txt");
        // "foo/../norm.txt" and "norm.txt" should resolve to the same handle
        String dir = tmp.getRoot().getAbsolutePath();
        String path1 = dir + "/norm.txt";
        String path2 = dir + "/./norm.txt";
        String path3 = dir + "/sub/../norm.txt";
        String tmpl =
                "emit \"a\\n\" to \"" + path1 + "\"\n" +
                "emit \"b\\n\" to \"" + path2 + "\"\n" +
                "emit \"c\\n\" to \"" + path3 + "\"\n";
        process(tmpl);
        // All three should write to the same file, in order
        assertEquals("a\nb\nc\n", new String(Files.readAllBytes(f.toPath())));
    }

    @Test(expected = TemplateException.class)
    public void testReadNonExistentFileFails() throws Exception {
        String tmpl = "x = read from \"/nonexistent/path/file.xyz\"\nemit x\n";
        process(tmpl);
    }

    @Test
    public void testEmitToStderr() throws Exception {
        // Just verify it parses and runs without error; can't easily capture stderr in test
        String tmpl = "emit \"diagnostic\\n\" to stderr\nemit \"main\" to default\n";
        assertEquals("main", process(tmpl));
    }

    @Test
    public void testEmitToStdout() throws Exception {
        // Same — verify it doesn't crash
        String tmpl = "emit \"to_default\" to default\n";
        assertEquals("to_default", process(tmpl));
    }

    @Test
    public void testMultiFileGeneration() throws Exception {
        // Common code-gen pattern: emit headers and source in single template
        File header = new File(tmp.getRoot(), "out.h");
        File source = new File(tmp.getRoot(), "out.c");
        String h = header.getAbsolutePath();
        String s = source.getAbsolutePath();
        String tmpl =
                "emit \"#ifndef OUT_H\\n\" to \"" + h + "\"\n" +
                "emit \"#define OUT_H\\n\" to \"" + h + "\"\n" +
                "emit \"#include \\\"out.h\\\"\\n\" to \"" + s + "\"\n" +
                "list [\"foo\", \"bar\"] as f\n" +
                "  emit \"void ${f}(void);\\n\" to \"" + h + "\"\n" +
                "  emit \"void ${f}(void) {}\\n\" to \"" + s + "\"\n" +
                "endlist\n" +
                "emit \"#endif\\n\" to \"" + h + "\"\n";
        process(tmpl);
        assertEquals("#ifndef OUT_H\n#define OUT_H\nvoid foo(void);\nvoid bar(void);\n#endif\n",
                new String(Files.readAllBytes(header.toPath())));
        assertEquals("#include \"out.h\"\nvoid foo(void) {}\nvoid bar(void) {}\n",
                new String(Files.readAllBytes(source.toPath())));
    }

    @Test
    public void testCreatesParentDirectories() throws Exception {
        File deep = new File(tmp.getRoot(), "a/b/c/file.txt");
        String tmpl = "emit \"hi\" to \"" + deep.getAbsolutePath() + "\"\n";
        process(tmpl);
        assertEquals("hi", new String(Files.readAllBytes(deep.toPath())));
    }

    @Test
    public void testReadlnWithFilterAndMap() throws Exception {
        // Practical: filter blank lines, transform remaining
        File f = tmp.newFile("data.txt");
        Files.write(f.toPath(), "alpha\n\nbeta\n   \ngamma\n".getBytes());
        String tmpl =
                "list (readln from \"" + f.getAbsolutePath() + "\")?filter(l -> l?trim?length > 0)?map(l -> l?upper_case) as l\n" +
                "  emit l + \",\"\n" +
                "endlist\n";
        assertEquals("ALPHA,BETA,GAMMA,", process(tmpl));
    }

    @Test
    public void testEmitToWithEOLEscape() throws Exception {
        // \e in emit-to should resolve to output_eol
        File f = new File(tmp.getRoot(), "eol.txt");
        String tmpl = "emit \"line1\\eline2\\e\" to \"" + f.getAbsolutePath() + "\"\n";
        process(tmpl);
        assertEquals("line1\nline2\n", new String(Files.readAllBytes(f.toPath())));
    }
}
