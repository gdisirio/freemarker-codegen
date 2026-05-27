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

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class CodeFirstUfcsTest {

    private String processCodeFirst(String templateContent) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        Template t = new Template("test.ftlc", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    private String processClassic(String templateContent) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    @Test
    public void testUfcsNoArgs() throws Exception {
        String tmpl =
                "function shout(s)\n" +
                "  return s?upper_case\n" +
                "endfunction\n" +
                "emit \"hi\"?shout()\n";
        assertEquals("HI", processCodeFirst(tmpl));
    }

    @Test
    public void testUfcsWithArgs() throws Exception {
        String tmpl =
                "function suffix(s, x)\n" +
                "  return s + x\n" +
                "endfunction\n" +
                "emit \"hi\"?suffix(\"!\")\n";
        assertEquals("hi!", processCodeFirst(tmpl));
    }

    @Test
    public void testUfcsMultipleArgs() throws Exception {
        String tmpl =
                "function wrap3(s, a, b)\n" +
                "  return a + s + b\n" +
                "endfunction\n" +
                "emit \"X\"?wrap3(\"[\", \"]\")\n";
        assertEquals("[X]", processCodeFirst(tmpl));
    }

    @Test
    public void testUfcsChaining() throws Exception {
        String tmpl =
                "function trimmed(s)\n" +
                "  return s?trim\n" +
                "endfunction\n" +
                "function shout(s)\n" +
                "  return s?upper_case\n" +
                "endfunction\n" +
                "emit \"  hi  \"?trimmed()?shout()\n";
        assertEquals("HI", processCodeFirst(tmpl));
    }

    @Test
    public void testBuiltinStillWins() throws Exception {
        // ?upper_case is a real built-in; UFCS must not shadow it even if a
        // function of the same name exists.
        String tmpl =
                "function upper_case(s)\n" +
                "  return \"WRONG\"\n" +
                "endfunction\n" +
                "emit \"hi\"?upper_case\n";
        assertEquals("HI", processCodeFirst(tmpl));
    }

    @Test
    public void testUfcsViaImportedNamespace() throws Exception {
        // x?u.foo() must resolve to the imported namespace's function: u.foo(x).
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("lib.ftlc",
                "function fmt(s)\n  return \"<\" + s + \">\"\nendfunction\n");
        loader.putTemplate("main.ftlc",
                "import \"lib.ftlc\" as u\nemit \"hi\"?u.fmt()\n");
        cfg.setTemplateLoader(loader);
        StringWriter sw = new StringWriter();
        cfg.getTemplate("main.ftlc").process(new HashMap<String, Object>(), sw);
        assertEquals("<hi>", sw.toString());
    }

    @Test
    public void testUfcsImportedNamespaceResolvesLibInternals() throws Exception {
        // A library function called via UFCS that itself uses UFCS internally must
        // resolve its bare names against its own (library) namespace, not the caller's.
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("lib.ftlc",
                "function helper(s)\n  return s?trim\n endfunction\n" +
                "function process(s)\n  return s?helper()?upper_case\n endfunction\n");
        loader.putTemplate("main.ftlc",
                "import \"lib.ftlc\" as u\nemit \"  hi  \"?u.process()\n");
        cfg.setTemplateLoader(loader);
        StringWriter sw = new StringWriter();
        cfg.getTemplate("main.ftlc").process(new HashMap<String, Object>(), sw);
        assertEquals("HI", sw.toString());
    }

    @Test
    public void testUfcsNotAvailableInClassicMode() throws Exception {
        // In classic mode, ?someUnknownThing must remain an "unknown built-in"
        // parse error, not a UFCS call.
        String tmpl =
                "<#function shout(s)><#return s?upper_case></#function>" +
                "${\"hi\"?shout()}";
        try {
            processClassic(tmpl);
            fail("Expected a parse error: UFCS must not be available in classic mode");
        } catch (Exception e) {
            String msg = e.getMessage();
            assertTrue("Expected 'unknown built-in' style error, got: " + msg,
                    msg.toLowerCase().contains("built-in") || msg.toLowerCase().contains("builtin"));
        }
    }

    @Test
    public void testUnknownNameStillErrorsInCodeFirst() throws Exception {
        // In code-first mode, ?name where name is neither a built-in nor a
        // defined function should still fail (at runtime, as an undefined call).
        String tmpl = "emit \"hi\"?nonexistent()\n";
        try {
            processCodeFirst(tmpl);
            fail("Expected an error for undefined function used via UFCS");
        } catch (Exception e) {
            // expected — nonexistent is not defined
        }
    }
}
