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

public class OutputEOLTest {

    private String processCodeFirst(String templateContent, String outputEOL) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        if (outputEOL != null) {
            cfg.setOutputEOL(outputEOL);
        }
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    private String processClassic(String templateContent, String outputEOL) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        if (outputEOL != null) {
            cfg.setOutputEOL(outputEOL);
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
                processCodeFirst("x = \"world\"\nemit \"hello\\e${x}\\e\"\n", null));
    }

    @Test
    public void testTemplateSettingOutputEOL() throws Exception {
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
    public void testDefaultOutputEOL() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        assertEquals("\n", cfg.getOutputEOL());
    }
}
