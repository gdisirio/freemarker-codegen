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

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;

/**
 * Eager file/stream read expression: {@code read from <target>}.
 *
 * <p>Returns the entire contents of the source as a string. For large files,
 * use {@link ReadlnExpression} instead to iterate line by line.
 *
 * <p>Targets: "stdin" (reserved keyword) or any string-valued expression
 * treated as a file path.
 *
 * @since 2.3.35
 */
final class ReadExpression extends Expression {

    private final Expression target;

    ReadExpression(Expression target) {
        this.target = target;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        String targetStr = evalTarget(env);
        BufferedReader reader = null;
        try {
            reader = env.openReaderForTarget(targetStr);
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) >= 0) {
                sb.append(buf, 0, n);
            }
            return new SimpleScalar(sb.toString());
        } catch (IOException e) {
            throw new _MiscTemplateException(this, env,
                    "Failed to read from target '", targetStr, "': ", e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
                env.unregisterReader(reader);
            }
        }
    }

    private String evalTarget(Environment env) throws TemplateException {
        TemplateModel m = target.eval(env);
        if (!(m instanceof TemplateScalarModel)) {
            throw new _MiscTemplateException(target, env,
                    "The target of 'read from' must be a string, but was: ",
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
        return new ReadExpression(
                target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }

    @Override
    public String getCanonicalForm() {
        return "read from " + target.getCanonicalForm();
    }

    @Override
    String getNodeTypeSymbol() {
        return "read";
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
