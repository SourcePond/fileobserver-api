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
package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newDirectoryStream;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A directory has to fulfill two purposes. If firstly holds the {@link WatchKey} of watched directory. Secondly, it
 * stores the checksums of changed files.
 */
public abstract class Directory {
    private static final Logger LOG = getLogger(SubDirectory.class);
    static final long TIMEOUT = 2000;
    private final ConcurrentMap<Path, Resource> resources = new ConcurrentHashMap<>();
    private final WatchKey watchKey;

    Directory(final WatchKey pWatchKey) {
        watchKey = pWatchKey;
    }

    /**
     * Iterates over a new collection of {@link FileKey} objects based on the file specified.
     * Creates then an asynchronous task for each {@link FileKey}/file combination. This tasks will
     * be executed sometime in the future. Such a task will call {@link FileObserver#modified(FileKey, Path)}
     * with the {@link FileKey}/file combination which has been associated with it.
     */
    private void forceModified(final FileObserver pObserver, final Path pFile) {
        for (final FileKey key : createKeys(pFile)) {
            getFactory().execute(() -> pObserver.modified(key, pFile));
        }
    }

    /**
     * Calls {@link #forceModified(FileObserver, Path)} with the file-observers and the file specified
     * if, and only if, the previous and current checksum specified are <em>not</em> equal.
     *
     * @param pPrevious  Previous checksum, must not be {@code null}
     * @param pCurrent   Current checksum, must not be {@code null}
     * @param pObservers Observers to be informed, must not be {@code null}
     * @param pFile      Modified (readable) file, must not be {@code null}.
     */
    private void informObservers(final Checksum pPrevious, final Checksum pCurrent, final Collection<FileObserver> pObservers, final Path pFile) {
        if (!pPrevious.equals(pCurrent)) {
            pObservers.forEach(o -> forceModified(o, pFile));
        }
    }

