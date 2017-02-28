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
package ch.sourcepond.io.fileobserver.spi;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.util.Objects.requireNonNull;

/**
 * Default implementation of the {@link WatchedDirectory} interface.
 */
final class DefaultWatchedDirectory implements WatchedDirectory {
    private final Collection<RelocationObserver> observers = new CopyOnWriteArraySet<>();
    private final Object key;
    private volatile Path directory;

    DefaultWatchedDirectory(final Object pKey, final Path pDirectory) {
        key = requireNonNull(pKey, "Key is null");
        validate(pDirectory);
        directory = pDirectory;
    }

    private void validate(final Path pDirectory) {
        requireNonNull(pDirectory, "Directory is null");
        if (!isDirectory(pDirectory)) {
            throw new IllegalArgumentException(format("%s is not a directory", pDirectory));
        }
    }

    @Override
    public Object getKey() {
        return key;
    }

    @Override
    public void addObserver(final RelocationObserver pObserver) {
        if (!observers.contains(requireNonNull(pObserver, "Observer is null"))) {
            observers.add(pObserver);
        }
    }

    @Override
    public void removeObserver(final RelocationObserver pObserver) {
        if (pObserver != null) {
            observers.remove(pObserver);
        }
    }

    @Override
    public Path getDirectory() {
        return directory;
    }

    @Override
    public void relocate(final Path pDirectory) {
        validate(pDirectory);
        final Path previous = directory;

        // Do only something if the previous location is
        // different to the directory specified.
        if (!previous.equals(pDirectory)) {
            directory = pDirectory;
            observers.forEach(o -> o.destinationChanged(this, previous));
        }
    }
}