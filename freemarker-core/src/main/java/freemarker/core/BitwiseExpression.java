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
 * Binary bitwise operations: AND, OR, XOR, left shift, right shift.
 * Only available in code-first mode.
 */
final class BitwiseExpression extends Expression {

    static final int TYPE_AND = 0;
    static final int TYPE_OR = 1;
    static final int TYPE_XOR = 2;
    static final int TYPE_LEFT_SHIFT = 3;
    static final int TYPE_RIGHT_SHIFT = 4;

    private static final String[] OPERATOR_IMAGES = { "&", "|", "^", "<<", ">>" };

    private final Expression lho;
    private final Expression rho;
    private final int operator;

    BitwiseExpression(Expression lho, Expression rho, int operator) {
        this.lho = lho;
        this.rho = rho;
        this.operator = operator;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        Number lhoNumber = lho.evalToNumber(env);
        Number rhoNumber = rho.evalToNumber(env);
        long left = lhoNumber.longValue();
        long right = rhoNumber.longValue();
        long result;
        switch (operator) {
            case TYPE_AND:
                result = left & right;
                break;
            case TYPE_OR:
                result = left | right;
                break;
            case TYPE_XOR:
                result = left ^ right;
                break;
            case TYPE_LEFT_SHIFT:
                result = left << right;
                break;
            case TYPE_RIGHT_SHIFT:
                result = left >> right;
                break;
            default:
                throw new _MiscTemplateException(this, "Unknown bitwise operation: ", Integer.valueOf(operator));
        }
        if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
            return new SimpleNumber(Integer.valueOf((int) result));
        }
        return new SimpleNumber(Long.valueOf(result));
    }

    @Override
    public String getCanonicalForm() {
        return lho.getCanonicalForm() + ' ' + OPERATOR_IMAGES[operator] + ' ' + rho.getCanonicalForm();
    }

    @Override
    String getNodeTypeSymbol() {
        return OPERATOR_IMAGES[operator];
    }

    @Override
    boolean isLiteral() {
        return constantValue != null || (lho.isLiteral() && rho.isLiteral());
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new BitwiseExpression(
                lho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                rho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                operator);
    }

    @Override
    int getParameterCount() {
        return 3;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return lho;
        case 1: return rho;
        case 2: return Integer.valueOf(operator);
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.LEFT_HAND_OPERAND;
        case 1: return ParameterRole.RIGHT_HAND_OPERAND;
        case 2: return ParameterRole.AST_NODE_SUBTYPE;
        default: throw new IndexOutOfBoundsException();
        }
    }
}
