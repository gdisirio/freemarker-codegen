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
 * Tests the block closers of code-first mode: the generic {@code end}, which closes whatever block is open, and the
 * {@code /something} forms, which also state (and therefore check) what is being closed.
 */
public class CodeFirstBlockEndTest {

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

    // ---- the generic "end" closes every kind of block ----

    @Test
    public void testEndClosesIf() throws Exception {
        assertEquals("a", run("if true\nemit \"a\"\nend\n"));
        assertEquals("b", run("if false\nemit \"a\"\nelse\nemit \"b\"\nend\n"));
    }

    @Test
    public void testEndClosesList() throws Exception {
        assertEquals("12", run("list [1, 2] as i\nemit i?c\nend\n"));
    }

    @Test
    public void testEndClosesMacroAndFunction() throws Exception {
        assertEquals("m", run("macro m()\nemit \"m\"\nend\nm()\n"));
        assertEquals("2", run("function f(n)\nreturn n * 2\nend\nemit f(1)?c\n"));
    }

    @Test
    public void testEndClosesSwitch() throws Exception {
        assertEquals("one", run("switch 1\ncase 1\nemit \"one\"\nbreak\nend\n"));
    }

    @Test
    public void testEndClosesAttempt() throws Exception {
        assertEquals("a", run("attempt\nemit \"a\"\nrecover\nemit \"r\"\nend\n"));
    }

    @Test
    public void testEndClosesNestedBlocks() throws Exception {
        assertEquals("1a2a", run("list [1, 2] as i\nemit i?c\nif true\nemit \"a\"\nend\nend\n"));
    }

    // ---- the explicit "/something" form still works, for every block ----

    @Test
    public void testExplicitClosers() throws Exception {
        assertEquals("a", run("if true\nemit \"a\"\n/if\n"));
        assertEquals("12", run("list [1, 2] as i\nemit i?c\n/list\n"));
        assertEquals("m", run("macro m()\nemit \"m\"\n/macro\nm()\n"));
        assertEquals("2", run("function f(n)\nreturn n * 2\n/function\nemit f(1)?c\n"));
        assertEquals("one", run("switch 1\ncase 1\nemit \"one\"\nbreak\n/switch\n"));
    }

    @Test
    public void testExplicitClosersThatUsedToBeMissing() throws Exception {
        // /attempt, /items, /autoesc and /noautoesc were documented but had no token, so they
        // didn't parse; only the endattempt-style keywords worked.
        assertEquals("a", run("attempt\nemit \"a\"\nrecover\nemit \"r\"\n/attempt\n"));
        // The "items" form requires the parent "list" to have no loop variable of its own.
        assertEquals("<1>", run("list [1]\nemit \"<\"\nitems as j\nemit j?c\n/items\nemit \">\"\n/list\n"));
    }

    @Test
    public void testExplicitCloserIsChecked() {
        // This is what the explicit form buys over the generic one.
        assertTrue(errorOf("function f()\nreturn 1\n/macro\n").contains("not /macro"));
        assertTrue(errorOf("macro m()\nemit \"m\"\n/function\n").contains("not /function"));
    }

    // ---- the old per-block keywords are gone ----

    @Test
    public void testOldEndKeywordsAreRejected() {
        for (String kw : new String[] { "endif", "endlist", "endmacro", "endfunction", "endswitch" }) {
            String tpl = kw.equals("endmacro") || kw.equals("endfunction")
                    ? kw.substring(3) + " x()\nemit \"a\"\n" + kw + "\n"
                    : "if true\nemit \"a\"\n" + kw + "\n";
            assertNotNull("Expected " + kw + " to be rejected", errorOf(tpl));
        }
    }

    @Test
    public void testEndIsNotReservedInExpressions() throws Exception {
        // "end" is only a closer at the start of a statement, so it stays usable as a name.
        assertEquals("3", run("assign end = 3\nemit end?c\n"));
    }
}
