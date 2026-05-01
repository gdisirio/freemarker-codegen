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

public class EmitToTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String processCodeFirst(String templateContent) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    @Test
    public void testEmitWithoutTarget() throws Exception {
        // Backward compatibility — no target writes to default
        assertEquals("hello", processCodeFirst("emit \"hello\"\n"));
    }

    @Test
    public void testEmitToDefault() throws Exception {
        // Explicit default
        assertEquals("hello", processCodeFirst("emit \"hello\" to default\n"));
    }

    @Test
    public void testEmitToFile() throws Exception {
        File outFile = new File(tmp.getRoot(), "out.txt");
        String tmpl = "emit \"line1\\n\" to \"" + outFile.getAbsolutePath() + "\"\n"
                    + "emit \"line2\\n\" to \"" + outFile.getAbsolutePath() + "\"\n";
        processCodeFirst(tmpl);
        String content = new String(Files.readAllBytes(outFile.toPath()));
        assertEquals("line1\nline2\n", content);
    }

    @Test
    public void testEmitToFileTextBlock() throws Exception {
        File outFile = new File(tmp.getRoot(), "block.txt");
        String tmpl = "emit \"\"\"\nhello\nworld\n\"\"\" to \"" + outFile.getAbsolutePath() + "\"\n";
        processCodeFirst(tmpl);
        String content = new String(Files.readAllBytes(outFile.toPath()));
        assertEquals("hello\nworld\n", content);
    }

    @Test
    public void testMultipleEmitsToSameFileInOrder() throws Exception {
        File outFile = new File(tmp.getRoot(), "ordered.txt");
        String path = outFile.getAbsolutePath();
        String tmpl =
                "emit \"a\\n\" to \"" + path + "\"\n" +
                "emit \"b\\n\" to \"" + path + "\"\n" +
                "emit \"c\\n\" to \"" + path + "\"\n";
        processCodeFirst(tmpl);
        assertEquals("a\nb\nc\n", new String(Files.readAllBytes(outFile.toPath())));
    }

    @Test
    public void testEmitToFileTruncatesExisting() throws Exception {
        File outFile = new File(tmp.getRoot(), "truncate.txt");
        Files.write(outFile.toPath(), "old content\n".getBytes());
        String tmpl = "emit \"new\\n\" to \"" + outFile.getAbsolutePath() + "\"\n";
        processCodeFirst(tmpl);
        assertEquals("new\n", new String(Files.readAllBytes(outFile.toPath())));
    }

    @Test
    public void testEmitToVariablePath() throws Exception {
        File outFile = new File(tmp.getRoot(), "via_var.txt");
        String tmpl =
                "p = \"" + outFile.getAbsolutePath() + "\"\n" +
                "emit \"hello\\n\" to p\n";
        processCodeFirst(tmpl);
        assertEquals("hello\n", new String(Files.readAllBytes(outFile.toPath())));
    }

    @Test
    public void testEmitToFileWithInterpolation() throws Exception {
        File outFile = new File(tmp.getRoot(), "interp.txt");
        String tmpl =
                "name = \"World\"\n" +
                "emit \"Hello, ${name}!\\n\" to \"" + outFile.getAbsolutePath() + "\"\n";
        processCodeFirst(tmpl);
        assertEquals("Hello, World!\n", new String(Files.readAllBytes(outFile.toPath())));
    }
}
