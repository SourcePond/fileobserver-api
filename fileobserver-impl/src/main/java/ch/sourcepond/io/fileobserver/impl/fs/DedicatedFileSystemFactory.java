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

import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 *
 */
public class DedicatedFileSystemFactory {
    private final DirectoryFactory directoryFactory;

    // Injected by Felix DM; this field must not be renamed!
    private volatile ExecutorService directoryWalkerExecutor;

    // Constructor for BundleActivator
    public DedicatedFileSystemFactory(final DirectoryFactory pDirectoryFactory) {
        directoryFactory = pDirectoryFactory;
    }

    public DirectoryFactory getDirectoryFactory() {
        return directoryFactory;
    }

    public DedicatedFileSystem newDirectories(final FileSystem pFs) throws IOException {
        final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
        final WatchServiceWrapper wrapper = new WatchServiceWrapper(pFs.newWatchService());
        final DirectoryRegistrationWalker walker = new DirectoryRegistrationWalker(
                wrapper,
                directoryFactory,
                directoryWalkerExecutor,
                dirs);
        return new DedicatedFileSystem(directoryFactory,
                wrapper,
                new DirectoryRebase(directoryFactory, wrapper, dirs),
                walker,
                dirs);
    }
}
