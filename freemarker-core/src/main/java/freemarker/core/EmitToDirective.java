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

import java.io.IOException;
import java.io.Writer;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.StringUtil;

/**
 * Code-first {@code emit <expr> to <target>} directive. Writes the value of {@code expr}
 * to the writer associated with {@code target}.
 *
 * <p>Target is itself an expression. Special string values "default", "stdout", "stderr"
 * resolve to predefined writers. Any other string value is treated as a file path; the
 * file is opened lazily on first use (in truncate mode), and the handle is cached for
 * the rest of the template processing.
 *
 * @since 2.3.35
 */
final class EmitToDirective extends TemplateElement {

    /**
     * Sentinel used to mark that no target was specified at the AST level
     * (used internally; in practice, no-target emit goes through {@link DollarVariable}).
     */
    static final String IMPLICIT_DEFAULT_TARGET = Environment.TARGET_DEFAULT;

    private final Expression expression;
    private final Expression target;

    EmitToDirective(Expression expression, Expression target) {
        this.expression = expression;
        this.target = target;
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        // Evaluate the value
        TemplateModel model = expression.eval(env);
        String value;
        if (model instanceof TemplateScalarModel) {
            value = ((TemplateScalarModel) model).getAsString();
        } else {
            value = EvalUtil.coerceModelToStringOrUnsupportedMarkup(
                    model, expression, null, env);
        }
        if (value == null) {
            value = "";
        }

        // Resolve the target
        String targetStr = evalTarget(env);

        // Get or open the writer
        Writer writer = env.getWriterForTarget(targetStr);

        // Resolve EOL placeholder for emit-to (consistent with ${...} in main output)
        String outputEOL = env.getOutputEOL();
        value = StringUtil.resolveOutputEOL(value, outputEOL);

        writer.write(value);
        return null;
    }

    private String evalTarget(Environment env) throws TemplateException {
        TemplateModel targetModel = target.eval(env);
        if (!(targetModel instanceof TemplateScalarModel)) {
            throw new _MiscTemplateException(target, env,
                    "The target of 'emit ... to' must evaluate to a string, but was: ",
                    targetModel == null ? "null" : targetModel.getClass().getName());
        }
        return ((TemplateScalarModel) targetModel).getAsString();
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(expression.getCanonicalForm());
        sb.append(" to ");
        sb.append(target.getCanonicalForm());
        if (canonical) sb.append("/>");
        return sb.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "#emit";
    }

    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
            case 0: return expression;
            case 1: return target;
            default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
            case 0: return ParameterRole.VALUE;
            case 1: return ParameterRole.TARGET_LOOP_VARIABLE; // closest existing role
            default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
}