    /**
     * Iterates over all files contained by this directory and calls for each entry
     * {@link #forceModified(FileObserver, Path)}. Only direct children will be considered,
     * sub-directories and non-regular files will be ignored.
     *
     * @param pObserver Observer to be informed, must not be {@code null}
     */
    private void streamDirectoryAndForceInform(final FileObserver pObserver) {
        try (final DirectoryStream<Path> stream = newDirectoryStream(getPath(), p -> isRegularFile(p))) {
            stream.forEach(p -> forceModified(pObserver, p));
        } catch (final IOException e) {
            LOG.warn("Exception occurred while trying to inform single observers!", e);
        }
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Returns the registered directory keys (see {@link #addDirectoryKey(Object)}). Any change
     * on the returned collection could possible change the internal state.
     *
     * @return (Possibly empty) collection of keys, never {@code null}
     */
    abstract Collection<Object> getDirectoryKeys();

    abstract DirectoryFactory getFactory();

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Relatives the first root-directory against the path specified. To determine which is the first
     * root-directory, the key specified will be matched against every directory in the tree. If a directory directly
     * contains the key, it will be used for relativization.
     *
     * @param pPath         Path to be relativized, must not be {@code null}
     * @param pDirectoryKey Key of the desired root directory, must not be {@code null}
     * @return Relative path between root and the path specified, never {@code null}.
     */
    abstract Path relativizeAgainstRoot(Object pDirectoryKey, Path pPath);

    private FileKey createKey(final Object pDirectoryKey, final Path pFile) {
        return getFactory().newKey(pDirectoryKey, relativizeAgainstRoot(pDirectoryKey, pFile));
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Creates a new collection of {@link FileKey} objects. Therefore, every directory-key
     * returned by {@link #getDirectoryKeys()} will be combined with the relative path of the
     * file specified. The relative path is the relativization between the root-directory and
     * the file specified (see {@link #relativizeAgainstRoot(Object, Path)}).
     *
     * @param pFile File to relativize against {@link #getPath()}, must not be {@code null}
     * @return New collection of {@link FileKey} objects, never {@code null}
     */
    private Collection<FileKey> createKeys(final Path pFile) {
        return getDirectoryKeys().
                stream().map(
                k -> createKey(k, pFile)).
                collect(toList());
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Returns the {@link WatchKey} which is associated with this directory.
     * The watch-key remains accessible even afeter {@link #cancelKey()} has been called.
     *
     * @return Watch-key, never {@code null}
     */
    WatchKey getWatchKey() {
        return watchKey;
    }

    public abstract boolean isRoot();

    public abstract boolean hasKeys();

    /**
     * <p>Adds the directory-key specified to this directory instance. When a change is detected, a
     * {@link FileKey} will be generated for every directory-key/relative-path combination.
     * This {@link FileKey} instance will then be delivered (along with the readable file path)
     * to the {@link FileObserver} objects which should be informed.</p>
     * <p>
     * <p>Note: The key object should be <em>immutable</em>, {@link String} or an {@link Enum}
     * objects are good condidates for being directory-keys.</p>
     *
     * @param pDirectoryKey Directory key, must not be {@code null}
     */
    public abstract void addDirectoryKey(Object pDirectoryKey);

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     *
     * Removes the directory-key specified from this directory instance. If no such
     * key is registered nothing happens.
     *
     * @param pDirectoryKey Directory-key to be removed, must be not {@code null}
     * @return {@code true} if the directory-key specified was removed, {@code false} otherwise.
     */
    abstract boolean removeDirectoryKey(Object pDirectoryKey);

    /**
     * Removes the directory-key specified from this directory instance and informs
     * the observers specified through their {@link FileObserver#discard(FileKey)}. If no such
     * key is registered nothing happens.
     *
     * @param pDirectoryKey Directory-key to be removed, must be not {@code null}
     * @param pObservers Observers to be informed, must not be {@code null}
     */
    public final void removeDirectoryKey(final Object pDirectoryKey, final Collection<FileObserver> pObservers) {
        if (removeDirectoryKey(pDirectoryKey)) {
            final FileKey key = createKey(pDirectoryKey, getPath());
            pObservers.forEach(o -> getFactory().execute(() -> o.discard(key)));
        }
    }

    /**
     * Cancels the {@link WatchKey} held by this directory object (see {@link WatchKey#cancel()}).
     * After this, checksum resources are cleared and no more events for this directory can be retrieved.
     */
    public void cancelKey() {
        try {
            getWatchKey().cancel();
        } finally {
            resources.clear();
        }
    }

    /**
     * Iterates over the files contained by this directory and creates tasks which will be executed
     * sometime in the future. Such a task will inform the observer specified through its
     * {@link FileObserver#modified(FileKey, Path)} method. Note: only direct children will be
     * considered, sub-directories and non-regular files will be ignored.
     *
     * @param pObserver Observer to be informed, must not be {@code null}.
     */
    public void forceInform(final FileObserver pObserver) {
        getFactory().execute(() -> streamDirectoryAndForceInform(pObserver));
    }

    /**
     * Returns the path represented by this directory object.
     *
     * @return Path of this directory, never {@code null}.
     */
    public Path getPath() {
        return (Path) getWatchKey().watchable();
    }

    /**
     * Iterates over the observers specified and informs them that the file specified has
     * been discarded through their {@link FileObserver#discard(FileKey)} method. The observers
     * will be called asynchronously sometime in the future.
     *
     * @param pObservers Observers to be informed, must not be {@code null}
     * @param pFile      Discarded file, must be {@code null}
     */
    public void informDiscard(final Collection<FileObserver> pObservers, final Path pFile) {
        // Remove the checksum resource to save memory
        resources.remove(pFile);

        for (final FileKey key : createKeys(pFile)) {
            pObservers.forEach(o -> getFactory().execute(() -> o.discard(key)));
        }
    }

    /**
     * Checks whether this directory is the directory parent of the directory specified.
     *
     * @param pOther Other directory, must not be {@code null}
     * @return {@code true} if this object is the direct parent of the directory specified, {@code false} otherwise
     */
    public boolean isDirectParentOf(Directory pOther) {
        return getPath().equals(pOther.getPath().getParent());
    }

    /**
     * Triggers the {@link FileObserver#modified(FileKey, Path)} on all observers specified if the
     * file represented by the path specified has been changed i.e. has a new checksum. If no checksum change
     * has been detected, nothing happens.
     *
     * @param pObservers Observers to be informed, must not be {@code null}
     * @param pFile      File which potentially has changed, must not be {@code null}
     */
    public void informIfChanged(final Collection<FileObserver> pObservers, final Path pFile) {
        // TODO: Replace interval with configurable value
        resources.computeIfAbsent(pFile,
                f -> getFactory().newResource(SHA256, pFile)).update(TIMEOUT,
                (pPrevious, pCurrent) ->
                        informObservers(pPrevious, pCurrent, pObservers, pFile));
    }

    public abstract Directory rebase(Directory pBaseDirectory);

    public abstract Directory toRootDirectory();
}
