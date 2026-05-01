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

import java.io.BufferedReader;
import java.io.IOException;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;

/**
 * Lazy line-by-line read expression: {@code readln from <target>}.
 *
 * <p>Returns a {@link freemarker.template.TemplateCollectionModel} that yields
 * lines of the source one at a time. The underlying reader is opened on first
 * iteration and closed automatically when iteration completes (reaches EOF or
 * {@code break}s out of the loop). Each call to this expression opens a fresh
 * reader; multiple {@code readln from} calls on the same path are independent.
 *
 * <p>Returned sequences support iteration (via {@code list}) and streaming-friendly
 * built-ins ({@code ?filter}, {@code ?map}, {@code ?take}, {@code ?drop},
 * {@code ?first}, {@code ?count}, {@code ?join}, {@code ?reduce}). To get a
 * regular sequence with {@code ?size} and random access, materialize with
 * {@code ?sequence}.
 *
 * <p>Targets: "stdin" (reserved keyword) or any string-valued expression
 * treated as a file path.
 *
 * @since 2.3.35
 */
final class ReadlnExpression extends Expression {

    private final Expression target;

    ReadlnExpression(Expression target) {
        this.target = target;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        String targetStr = evalTarget(env);
        try {
            BufferedReader reader = env.openReaderForTarget(targetStr);
            return new LazyLineCollectionModel(reader, env);
        } catch (IOException e) {
            throw new _MiscTemplateException(this, env,
                    "Failed to open target '", targetStr, "' for reading: ", e.getMessage());
        }
    }

    private String evalTarget(Environment env) throws TemplateException {
        TemplateModel m = target.eval(env);
        if (!(m instanceof TemplateScalarModel)) {
            throw new _MiscTemplateException(target, env,
                    "The target of 'readln from' must be a string, but was: ",
                    m == null ? "null" : m.getClass().getName());
        }
        return ((TemplateScalarModel) m).getAsString();
    }

    @Override
    boolean isLiteral() {
        return false;
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new ReadlnExpression(
                target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }

    @Override
    public String getCanonicalForm() {
        return "readln from " + target.getCanonicalForm();
    }

    @Override
    String getNodeTypeSymbol() {
        return "readln";
    }

    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx == 0) return target;
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx == 0) return ParameterRole.VALUE;
        throw new IndexOutOfBoundsException();
    }
}
