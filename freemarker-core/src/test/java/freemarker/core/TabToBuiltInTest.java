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
import freemarker.template.TemplateException;

public class TabToBuiltInTest {

    private String eval(String expr) throws Exception {
        return eval(expr, new HashMap<String, Object>());
    }

    private String eval(String expr, Map<String, Object> model) throws Exception {
        String templateContent = "${" + expr + "}";
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(model, sw);
        return sw.toString();
    }

    @Test
    public void testBasicPadding() throws Exception {
        assertEquals("hello               ", eval("'hello'?tab_to(20)"));
    }

    @Test
    public void testPaddingWithCustomChar() throws Exception {
        assertEquals("hello...............", eval("'hello'?tab_to(20, '.')"));
    }

    @Test
    public void testStringAlreadyAtColumn() throws Exception {
        assertEquals("hello", eval("'hello'?tab_to(5)"));
    }

    @Test
    public void testStringPastColumn() throws Exception {
        assertEquals("hello world", eval("'hello world'?tab_to(5)"));
    }

    @Test
    public void testEmptyString() throws Exception {
        assertEquals("     ", eval("''?tab_to(5)"));
    }

    @Test
    public void testZeroColumn() throws Exception {
        assertEquals("hello", eval("'hello'?tab_to(0)"));
    }

    @Test
    public void testCamelCaseAlias() throws Exception {
        assertEquals("hello               ", eval("'hello'?tabTo(20)"));
    }

    @Test
    public void testPaddingWithDash() throws Exception {
        assertEquals("hello---", eval("'hello'?tab_to(8, '-')"));
    }

    @Test(expected = TemplateException.class)
    public void testNegativeColumnThrows() throws Exception {
        eval("'hello'?tab_to(-1)");
    }

    @Test(expected = TemplateException.class)
    public void testEmptyFillCharThrows() throws Exception {
        eval("'hello'?tab_to(20, '')");
    }

    @Test(expected = TemplateException.class)
    public void testMultiCharFillThrows() throws Exception {
        eval("'hello'?tab_to(20, 'ab')");
    }
}
