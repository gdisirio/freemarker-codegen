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

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TextBlockErrorLocationTest {

    private String process(String templateContent) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        Template t = new Template("test.ftlc", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    @Test
    public void testTextBlockStillNormalizesEOL() throws Exception {
        // Sanity check: with the new approach, text blocks still normalize newlines
        // to output_eol at runtime.
        assertEquals("line1\nline2\n", process("emit \"\"\"\nline1\nline2\n\"\"\"\n"));
    }

    @Test
    public void testTextBlockErrorPointsToCorrectLine() throws Exception {
        // Template: emit text block where line 3 has an undefined variable.
        // Error should point to a line number > 1, not "1" (which was the bug).
        String tmpl =
                "emit \"\"\"\n" +     // line 1
                "first line\n" +       // line 2
                "second line ${undefined_var}\n" +  // line 3 — error here
                "fourth line\n" +      // line 4
                "\"\"\"\n";            // line 5
        try {
            process(tmpl);
            fail("Expected a TemplateException for the undefined variable");
        } catch (TemplateException e) {
            // Verify the error references line 3 (where ${undefined_var} actually lives),
            // not line 1 (which was the symptom of the original bug — text-block newlines
            // had been stripped before the embedded parser saw them, so all of the
            // text-block content was treated as a single line).
            String fullMsg = e.getMessage();
            assertTrue("Error message should mention line 3: " + fullMsg,
                    fullMsg.contains("at line 3"));
        }
    }

    @Test
    public void testTextBlockNoInterpolations() throws Exception {
        // Text block without any ${...}. Just sanity check it still works.
        assertEquals("plain text\n",
                process("emit \"\"\"\nplain text\n\"\"\"\n"));
    }

    @Test
    public void testTextBlockWithMultipleInterpolations() throws Exception {
        // Multiple interpolations across multiple lines all work.
        String tmpl =
                "a = \"X\"\n" +
                "b = \"Y\"\n" +
                "emit \"\"\"\n" +
                "first ${a}\n" +
                "second ${b}\n" +
                "\"\"\"\n";
        assertEquals("first X\nsecond Y\n", process(tmpl));
    }

    @Test
    public void testTextBlockBackslashNStaysLiteral() throws Exception {
        // \n inside a text block should still represent a literal newline (\n),
        // not be normalized — only RAW newlines in the source should be normalized.
        // (Note: with default output_eol of "\n", both produce the same output;
        // this test ensures the value is correct either way.)
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        cfg.setOutputEol("<EOL>");

        // Source-level newlines (between "line1" and "line2") get normalized to <EOL>.
        // The 2-char sequence backslash + n in the text block source stays as 2 chars
        // (text blocks don't interpret backslash escapes).
        Template t = new Template("test.ftlc",
                new StringReader("emit \"\"\"\nline1\nline2 with embedded\\n\n\"\"\"\n"), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        // Expected output: line1<EOL>line2 with embedded\n<EOL>
        // (the literal 2-char "\n" sequence is preserved verbatim from the text block)
        assertEquals("line1<EOL>line2 with embedded\\n<EOL>", sw.toString());
    }
}
