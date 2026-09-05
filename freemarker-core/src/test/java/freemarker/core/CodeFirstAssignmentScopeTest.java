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

/**
 * An assignment in code-first mode has to say what scope it assigns to. A bare {@code x = 1} could only mean
 * "namespace at the top level, local inside a macro or function", and that makes moving a fragment of a template
 * into a macro silently change where the value is written.
 */
public class CodeFirstAssignmentScopeTest {

    private String run(String tpl) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        Template t = new Template("test.ftlc", new StringReader(tpl), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<String, Object>(), sw);
        return sw.toString();
    }

    private String errorOf(String tpl) {
        try {
            run(tpl);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // ---- the explicit forms work ----

    @Test
    public void testAssign() throws Exception {
        assertEquals("1", run("assign x = 1\nemit x?c\n"));
    }

    @Test
    public void testLocal() throws Exception {
        assertEquals("2", run("macro m()\nlocal y = 2\nemit y?c\nend\nm()\n"));
    }

    @Test
    public void testGlobal() throws Exception {
        assertEquals("3", run("global z = 3\nemit z?c\n"));
    }

    @Test
    public void testCompoundAndIncrementWithKeyword() throws Exception {
        assertEquals("7", run("assign x = 5\nassign x += 2\nemit x?c\n"));
        assertEquals("6", run("assign x = 5\nassign x++\nemit x?c\n"));
        assertEquals("15", run("assign f = 0xFF\nassign f &= 0x0F\nemit f?c\n"));
    }

    // ---- the bare form is rejected ----

    @Test
    public void testBareAssignmentRejected() {
        assertNotNull(errorOf("x = 1\n"));
    }

    @Test
    public void testBareCompoundAndIncrementRejected() {
        for (String stmt : new String[] { "x += 1", "x -= 1", "x *= 2", "x /= 2", "x %= 2",
                "x &= 1", "x |= 1", "x ^= 1", "x <<= 1", "x >>= 1", "x++", "x--" }) {
            assertNotNull("Expected \"" + stmt + "\" to be rejected",
                    errorOf("assign x = 8\n" + stmt + "\n"));
        }
    }

    @Test
    public void testErrorSuggestsAssignAtTopLevel() {
        String msg = errorOf("counter = 1\n");
        assertTrue(msg, msg.contains("assign counter = ..."));
        assertTrue(msg, msg.contains("\"global\""));
    }

    @Test
    public void testErrorSuggestsLocalInsideMacro() {
        String msg = errorOf("macro m()\ntemp = 1\nend\n");
        assertTrue(msg, msg.contains("local temp = ..."));
    }

    @Test
    public void testErrorSuggestsLocalInsideFunction() {
        String msg = errorOf("function f()\ntemp = 1\nreturn temp\nend\n");
        assertTrue(msg, msg.contains("local temp = ..."));
    }

    // ---- things that look like assignments but aren't ----

    @Test
    public void testNamedArgumentsStillWork() throws Exception {
        assertEquals("[x]", run("macro m(v)\nemit \"[\" + v + \"]\"\nend\nm(v = \"x\")\n"));
    }

    @Test
    public void testComparisonStillWorks() throws Exception {
        assertEquals("eq", run("assign x = 1\nif x == 1\nemit \"eq\"\nend\n"));
    }

    @Test
    public void testSettingDirectiveStillWorks() throws Exception {
        assertEquals("a\r\nb", run("setting output_eol = \"\\r\\n\"\nemit \"a\\eb\"\n"));
    }
}
