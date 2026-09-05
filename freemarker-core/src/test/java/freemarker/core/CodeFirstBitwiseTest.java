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

public class CodeFirstBitwiseTest {

    private String run(String tpl) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setCodeFirstMode(true);
        Template t = new Template("test.ftl", new StringReader(tpl), cfg);
        StringWriter sw = new StringWriter();
        t.process(new HashMap<>(), sw);
        return sw.toString();
    }

    // === Bitwise AND ===

    @Test
    public void testBitwiseAnd() throws Exception {
        assertEquals("15", run("emit (0xFF & 0x0F)?c\n"));
    }

    @Test
    public void testBitwiseAndZero() throws Exception {
        assertEquals("0", run("emit (0xFF & 0x00)?c\n"));
    }

    // === Bitwise OR ===

    @Test
    public void testBitwiseOr() throws Exception {
        assertEquals("255", run("emit (0x0F | 0xF0)?c\n"));
    }

    // === Bitwise XOR ===

    @Test
    public void testBitwiseXor() throws Exception {
        assertEquals("240", run("emit (0xFF ^ 0x0F)?c\n"));
    }

    // === Bitwise NOT ===

    @Test
    public void testBitwiseNot() throws Exception {
        assertEquals("-1", run("assign x = ~0\nemit x?c\n"));
    }

    @Test
    public void testBitwiseNotFF() throws Exception {
        assertEquals("-256", run("assign x = ~0xFF\nemit x?c\n"));
    }

    // === Left Shift ===

    @Test
    public void testLeftShift() throws Exception {
        assertEquals("256", run("emit (1 << 8)?c\n"));
    }

    @Test
    public void testLeftShiftLarge() throws Exception {
        assertEquals("1048576", run("emit (1 << 20)?c\n"));
    }

    // === Right Shift ===

    @Test
    public void testRightShift() throws Exception {
        assertEquals("1", run("emit (256 >> 8)?c\n"));
    }

    // === Compound assignment ===

    @Test
    public void testAndEquals() throws Exception {
        assertEquals("15", run("assign x = 0xFF\nassign x &= 0x0F\nemit x?c\n"));
    }

    @Test
    public void testOrEquals() throws Exception {
        assertEquals("255", run("assign x = 0x0F\nassign x |= 0xF0\nemit x?c\n"));
    }

    @Test
    public void testXorEquals() throws Exception {
        assertEquals("240", run("assign x = 0xFF\nassign x ^= 0x0F\nemit x?c\n"));
    }

    @Test
    public void testLeftShiftEquals() throws Exception {
        assertEquals("256", run("assign x = 1\nassign x <<= 8\nemit x?c\n"));
    }

    @Test
    public void testRightShiftEquals() throws Exception {
        assertEquals("1", run("assign x = 256\nassign x >>= 8\nemit x?c\n"));
    }

    // === Operator precedence ===

    @Test
    public void testAndOrPrecedence() throws Exception {
        // & has higher precedence than |
        // & has higher precedence than |: 1 | (2 & 6) = 1 | 2 = 3
        assertEquals("3", run("emit (1 | 2 & 6)?c\n"));
    }

    @Test
    public void testShiftPrecedence() throws Exception {
        // << has higher precedence than &
        assertEquals("2", run("emit (0xFF & 1 << 1)?c\n"));  // 0xFF & (1 << 1) = 0xFF & 2 = 2
    }

    // === Logical vs Bitwise ===

    @Test
    public void testLogicalAndStillWorks() throws Exception {
        assertEquals("yes", run("if true && true\nemit \"yes\"\nend\n"));
    }

    @Test
    public void testLogicalOrStillWorks() throws Exception {
        assertEquals("yes", run("if false || true\nemit \"yes\"\nend\n"));
    }

    // === Combined with hex literals ===

    @Test
    public void testMaskExtraction() throws Exception {
        // Extract green channel from RGB color
        assertEquals("128", run("assign color = 0x1A803C\nassign green = (color >> 8) & 0xFF\nemit green?c\n"));
    }

    @Test
    public void testAllBitsSetMask() throws Exception {
        // 0xFFFFFFFFFFFFFFFF exceeds the range of a signed long, so it's a BigInteger.
        // Bitwise operations use the low 64 bits of the value, so an all-bits-set mask
        // behaves as it does in C/Java.
        assertEquals("255", run("emit (0xFFFFFFFFFFFFFFFF & 0xFF)?c\n"));
        assertEquals("0", run("emit (0xFFFFFFFFFFFFFFFF & 0x0)?c\n"));
    }
}
