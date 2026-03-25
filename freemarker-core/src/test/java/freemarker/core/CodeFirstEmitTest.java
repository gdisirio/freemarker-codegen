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
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class CodeFirstEmitTest {

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

    @Test
    public void testEmitStringLiteral() throws Exception {
        assertEquals("Hello", processCodeFirst("emit \"Hello\"\n"));
    }

    @Test
    public void testEmitWithInterpolation() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "World");
        assertEquals("Hello, World!", processCodeFirst("emit \"Hello, ${name}!\"\n", model));
    }

    @Test
    public void testEmitNumber() throws Exception {
        assertEquals("42", processCodeFirst("x = 42\nemit x?c\n"));
    }

    @Test
    public void testEmitInIf() throws Exception {
        assertEquals("yes", processCodeFirst("if true\nemit \"yes\"\n/if\n"));
    }

    @Test
    public void testEmitInIfElse() throws Exception {
        assertEquals("no", processCodeFirst("if false\nemit \"yes\"\nelse\nemit \"no\"\n/if\n"));
    }

    @Test
    public void testEmitInList() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("items", Arrays.asList("a", "b", "c"));
        assertEquals("abc", processCodeFirst(
                "list items as item\nemit item\n/list\n", model));
    }

    @Test
    public void testEmitMultiple() throws Exception {
        assertEquals("HelloWorld", processCodeFirst(
                "emit \"Hello\"\nemit \"World\"\n"));
    }

    @Test
    public void testEmitInMacro() throws Exception {
        assertEquals("Hi Bob", processCodeFirst(
                "macro greet(name)\nemit \"Hi ${name}\"\n/macro\n" +
                "greet(\"Bob\")\n"));
    }

    @Test
    public void testEmitInFunction() throws Exception {
        assertEquals("6", processCodeFirst(
                "function add(a, b)\nreturn a + b\n/function\n" +
                "emit add(2, 4)?c\n"));
    }

    @Test
    public void testEmitWithHex() throws Exception {
        assertEquals("255", processCodeFirst("emit 0xFF?c\n"));
    }

    // === Text block tests ===

    @Test
    public void testTextBlockBasic() throws Exception {
        assertEquals("Hello", processCodeFirst("emit \"\"\"\nHello\"\"\"\n"));
    }

    @Test
    public void testTextBlockLeadingNewlineStripped() throws Exception {
        // """ followed by EOL: first EOL is stripped
        assertEquals("Hello\n", processCodeFirst("emit \"\"\"\nHello\n\"\"\"\n"));
    }

    @Test
    public void testTextBlockInlineStart() throws Exception {
        // """ followed by content on same line: content emitted as-is
        assertEquals("Hello\n", processCodeFirst("emit \"\"\"Hello\n\"\"\"\n"));
    }

    @Test
    public void testTextBlockMultiline() throws Exception {
        assertEquals("line 1\nline 2\n", processCodeFirst(
                "emit \"\"\"\nline 1\nline 2\n\"\"\"\n"));
    }

    @Test
    public void testTextBlockWithInterpolation() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "World");
        assertEquals("Hello, World!\n", processCodeFirst(
                "emit \"\"\"\nHello, ${name}!\n\"\"\"\n", model));
    }

    @Test
    public void testTextBlockEmpty() throws Exception {
        assertEquals("", processCodeFirst("emit \"\"\"\n\"\"\"\n"));
    }

    @Test
    public void testTextBlockWhitespaceBeforeEol() throws Exception {
        // """  \n should strip the whitespace+EOL
        assertEquals("content\n", processCodeFirst("emit \"\"\"  \ncontent\n\"\"\"\n"));
    }

    @Test
    public void testTextBlockInIf() throws Exception {
        assertEquals("yes\n", processCodeFirst(
                "if true\nemit \"\"\"\nyes\n\"\"\"\n/if\n"));
    }

    @Test
    public void testTextBlockInList() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("items", Arrays.asList("a", "b"));
        assertEquals("item: a\nitem: b\n", processCodeFirst(
                "list items as item\nemit \"\"\"\nitem: ${item}\n\"\"\"\n/list\n", model));
    }
}
