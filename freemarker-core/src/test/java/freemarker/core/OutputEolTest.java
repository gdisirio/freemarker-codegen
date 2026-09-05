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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class OutputEolTest {

    private String processCodeFirst(String templateContent, String outputEol) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        if (outputEol != null) {
            cfg.setOutputEol(outputEol);
        }
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    private String processClassic(String templateContent, String outputEol) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        if (outputEol != null) {
            cfg.setOutputEol(outputEol);
        }
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    // ---- \e escape tests ----

    @Test
    public void testBackslashEDefaultLF() throws Exception {
        assertEquals("line1\nline2\n",
                processCodeFirst("emit \"line1\\eline2\\e\"\n", null));
    }

    @Test
    public void testBackslashEWindowsEOL() throws Exception {
        assertEquals("line1\r\nline2\r\n",
                processCodeFirst("emit \"line1\\eline2\\e\"\n", "\r\n"));
    }

    @Test
    public void testBackslashECustomEOL() throws Exception {
        assertEquals("line1<BR>line2<BR>",
                processCodeFirst("emit \"line1\\eline2\\e\"\n", "<BR>"));
    }

    @Test
    public void testBackslashNUnaffected() throws Exception {
        // \n always produces \n regardless of output_eol
        assertEquals("line1\nline2\n",
                processCodeFirst("emit \"line1\\nline2\\n\"\n", "\r\n"));
    }

    @Test
    public void testBackslashEInClassicMode() throws Exception {
        // \e also works in classic mode string literals
        assertEquals("a\r\nb",
                processClassic("${\"a\\eb\"}", "\r\n"));
    }

    @Test
    public void testBackslashEWithInterpolation() throws Exception {
        assertEquals("hello\nworld\n",
                processCodeFirst("assign x = \"world\"\nemit \"hello\\e${x}\\e\"\n", null));
    }

    @Test
    public void testTemplateSettingOutputEol() throws Exception {
        assertEquals("line1\r\nline2\r\n",
                processCodeFirst("setting output_eol = \"\\r\\n\"\nemit \"line1\\eline2\\e\"\n", null));
    }

    // ---- Text block EOL normalization tests ----

    @Test
    public void testTextBlockNormalizesLF() throws Exception {
        // Text block with \n line endings → normalized to output_eol
        assertEquals("line1\nline2\n",
                processCodeFirst("emit \"\"\"\nline1\nline2\n\"\"\"\n", null));
    }

    @Test
    public void testTextBlockNormalizesToCRLF() throws Exception {
        // Text block with \n line endings, output_eol=\r\n
        assertEquals("line1\r\nline2\r\n",
                processCodeFirst("emit \"\"\"\nline1\nline2\n\"\"\"\n", "\r\n"));
    }

    @Test
    public void testTextBlockWithCRLFInput() throws Exception {
        // Text block with \r\n line endings in template → still normalized to output_eol
        assertEquals("line1\nline2\n",
                processCodeFirst("emit \"\"\"\r\nline1\r\nline2\r\n\"\"\"\n", null));
    }

    // ---- Configuration defaults ----

    @Test
    public void testDefaultOutputEol() throws Exception {
        // Not set by default, which means that the line breaks of the static text of a classic template are output
        // as they are in the template file.
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        assertNull(cfg.getOutputEol());
        assertFalse(cfg.isOutputEolSet());
    }

    @Test
    public void testUnsetStillGivesLfForEscapeAndTextBlocks() throws Exception {
        // The \e escape and the code-first text blocks fall back to a line feed when the setting isn't set, so
        // nothing changes for them by it defaulting to unset.
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        assertEquals("a\nb", process(cfg, "emit \"a\\eb\"\n"));
        assertEquals("x\ny\n", process(cfg, "emit \"\"\"\nx\ny\n\"\"\"\n"));
    }

    @Test
    public void testStaticTextOfClassicTemplateIsNormalizedWhenSet() throws Exception {
        // New: with the setting set, the template file's own line breaks don't leak into the output.
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setOutputEol("\r\n");
        assertEquals("A\r\nB\r\n", process(cfg, "A\r\nB\r\n"));
        assertEquals("A\r\nB\r\n", process(cfg, "A\nB\n"));
    }

    @Test
    public void testStaticTextOfClassicTemplateIsKeptWhenUnset() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        assertEquals("A\r\nB\r\n", process(cfg, "A\r\nB\r\n"));
        assertEquals("A\nB\n", process(cfg, "A\nB\n"));
    }

    private static String process(Configuration cfg, String templateSource) throws Exception {
        Template t = new Template(cfg.getCodeFirstMode() ? "t.ftlc" : "t.ftl",
                new StringReader(templateSource), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }
}
