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

/**
 * Code-first {@code into <target>} ... {@code end} directive: everything emitted inside the block goes to the named
 * target instead of to the main output.
 *
 * <p>Naming the same target from more than one block appends to it, so a template can go back and forth between the
 * files it's generating. Anything emitted outside every block goes to the main output as usual — which means a
 * template that puts all of its output into blocks writes nothing to the main output at all.
 *
 * @since 2.3.35
 */
final class IntoDirective extends TemplateElement {

    private final Expression targetExp;

    IntoDirective(Expression targetExp, TemplateElements children) {
        this.targetExp = targetExp;
        setChildren(children);
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        String name = evalTargetName(env);
        OutputTarget target = env.getOutputTarget(name, targetExp);

        Writer prevOut = env.getOut();
        OutputTarget prevIntoTarget = env.getCurrentIntoTarget();
        env.setOut(target.getWriter());
        env.setCurrentIntoTarget(target);
        try {
            env.visit(getChildBuffer());
        } finally {
            env.setOut(prevOut);
            env.setCurrentIntoTarget(prevIntoTarget);
        }
        return null;
    }

    private String evalTargetName(Environment env) throws TemplateException {
        TemplateModel model = targetExp.eval(env);
        if (!(model instanceof TemplateScalarModel)) {
            throw new NonStringException(targetExp, model, env);
        }
        String name = ((TemplateScalarModel) model).getAsString();
        if (name == null) {
            throw new _MiscTemplateException(targetExp, env, "The output target name evaluated to null.");
        }
        return name;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(targetExp.getCanonicalForm());
        if (canonical) {
            sb.append('\n');
            sb.append(getChildrenCanonicalForm());
            sb.append("end");
        }
        return sb.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "into";
    }

    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return targetExp;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.VALUE;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

    @Override
    boolean isShownInStackTrace() {
        return true;
    }

}
