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

public class ReadFromTest {

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
    public void testReadFile() throws Exception {
        File inputFile = tmp.newFile("input.txt");
        Files.write(inputFile.toPath(), "hello world".getBytes());
        String tmpl = "x = read from \"" + inputFile.getAbsolutePath() + "\"\nemit x\n";
        assertEquals("hello world", processCodeFirst(tmpl));
    }

    @Test
    public void testReadFileMultiline() throws Exception {
        File inputFile = tmp.newFile("ml.txt");
        Files.write(inputFile.toPath(), "line1\nline2\nline3".getBytes());
        String tmpl = "x = read from \"" + inputFile.getAbsolutePath() + "\"\nemit x\n";
        assertEquals("line1\nline2\nline3", processCodeFirst(tmpl));
    }

    @Test
    public void testReadEmptyFile() throws Exception {
        File inputFile = tmp.newFile("empty.txt");
        String tmpl = "x = read from \"" + inputFile.getAbsolutePath() + "\"\nemit \"<\" + x + \">\"\n";
        assertEquals("<>", processCodeFirst(tmpl));
    }

    @Test
    public void testReadlnIterate() throws Exception {
        File inputFile = tmp.newFile("data.txt");
        Files.write(inputFile.toPath(), "a\nb\nc\n".getBytes());
        String tmpl =
                "list (readln from \"" + inputFile.getAbsolutePath() + "\") as line\n" +
                "  emit line + \"!\\n\"\n" +
                "endlist\n";
        assertEquals("a!\nb!\nc!\n", processCodeFirst(tmpl));
    }

    @Test
    public void testReadlnEmptyFile() throws Exception {
        File inputFile = tmp.newFile("empty.txt");
        String tmpl =
                "list (readln from \"" + inputFile.getAbsolutePath() + "\") as line\n" +
                "  emit \"X\"\n" +
                "endlist\n" +
                "emit \"done\"\n";
        assertEquals("done", processCodeFirst(tmpl));
    }

    @Test
    public void testReadlnLastLineNoTrailingNewline() throws Exception {
        File inputFile = tmp.newFile("nolastnl.txt");
        Files.write(inputFile.toPath(), "a\nb".getBytes());
        String tmpl =
                "list (readln from \"" + inputFile.getAbsolutePath() + "\") as line\n" +
                "  emit \"[\" + line + \"]\"\n" +
                "endlist\n";
        assertEquals("[a][b]", processCodeFirst(tmpl));
    }

    @Test
    public void testReadlnAssignAndIterate() throws Exception {
        File inputFile = tmp.newFile("data.csv");
        Files.write(inputFile.toPath(), "x\ny\n".getBytes());
        String tmpl =
                "lines = readln from \"" + inputFile.getAbsolutePath() + "\"\n" +
                "list lines as l\n" +
                "  emit l\n" +
                "endlist\n";
        assertEquals("xy", processCodeFirst(tmpl));
    }

    @Test
    public void testReadlnTwicePerformsTwoReads() throws Exception {
        File inputFile = tmp.newFile("twopass.txt");
        Files.write(inputFile.toPath(), "1\n2\n3\n".getBytes());
        String path = inputFile.getAbsolutePath();
        String tmpl =
                "list (readln from \"" + path + "\") as l\n" +
                "  emit \"a:\" + l + \"\\n\"\n" +
                "endlist\n" +
                "list (readln from \"" + path + "\") as l\n" +
                "  emit \"b:\" + l + \"\\n\"\n" +
                "endlist\n";
        assertEquals("a:1\na:2\na:3\nb:1\nb:2\nb:3\n", processCodeFirst(tmpl));
    }

    @Test
    public void testReadlnBreakStopsIteration() throws Exception {
        File inputFile = tmp.newFile("break.txt");
        Files.write(inputFile.toPath(), "1\n2\n3\n4\n5\n".getBytes());
        String tmpl =
                "list (readln from \"" + inputFile.getAbsolutePath() + "\") as l\n" +
                "  if l == \"3\"\n" +
                "    break\n" +
                "  endif\n" +
                "  emit l\n" +
                "endlist\n";
        assertEquals("12", processCodeFirst(tmpl));
    }

    @Test
    public void testReadFromVariablePath() throws Exception {
        File inputFile = tmp.newFile("via_var.txt");
        Files.write(inputFile.toPath(), "content".getBytes());
        String tmpl =
                "p = \"" + inputFile.getAbsolutePath() + "\"\n" +
                "emit read from p\n";
        assertEquals("content", processCodeFirst(tmpl));
    }
}
