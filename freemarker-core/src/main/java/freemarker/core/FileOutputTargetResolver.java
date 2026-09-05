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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link OutputTargetResolver} that treats target names as file paths, which is what a code generator usually
 * wants. It isn't installed by default — deciding that a name is a file, and where a relative one is resolved
 * against, is the application's call; this class only saves you writing it.
 *
 * <p>A relative name is resolved against the base directory given to the constructor, or against the working
 * directory if that's {@code null}. Missing parent directories are created, and an existing file is truncated. The
 * file is closed when the template execution ends, and deleted instead if the template discarded it.
 *
 * @since 2.3.35
 */
public class FileOutputTargetResolver implements OutputTargetResolver {

    private final File baseDirectory;
    private final Charset charset;

    /**
     * Resolves relative names against the working directory, writing UTF-8.
     */
    public FileOutputTargetResolver() {
        this(null, null);
    }

    /**
     * Resolves relative names against {@code baseDirectory} (or the working directory if that's {@code null}),
     * writing UTF-8.
     */
    public FileOutputTargetResolver(File baseDirectory) {
        this(baseDirectory, null);
    }

    /**
     * @param baseDirectory
     *            What relative names are resolved against, or {@code null} for the working directory.
     * @param charset
     *            The charset to write with, or {@code null} for UTF-8.
     */
    public FileOutputTargetResolver(File baseDirectory, Charset charset) {
        this.baseDirectory = baseDirectory;
        this.charset = charset != null ? charset : Charset.forName("UTF-8");
    }

    @Override
    public OutputTarget open(String name, Environment env) throws IOException {
        Path path = Paths.get(name);
        if (!path.isAbsolute() && baseDirectory != null) {
            path = baseDirectory.toPath().resolve(path);
        }
        path = path.toAbsolutePath().normalize();

        // Names that only differ in how the path is spelled ("a.txt", "./a.txt", "sub/../a.txt") are the same file,
        // and so must be the same target; opening it a second time would truncate what was already written.
        Map<Path, OutputTarget> openTargets = getOpenTargets(env);
        OutputTarget target = openTargets.get(path);
        if (target != null) {
            return target;
        }

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        target = new FileOutputTarget(path, new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(path), charset)));
        openTargets.put(path, target);
        return target;
    }

    /**
     * The targets opened for one template execution, kept on the {@link Environment} so that a resolver instance can
     * be shared between executions and threads.
     */
    @SuppressWarnings("unchecked")
    private Map<Path, OutputTarget> getOpenTargets(Environment env) {
        Object attr = env.getCustomAttribute(OPEN_TARGETS_ATTR);
        if (attr == null) {
            attr = new HashMap<Path, OutputTarget>();
            env.setCustomAttribute(OPEN_TARGETS_ATTR, attr);
        }
        return (Map<Path, OutputTarget>) attr;
    }

    private static final String OPEN_TARGETS_ATTR = FileOutputTargetResolver.class.getName() + ".openTargets";

    private static final class FileOutputTarget implements OutputTarget {
        private final Path path;
        private final Writer writer;

        FileOutputTarget(Path path, Writer writer) {
            this.path = path;
            this.writer = writer;
        }

        @Override
        public Writer getWriter() {
            return writer;
        }

        @Override
        public void commit() throws IOException {
            writer.close();
        }

        @Override
        public void discard() throws IOException {
            // Nothing is buffered in order to be able to take output back, so we delete what was written.
            writer.close();
            Files.deleteIfExists(path);
        }
    }

}
