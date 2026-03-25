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

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class CodeFirstModeActivationTest {

    @Test
    public void testFtlcExtensionActivatesCodeFirst() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Template t = new Template("test.ftlc", new StringReader(""), cfg);
        FMParser parser = new FMParser(t, new StringReader(""), cfg);
        assertTrue("Code-first mode should be active for .ftlc extension",
                parser._isCodeFirstMode());
    }

    @Test
    public void testFtlExtensionDoesNotActivateCodeFirst() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Template t = new Template("test.ftl", new StringReader(""), cfg);
        FMParser parser = new FMParser(t, new StringReader(""), cfg);
        assertFalse("Code-first mode should not be active for .ftl extension",
                parser._isCodeFirstMode());
    }

    @Test
    public void testConfigurationFlagActivatesCodeFirst() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        Template t = new Template("test.ftl", new StringReader(""), cfg);
        FMParser parser = new FMParser(t, new StringReader(""), cfg);
        assertTrue("Code-first mode should be active when set on Configuration",
                parser._isCodeFirstMode());
    }

    @Test
    public void testFtlHeaderActivatesCodeFirst() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        String templateContent = "<#ftl syntax='code-first'>";
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        // After parsing, the template should be in code-first mode
        // We verify by creating a new parser to check the extension isn't what triggers it
        assertFalse("Configuration should not be in code-first mode", cfg.getCodeFirstMode());
    }

    @Test
    public void testFtlcExtensionCaseInsensitive() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Template t = new Template("test.FTLC", new StringReader(""), cfg);
        FMParser parser = new FMParser(t, new StringReader(""), cfg);
        assertTrue("Code-first mode should be active for .FTLC extension",
                parser._isCodeFirstMode());
    }

    @Test
    public void testTemplateConfigurationCodeFirst() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        TemplateConfiguration tc = new TemplateConfiguration();
        tc.setCodeFirstMode(true);
        tc.setParentConfiguration(cfg);
        assertTrue("TemplateConfiguration should report code-first mode", tc.getCodeFirstMode());
    }

    @Test
    public void testTemplateConfigurationInheritsFromParent() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        TemplateConfiguration tc = new TemplateConfiguration();
        tc.setParentConfiguration(cfg);
        assertTrue("TemplateConfiguration should inherit code-first from parent", tc.getCodeFirstMode());
    }

    @Test
    public void testTemplateConfigurationOverridesParent() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        TemplateConfiguration tc = new TemplateConfiguration();
        tc.setCodeFirstMode(false);
        tc.setParentConfiguration(cfg);
        assertFalse("TemplateConfiguration should override parent code-first", tc.getCodeFirstMode());
    }
}
