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
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

/**
 * A lazy, single-pass {@link TemplateCollectionModel} backed by a {@link BufferedReader}.
 * Yields one line at a time, with line terminators stripped. The underlying reader
 * is closed when iteration completes (either reaching EOF or being abandoned).
 *
 * <p>Supports only single iteration. Calling {@link #iterator()} more than once
 * returns the same iterator (which may be exhausted).
 *
 * @since 2.3.35
 */
final class LazyLineCollectionModel implements TemplateCollectionModel {

    private final BufferedReader reader;
    private final Environment env;
    private LineIterator iterator;

    LazyLineCollectionModel(BufferedReader reader, Environment env) {
        this.reader = reader;
        this.env = env;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateModelException {
        if (iterator == null) {
            iterator = new LineIterator();
        }
        return iterator;
    }

    private final class LineIterator implements TemplateModelIterator {
        private String nextLine;
        private boolean nextLineFetched;
        private boolean closed;

        @Override
        public boolean hasNext() throws TemplateModelException {
            ensureFetched();
            return nextLine != null;
        }

        @Override
        public TemplateModel next() throws TemplateModelException {
            ensureFetched();
            if (nextLine == null) {
                throw new TemplateModelException("No more lines to read.");
            }
            String line = nextLine;
            nextLine = null;
            nextLineFetched = false;
            return new SimpleScalar(line);
        }

        private void ensureFetched() throws TemplateModelException {
            if (nextLineFetched || closed) {
                return;
            }
            try {
                nextLine = reader.readLine();
                nextLineFetched = true;
                if (nextLine == null) {
                    closeReader();
                }
            } catch (IOException e) {
                closeReader();
                throw new TemplateModelException("Failed to read next line", e);
            }
        }

        private void closeReader() {
            if (closed) return;
            closed = true;
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
            if (env != null) {
                env.unregisterReader(reader);
            }
        }
    }
}
