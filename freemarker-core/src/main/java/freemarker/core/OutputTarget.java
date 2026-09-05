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

/**
 * Somewhere that the {@code into} directive of code-first mode can send output to, as opened by an
 * {@link OutputTargetResolver}.
 *
 * <p>A target is opened at most once per template execution, however many {@code into} blocks name it; writing to it
 * again appends, so a template can come back to a target it wrote to earlier. At the end of the execution every target
 * that was opened is either committed or discarded.
 *
 * <p>Deciding what committing and discarding <em>mean</em> is left to the implementation, which is the point of this
 * interface: FreeMarker doesn't know whether a target is a file, and doesn't buffer anything so as to be able to take
 * output back. A target backed by a file typically closes the file on commit and deletes it on discard; one backed by
 * a stream that can't be un-written may refuse to be discarded.
 *
 * @since 2.3.35
 */
public interface OutputTarget {

    /**
     * The writer to send the output to; the same instance on each call.
     */
    Writer getWriter() throws IOException;

    /**
     * Called when the template execution ends and the output of this target is to be kept.
     */
    void commit() throws IOException;

    /**
     * Called when the template abandoned this target with the {@code discard} directive, and its output is not to be
     * kept. Implementations that can't undo what was already written should throw an {@link IOException} rather than
     * silently keeping it.
     */
    void discard() throws IOException;

}
