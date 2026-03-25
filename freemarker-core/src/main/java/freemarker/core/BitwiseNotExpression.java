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

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Unary bitwise NOT operator (~).
 * Only available in code-first mode.
 */
final class BitwiseNotExpression extends Expression {

    private final Expression target;

    BitwiseNotExpression(Expression target) {
        this.target = target;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        Number number = target.evalToNumber(env);
        long value = number.longValue();
        long result = ~value;
        if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
            return new SimpleNumber(Integer.valueOf((int) result));
        }
        return new SimpleNumber(Long.valueOf(result));
    }

    @Override
    public String getCanonicalForm() {
        return "~" + target.getCanonicalForm();
    }

    @Override
    String getNodeTypeSymbol() {
        return "~";
    }

    @Override
    boolean isLiteral() {
        return target.isLiteral();
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new BitwiseNotExpression(
                target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }

    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return target;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.RIGHT_HAND_OPERAND;
    }
}
