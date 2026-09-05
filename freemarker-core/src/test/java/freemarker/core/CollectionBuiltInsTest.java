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

public class CollectionBuiltInsTest {

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

    private File makeFile(String name, String content) throws Exception {
        File f = tmp.newFile(name);
        Files.write(f.toPath(), content.getBytes());
        return f;
    }

    @Test
    public void testFirstOnCollection() throws Exception {
        File f = makeFile("data.txt", "alpha\nbeta\ngamma\n");
        String tmpl = "assign lines = readln from \"" + f.getAbsolutePath() + "\"\nemit lines?first\n";
        assertEquals("alpha", process(tmpl));
    }

    @Test
    public void testJoinOnCollection() throws Exception {
        File f = makeFile("data.txt", "a\nb\nc\n");
        String tmpl = "assign lines = readln from \"" + f.getAbsolutePath() + "\"\nemit lines?join(\",\")\n";
        assertEquals("a,b,c", process(tmpl));
    }

    @Test
    public void testFilterOnCollection() throws Exception {
        File f = makeFile("data.txt", "apple\nbanana\napricot\ncherry\n");
        String tmpl =
                "assign lines = readln from \"" + f.getAbsolutePath() + "\"\n" +
                "list lines?filter(l -> l?starts_with(\"a\")) as l\n" +
                "  emit l + \"\\n\"\n" +
                "end\n";
        assertEquals("apple\napricot\n", process(tmpl));
    }

    @Test
    public void testMapOnCollection() throws Exception {
        File f = makeFile("data.txt", "a\nb\nc\n");
        String tmpl =
                "assign lines = readln from \"" + f.getAbsolutePath() + "\"\n" +
                "list lines?map(l -> l?upper_case) as l\n" +
                "  emit l\n" +
                "end\n";
        assertEquals("ABC", process(tmpl));
    }

    @Test
    public void testTakeWhileOnCollection() throws Exception {
        File f = makeFile("data.txt", "a\nb\nSTOP\nc\nd\n");
        String tmpl =
                "assign lines = readln from \"" + f.getAbsolutePath() + "\"\n" +
                "list lines?take_while(l -> l != \"STOP\") as l\n" +
                "  emit l\n" +
                "end\n";
        assertEquals("ab", process(tmpl));
    }

    @Test
    public void testDropWhileOnCollection() throws Exception {
        File f = makeFile("data.txt", "header1\nheader2\n---\na\nb\n");
        String tmpl =
                "assign lines = readln from \"" + f.getAbsolutePath() + "\"\n" +
                "list lines?drop_while(l -> l != \"---\")?filter(l -> l != \"---\") as l\n" +
                "  emit l + \"\\n\"\n" +
                "end\n";
        assertEquals("a\nb\n", process(tmpl));
    }

    @Test
    public void testSequenceMaterializesCollection() throws Exception {
        File f = makeFile("data.txt", "a\nb\nc\n");
        String tmpl =
                "assign lines = (readln from \"" + f.getAbsolutePath() + "\")?sequence\n" +
                "emit lines?size?c\n";
        assertEquals("3", process(tmpl));
    }

    @Test
    public void testFilterPipelineLazy() throws Exception {
        // Multiple chained filters/maps should still iterate the source once
        File f = makeFile("data.txt", "1\n2\n3\n4\n5\n6\n");
        String tmpl =
                "assign lines = readln from \"" + f.getAbsolutePath() + "\"\n" +
                "list lines?filter(l -> l?number % 2 == 0)?map(l -> l + \"!\") as l\n" +
                "  emit l\n" +
                "end\n";
        assertEquals("2!4!6!", process(tmpl));
    }
}
