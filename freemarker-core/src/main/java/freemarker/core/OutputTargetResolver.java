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

/**
 * Turns the target name used by the {@code into} directive of code-first mode into somewhere that can be written to.
 *
 * <p>There's no resolver by default, and then naming a target other than {@code stdout} or {@code stderr} is an error.
 * That's deliberate: what a name means — a file, a database row, an entry in an archive — and where a relative path
 * would be resolved against are decisions for the application embedding FreeMarker, not for FreeMarker. Set one with
 * {@link freemarker.template.Configuration#setOutputTargetResolver(OutputTargetResolver)}.
 *
 * <p>{@code stdout} and {@code stderr} are handled by FreeMarker itself and never reach a resolver, as they always
 * exist and involve no such decision.
 *
 * @since 2.3.35
 */
public interface OutputTargetResolver {

    /**
     * Opens the target with the given name, or throws {@link IOException} if it can't be. Called at most once per name
     * per template execution.
     *
     * @param name
     *            The name as it appeared in the template, never {@code null}.
     * @param env
     *            The execution the target is opened for; useful for reaching custom attributes or the template's name.
     */
    OutputTarget open(String name, Environment env) throws IOException;

}
