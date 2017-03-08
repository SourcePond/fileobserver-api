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

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class DedicatedFileSystem implements Closeable {
    private static final Logger LOG = getLogger(DedicatedFileSystem.class);
    private final Map<Object, WatchedDirectory> watchtedDirectories = new HashMap<>();
    private final ConcurrentMap<Path, Directory> dirs;
    private final ExecutorServices executorServices;
    private final DirectoryFactory directoryFactory;
    private final WatchServiceWrapper wsRegistrar;
    private final DirectoryRebase rebase;
    private final DirectoryRegistrationWalker walker;

    DedicatedFileSystem(final ExecutorServices pExecutorServices,
                        final DirectoryFactory pDirectoryFactory,
                        final WatchServiceWrapper pWsRegistrar,
                        final DirectoryRebase pRebase,
                        final DirectoryRegistrationWalker pWalker,
                        final ConcurrentMap<Path, Directory> pDirs) {
        executorServices = pExecutorServices;
        directoryFactory = pDirectoryFactory;
        wsRegistrar = pWsRegistrar;
        rebase = pRebase;
        walker = pWalker;
        dirs = pDirs;
    }

    /**
     * <p>Iterates through all registered directories and passes all their files to the
     * {@link FileObserver#modified(FileKey, Path)} of the observer specified. This is necessary for newly registered
     * observers who need to know about all watched files. See {@link #registerRootDirectory(WatchedDirectory, Collection)} and
     * {@link #directoryCreated(Path, Collection)} to get an idea how directories are registered with this object.
     * <p>Note: it's guaranteed that the {@link Path} instances passed
     * to the observer are regular files (not directories).
     *
     * @param pObserver Observer to be informed, must not be {@code null}.
     */
    public void forceInform(final FileObserver pObserver) {
        dirs.values().forEach(d -> d.forceInform(pObserver));
    }
    
    /**
     * @param pWatchedDirectory
     * @param pObservers
     */
    // Note: Despite a ConcurrentMap is used it's necessary to synchronize this method.
    // The reason is because all sub-directories need to be registered before another WatchedDirectory
    // is being registered through this method.
    public synchronized void registerRootDirectory(final WatchedDirectory pWatchedDirectory,
                                                   final Collection<FileObserver> pObservers)
            throws IOException {
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        if (watchtedDirectories.containsKey(key)) {
            throw new IllegalArgumentException(format("Directory-key %s is already used by %s", watchtedDirectories.get(key)));
        }
        watchtedDirectories.put(key, pWatchedDirectory);

        // It's already checked that the directory is not null
        final Path directory = pWatchedDirectory.getDirectory();

        Directory dir = dirs.get(directory);
        if (dir == null) {
            // If no directory is registered for the path specified, create a new root-directory.
            // Register the path with the watch-service and use the returned watch-key to create the root directory.
            dir = directoryFactory.newRoot(wsRegistrar.register(directory));

            // If there are root-directories registered which are children or the newly added
            // root directory, they need to be rebased (including their direct children)
            rebase.rebaseExistingRootDirectories(dir);

            // Register directories; important here is to pass the newly create root-directory
            // (otherwise FileObserver#supplement would not be called).
            walker.rootRebased(dir, pObservers);
        }

        // VERY IMPORTANT: in any case, add the directory with the directory-key
        dir.addDirectoryKey(key);
    }


    public synchronized void unregisterRootDirectory(final WatchedDirectory pWatchedDirectory,
                                                     final Collection<FileObserver> pObservers) {
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");
        watchtedDirectories.remove(key);

        final Directory dir = dirs.get(directory);

        // Remove the directory-key of the watched directory from the key list
        dir.removeDirectoryKey(key, pObservers);

        // If all watched-directories which referenced the directory have been de-registered,
        // it's time to clean-up.
        if (!dir.hasKeys()) {
            rebase.cancelAndRebaseDiscardedDirectory(dir);
        }
    }

    /**
     * Registers the directory specified and all its sub-directories with the watch-service held by this object.
     * Additionally, it passes any detected file to {@link FileObserver#modified(FileKey, Path)} to the observers
     * specified.
     *
     * @param pDirectory Newly created directory, must not be {@code null}
     * @param pObservers Observers to be informed about detected files, must not be {@code null}
     */
    void directoryCreated(final Path pDirectory, final Collection<FileObserver> pObservers) {
        walker.directoryCreated(pDirectory, pObservers);
    }

    public boolean directoryDiscarded(final Collection<FileObserver> pObservers, final Path pDirectory) {
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
            dir.informDiscard(pObservers, pDirectory);
        }
        return wasDirectory;
    }

    public Directory getDirectory(final Path pPath) {
        return dirs.get(pPath);
    }

    @Override
    public void close() {
        try {
            wsRegistrar.close();
        } finally {
            dirs.clear();
        }
    }

    public WatchKey poll() {
        return wsRegistrar.poll();
    }
}
