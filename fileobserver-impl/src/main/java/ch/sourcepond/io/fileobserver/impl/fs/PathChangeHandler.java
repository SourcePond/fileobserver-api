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
package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.VirtualRoot;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class PathChangeHandler {
    private static final Logger LOG = getLogger(PathChangeHandler.class);
    private final VirtualRoot virtualRoot;
    private final DirectoryRegistrationWalker walker;
    private final ConcurrentMap<Path, Directory> dirs;

    PathChangeHandler(final VirtualRoot pVirtualRoot,
                      final DirectoryRegistrationWalker pWalker,
                      final ConcurrentMap<Path, Directory> pDirs) {
        virtualRoot = pVirtualRoot;
        walker = pWalker;
        dirs = pDirs;
    }

    private Collection<FileObserver> getObservers() {
        return virtualRoot.getObservers();
    }

    void rootAdded(final Directory pNewRoot) {
        walker.rootAdded(pNewRoot, getObservers());
    }

    void removeFileSystem(final DedicatedFileSystem pDfs) {
        virtualRoot.removeFileSystem(pDfs);
    }

    private Directory getDirectory(final Path pPath) {
        return dirs.get(pPath);
    }

    void pathModified(final BasicFileAttributes pAttrs, final Path pPath) {
        if (pAttrs.isDirectory()) {
            walker.directoryCreated(pPath, getObservers());
        } else {
            final Directory dir = requireNonNull(getDirectory(pPath.getParent()),
                    () -> format("No directory registered for file %s", pPath));
            dir.informIfChanged(getObservers(), pPath);
        }
    }

    void pathDiscarded(final Path pPath) {
        // The deleted path was a directory
        if (!directoryDiscarded(pPath)) {
            final Directory parentDirectory = getDirectory(pPath.getParent());
            if (parentDirectory == null) {
                LOG.warn("Parent of {} does not exist. Nothing to discard", pPath, new Exception());
            } else {
                // The deleted path was a file
                parentDirectory.informDiscard(getObservers(), pPath);
            }
        }
    }

    private boolean directoryDiscarded(final Path pDirectory) {
        final Directory dir = dirs.remove(pDirectory);
        final boolean wasDirectory = dir != null;
        if (wasDirectory) {
            dir.cancelKey();
            for (final Iterator<Map.Entry<Path, Directory>> it = dirs.entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry<Path, Directory> entry = it.next();
                if (entry.getKey().startsWith(pDirectory)) {
                    entry.getValue().cancelKey();
                    it.remove();
                }
            }
            dir.informDiscard(getObservers(), pDirectory);
        }
        return wasDirectory;
    }
}