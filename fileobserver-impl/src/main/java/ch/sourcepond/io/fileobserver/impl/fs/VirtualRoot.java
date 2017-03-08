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
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.util.Objects.requireNonNull;

/**
 *
 */
public class VirtualRoot {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualRoot.class);
    private final ConcurrentMap<FileSystem, DedicatedFileSystem> children = new ConcurrentHashMap<>();
    private final Set<FileObserver> observers = ConcurrentHashMap.newKeySet();
    private final DedicatedFileSystemFactory dedicatedFileSystemFactory;

    // Injected by Felix DM
    private final ExecutorServices executorServices;

    /**
     * We intentionally do <em>not</em> use {@code children.values()} because we need to iterate
     * over the children in the scanner-thread loop. If we used {@code children.values()} directly
     * we must create a new iterator during every loop iteration. This is a potential memory leak
     * and causes unnecessary GC activity.
     */
    private final List<DedicatedFileSystem> roots;

    // Constructor for BundleActivator
    public VirtualRoot(final ExecutorServices pExecutorServices, final DirectoryFactory pDirectoryFactory) {
        executorServices = pExecutorServices;
        dedicatedFileSystemFactory = new DedicatedFileSystemFactory(pExecutorServices, pDirectoryFactory);
        roots = new CopyOnWriteArrayList<>();
    }

    // Constructor for testing
    public VirtualRoot(final DedicatedFileSystemFactory pFsDirectories,
                       final ExecutorServices pExecutorServices,
                       final List<DedicatedFileSystem> pRoots) {
        dedicatedFileSystemFactory = pFsDirectories;
        executorServices = pExecutorServices;
        roots = pRoots;
    }

    // Composition callback for Felix DM
    public Object[] getComposition() {
        return new Object[]{dedicatedFileSystemFactory, dedicatedFileSystemFactory.getDirectoryFactory()};
    }

    public void addObserver(final FileObserver pObserver) {
        if (null == pObserver) {
            LOG.warn("Observer is null; nothing to add");
        } else if (observers.add(pObserver)) {
            children.values().forEach(f -> f.forceInform(pObserver));
        }
    }

    public void removeObserver(final FileObserver pObserver) {
        if (null == pObserver) {
            LOG.warn("Observer is null; nothing to remove");
        } else {
            observers.remove(pObserver);
        }
    }

    private DedicatedFileSystem newDirectories(final FileSystem pFs) {
        try {
            final DedicatedFileSystem fsdirs = dedicatedFileSystemFactory.newDirectories(pFs);
            roots.add(fsdirs);
            return fsdirs;
        } catch (final IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    public void addRoot(final WatchedDirectory pWatchedDirectory) throws IOException {
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");
        try {
            children.computeIfAbsent(directory.getFileSystem(),
                    this::newDirectories).registerRootDirectory(pWatchedDirectory, observers);
        } catch (final UncheckedIOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private DedicatedFileSystem getDedicatedFileSystem(final Path pPath) {
        final DedicatedFileSystem fsdirs = children.get(pPath.getFileSystem());
        if (null == fsdirs) {
            throw new IllegalStateException(format("No appropriate root-directory found for %s", pPath));
        }
        return fsdirs;
    }

    public void pathModified(final Path pPath) {
        if (isDirectory(pPath)) {
            getDedicatedFileSystem(pPath).directoryCreated(pPath, observers);
        } else {
            final Directory dir = getDedicatedFileSystem(pPath).getDirectory(pPath.getParent());
            if (null == dir) {
                throw new NullPointerException(format("No directory registered for file %s", pPath));
            }
            dir.informIfChanged(observers, pPath);
        }
    }

    public void pathDiscarded(final Path pPath) {
        final DedicatedFileSystem dfs = getDedicatedFileSystem(pPath);

        // The deleted path was a directory
        if (!dfs.directoryDiscarded(observers, pPath)) {

            // The deleted path was a file
            dfs.getDirectory(pPath.getParent()).informDiscard(observers, pPath);
        }
    }

    // Lifecycle method for Felix DM
    public void stop() {
        children.values().forEach(DedicatedFileSystem::close);
        children.clear();
    }

    /**
     * Closes the {@link DedicatedFileSystem} instance specified and removes it from
     * the root list.
     *
     * @param pDedicatedFileSystem
     */
    public void close(final DedicatedFileSystem pDedicatedFileSystem) {
        if (pDedicatedFileSystem != null) {
            pDedicatedFileSystem.close();

            // Remove closed watch-service from the roots-list and children-map
            roots.remove(pDedicatedFileSystem);
            children.values().remove(pDedicatedFileSystem);
        }
    }

    public List<DedicatedFileSystem> getRoots() {
        return roots;
    }
}
