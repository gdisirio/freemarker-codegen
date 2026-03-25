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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class CodeFirstCommentTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setCodeFirstMode(true);
        return cfg;
    }

    @Test
    public void testEmptyTemplate() throws IOException, TemplateException {
        assertOutput("", "");
    }

    @Test
    public void testBlankLines() throws IOException, TemplateException {
        assertOutput("\n\n\n", "");
    }

    @Test
    public void testLineCommentEmpty() throws IOException, TemplateException {
        assertOutput("//\n", "");
    }

    @Test
    public void testLineComment() throws IOException, TemplateException {
        assertOutput("// this is a comment\n", "");
    }

    @Test
    public void testLineCommentNoTrailingNewline() throws IOException, TemplateException {
        assertOutput("// this is a comment", "");
    }

    @Test
    public void testMultipleLineComments() throws IOException, TemplateException {
        assertOutput("// comment 1\n// comment 2\n// comment 3\n", "");
    }

    @Test
    public void testBlockComment() throws IOException, TemplateException {
        assertOutput("/* this is a block comment */", "");
    }

    @Test
    public void testMultilineBlockComment() throws IOException, TemplateException {
        assertOutput("/* this is\na multiline\nblock comment */", "");
    }

    @Test
    public void testBlockCommentFollowedByLineComment() throws IOException, TemplateException {
        assertOutput("/* block */\n// line\n", "");
    }

    @Test
    public void testCommentsWithWhitespace() throws IOException, TemplateException {
        assertOutput("  // indented comment\n  /* indented block */\n", "");
    }

    @Test
    public void testBlockCommentWithStars() throws IOException, TemplateException {
        assertOutput("/* ** stars ** */", "");
    }

    @Test
    public void testBlockCommentWithSlashes() throws IOException, TemplateException {
        assertOutput("/* // slashes // */", "");
    }

    @Test
    public void testFtlHeaderWithCodeFirst() throws IOException, TemplateException {
        // Using ftl header syntax to activate code-first in a non-code-first config.
        // We create a template directly with a non-code-first config.
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        // The <#ftl> header is parsed in classic mode, then switches to code-first
        Template t = new Template("test.ftl", "<#ftl syntax='code-first'>\n// comment\n", cfg);
        StringWriter sw = new StringWriter();
        t.process(null, sw);
        assertEquals("", sw.toString());
    }
}
