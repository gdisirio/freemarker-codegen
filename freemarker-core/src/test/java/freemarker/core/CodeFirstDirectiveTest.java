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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleSequence;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class CodeFirstDirectiveTest {

    private String processCodeFirst(String templateContent, Map<String, Object> dataModel) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(dataModel, sw);
        return sw.toString();
    }

    private String processCodeFirst(String templateContent) throws Exception {
        return processCodeFirst(templateContent, new HashMap<String, Object>());
    }

    // === IF tests ===

    @Test
    public void testIfTrue() throws Exception {
        // Currently no text output mechanism, so if-block produces empty output
        // We verify it parses without error
        assertEquals("", processCodeFirst("if true\n/if\n"));
    }

    @Test
    public void testIfFalse() throws Exception {
        assertEquals("", processCodeFirst("if false\n/if\n"));
    }

    @Test
    public void testIfElse() throws Exception {
        assertEquals("", processCodeFirst("if true\nelse\n/if\n"));
    }

    @Test
    public void testIfElseif() throws Exception {
        assertEquals("", processCodeFirst("if false\nelseif true\n/if\n"));
    }

    @Test
    public void testIfElseifElse() throws Exception {
        assertEquals("", processCodeFirst("if false\nelseif false\nelse\n/if\n"));
    }

    // === LIST tests ===

    @Test
    public void testListBasic() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("items", Arrays.asList("a", "b", "c"));
        assertEquals("", processCodeFirst("list items as item\n/list\n", model));
    }

    @Test
    public void testListWithElse() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("items", Arrays.asList());
        assertEquals("", processCodeFirst("list items as item\nelse\n/list\n", model));
    }

    // === MACRO tests ===

    @Test
    public void testMacroDefinition() throws Exception {
        assertEquals("", processCodeFirst("macro greet(name)\n/macro\n"));
    }

    @Test
    public void testFunctionDefinition() throws Exception {
        assertEquals("", processCodeFirst("function add(a, b)\n/function\n"));
    }

    @Test
    public void testMacroWithDefaults() throws Exception {
        assertEquals("", processCodeFirst("macro greet(name=\"world\")\n/macro\n"));
    }

    // === ASSIGNMENT tests ===

    @Test
    public void testSimpleAssignment() throws Exception {
        assertEquals("", processCodeFirst("x = 1\n"));
    }

    @Test
    public void testGlobalAssignment() throws Exception {
        assertEquals("", processCodeFirst("global x = 1\n"));
    }

    @Test
    public void testAssignKeyword() throws Exception {
        assertEquals("", processCodeFirst("assign x = 1\n"));
    }

    @Test
    public void testLocalKeyword() throws Exception {
        assertEquals("", processCodeFirst("macro m()\nlocal x = 1\n/macro\n"));
    }

    @Test
    public void testAssignPlusEquals() throws Exception {
        assertEquals("", processCodeFirst("assign x = 1\nassign x += 2\n"));
    }

    @Test
    public void testPlusEqualsAssignment() throws Exception {
        assertEquals("", processCodeFirst("x = 1\nx += 2\n"));
    }

    @Test
    public void testPlusPlusAssignment() throws Exception {
        assertEquals("", processCodeFirst("x = 1\nx++\n"));
    }

    // === SWITCH tests ===

    @Test
    public void testSwitch() throws Exception {
        assertEquals("", processCodeFirst("x = 1\nswitch x\ncase 1\ncase 2\ndefault\n/switch\n"));
    }

    // === IMPORT tests ===

    @Test
    public void testImport() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        freemarker.cache.StringTemplateLoader stl = new freemarker.cache.StringTemplateLoader();
        stl.putTemplate("lib.ftl", "");
        cfg.setTemplateLoader(stl);
        stl.putTemplate("main.ftlc", "import \"lib.ftl\" as u\n");
        Template t = cfg.getTemplate("main.ftlc");
        StringWriter sw = new StringWriter();
        t.process(new HashMap<>(), sw);
        assertEquals("", sw.toString());
    }

    // === BREAK / CONTINUE tests ===

    @Test
    public void testBreakInList() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("items", Arrays.asList("a", "b", "c"));
        assertEquals("", processCodeFirst("list items as item\nbreak\n/list\n", model));
    }

    // === Nested blocks ===

    @Test
    public void testNestedIfInList() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("items", Arrays.asList("a", "b"));
        assertEquals("", processCodeFirst(
                "list items as item\nif true\n/if\n/list\n", model));
    }

    // === Comments mixed with directives ===

    @Test
    public void testCommentsInDirectives() throws Exception {
        assertEquals("", processCodeFirst(
                "// comment before\nif true\n// comment inside\n/if\n// comment after\n"));
    }

    // === end* aliases ===

    @Test
    public void testEndifAlias() throws Exception {
        assertEquals("", processCodeFirst("if true\nendif\n"));
    }

    @Test
    public void testEndlistAlias() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("items", Arrays.asList("a"));
        assertEquals("", processCodeFirst("list items as item\nendlist\n", model));
    }

    @Test
    public void testEndmacroAlias() throws Exception {
        assertEquals("", processCodeFirst("macro greet(name)\nendmacro\n"));
    }

    @Test
    public void testEndfunctionAlias() throws Exception {
        assertEquals("", processCodeFirst("function add(a, b)\nendfunction\n"));
    }

    @Test
    public void testEndswitchAlias() throws Exception {
        assertEquals("", processCodeFirst("x = 1\nswitch x\ncase 1\nendswitch\n"));
    }

    @Test
    public void testMixedClosingStyles() throws Exception {
        // /if and endif can be used interchangeably
        assertEquals("", processCodeFirst(
                "if true\nif true\nendif\n/if\n"));
    }

    // === Multiline expressions ===

    @Test
    public void testMultilineIfCondition() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("x", 15);
        model.put("y", 5);
        assertEquals("", processCodeFirst(
                "if (x > 10 &&\n    y < 20)\nendif\n", model));
    }

    @Test
    public void testMultilineThreeLines() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("a", true);
        model.put("b", true);
        model.put("c", true);
        assertEquals("", processCodeFirst(
                "if (a &&\n    b &&\n    c)\nendif\n", model));
    }

    // === Greater-than operators without parens ===

    @Test
    public void testGreaterThanWithoutParens() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("x", 15);
        assertEquals("", processCodeFirst("if x > 10\nendif\n", model));
    }

    @Test
    public void testGreaterThanEqualsWithoutParens() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("x", 10);
        assertEquals("", processCodeFirst("if x >= 10\nendif\n", model));
    }

    @Test
    public void testBackslashLineContinuation() throws Exception {
        // Assignment with line continuation
        assertEquals("hello world",
                processCodeFirst("x = \"hello\" + \\\n\" world\"\nemit x\n"));
    }

    @Test
    public void testBackslashLineContinuationInIf() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("a", true);
        model.put("b", true);
        assertEquals("yes",
                processCodeFirst("if a && \\\nb\n  emit \"yes\"\nendif\n", model));
    }

    @Test
    public void testBackslashLineContinuationMultiple() throws Exception {
        // Multiple continuations
        assertEquals("6",
                processCodeFirst("x = 1 + \\\n2 + \\\n3\nemit x?c\n"));
    }
}
