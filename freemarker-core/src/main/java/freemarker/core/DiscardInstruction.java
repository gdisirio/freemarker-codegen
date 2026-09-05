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

import freemarker.template.TemplateException;

/**
 * Code-first {@code discard} directive: says that the output of the enclosing {@code into} block is not to be kept.
 *
 * <p>The rest of the block still runs, and may still write; it's at the end of the template execution that the target
 * is discarded rather than committed. What discarding means is up to the target: one backed by a file typically
 * deletes it. Nothing is buffered in order to make this possible, which is why a target that can't take back what was
 * already written (the standard streams) refuses to be discarded.
 *
 * @since 2.3.35
 */
final class DiscardInstruction extends TemplateElement {

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException {
        OutputTarget target = env.getCurrentIntoTarget();
        if (target == null) {
            throw new _MiscTemplateException(env,
                    "There's nothing to discard here, as this isn't inside an \"into\" block.");
        }
        env.discardOutputTarget(target);
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        return canonical ? "discard" : getNodeTypeSymbol();
    }

    @Override
    String getNodeTypeSymbol() {
        return "discard";
    }

    @Override
    int getParameterCount() {
        return 0;
    }

    @Override
    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
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
