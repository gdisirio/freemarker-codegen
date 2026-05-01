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
 * An instruction that flushes the output stream(s).
 *
 * <p>Three forms in code-first mode:
 * <ul>
 *   <li>{@code flush} — flushes the default writer (legacy behavior)</li>
 *   <li>{@code flush to <target>} — flushes a specific target (path / stdout / stderr / default)</li>
 *   <li>{@code flush all} — flushes the default writer and all auxiliary writers</li>
 * </ul>
 *
 * <p>Classic {@code <#flush>} always uses the default-only form.
 */
final class FlushInstruction extends TemplateElement {

    /** Target expression. Null means: flush the default writer. */
    private final Expression target;

    /** If true, flush the default writer and all open auxiliary writers. */
    private final boolean flushAll;

    FlushInstruction() {
        this.target = null;
        this.flushAll = false;
    }

    FlushInstruction(Expression target, boolean flushAll) {
        this.target = target;
        this.flushAll = flushAll;
    }

    @Override
    TemplateElement[] accept(Environment env) throws IOException, TemplateException {
        if (flushAll) {
            env.flushAllWriters();
        } else if (target == null) {
            env.getOut().flush();
        } else {
            String t = evalTarget(env);
            Writer w = env.getWriterForTarget(t);
            w.flush();
        }
        return null;
    }

    private String evalTarget(Environment env) throws TemplateException {
        TemplateModel m = target.eval(env);
        if (!(m instanceof TemplateScalarModel)) {
            throw new _MiscTemplateException(target, env,
                    "The target of 'flush to' must be a string, but was: ",
                    m == null ? "null" : m.getClass().getName());
        }
        return ((TemplateScalarModel) m).getAsString();
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        if (flushAll) {
            sb.append(" all");
        } else if (target != null) {
            sb.append(" to ").append(target.getCanonicalForm());
        }
        if (canonical) sb.append("/>");
        return sb.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "#flush";
    }

    @Override
    int getParameterCount() {
        return target != null ? 1 : 0;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx == 0 && target != null) return target;
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx == 0 && target != null) return ParameterRole.VALUE;
        throw new IndexOutOfBoundsException();
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

}
