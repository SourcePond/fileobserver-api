/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fileobserver.impl.listener;

import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.Update;
import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class DiffListener implements PathChangeListener, Closeable {
    private static final Logger LOG = getLogger(DiffListener.class);
    private final Map<DispatchKey, Path> modifiedKeys = new HashMap<>();
    private final Set<DispatchKey> discardedKeys = new HashSet<>();
    private final Map<DispatchKey, Collection<DispatchKey>> supplementKeys = new HashMap<>();
    private final DedicatedFileSystem fs;
    private final EventDispatcher dispatcher;
    private final Config config;

    DiffListener(final DedicatedFileSystem pFs,
                 final EventDispatcher pDispatcher,
                 final Config pConfig) {
        fs = pFs;
        dispatcher = pDispatcher;
        config = pConfig;
    }

    private void informModified(final Update pUpdate, final DispatchKey pKey, final Path pFile) {
        if (pUpdate.hasChanged()) {
            final Collection<DispatchKey> supplementKeysOrNull = supplementKeys.computeIfAbsent(pKey, k -> emptyList());
            dispatcher.modified(pKey, pFile, supplementKeysOrNull);
        }
    }

    private Resource getResource(final Path pFile) {
        final Directory dir = fs.getDirectory(pFile.getParent());
        if (dir == null) {
            LOG.warn("Checksum update cancelled because no directory registered for {}", pFile);
            return null;
        }
        return dir.getResource(pFile);
    }

    private void updateResource(final DispatchKey pKey, final Path pFile) {
        final Resource resource = getResource(pFile);
        if (resource != null) {
            resource.update(config.writeDeadlineMillis(), u -> informModified(u, pKey, pFile));
        }
    }

    @Override
    public void close() {
        modifiedKeys.forEach(this::updateResource);
        discardedKeys.removeAll(modifiedKeys.keySet());
        discardedKeys.forEach(k -> dispatcher.discard(k));
    }

    @Override
    public void modified(final PathChangeEvent pEvent) throws IOException {
        modifiedKeys.put(pEvent.getKey(), pEvent.getFile());
    }

    @Override
    public void discard(final DispatchKey pKey) {
        discardedKeys.add(pKey);
    }

    @Override
    public void supplement(final DispatchKey pKnownKey, final DispatchKey pAdditionalKey) {
        supplementKeys.computeIfAbsent(pKnownKey, k -> new LinkedHashSet<>()).add(pAdditionalKey);
    }
}
