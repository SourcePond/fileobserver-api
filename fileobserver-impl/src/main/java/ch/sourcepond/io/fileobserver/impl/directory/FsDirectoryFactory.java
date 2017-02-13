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

import ch.sourcepond.io.checksum.api.Algorithm;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.impl.filekey.FileKeyFactory;

import java.nio.file.Path;
import java.nio.file.WatchKey;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class FsDirectoryFactory {
    private final ResourcesFactory resourcesFactory;
    private final FileKeyFactory fileKeyFactory;

    FsDirectoryFactory(final ResourcesFactory pResourcesFactory, final FileKeyFactory pFileKeyFactory) {
        resourcesFactory = pResourcesFactory;
        fileKeyFactory = pFileKeyFactory;
    }

    public FsRootDirectory newRoot(final Enum<?> pWatchedDirectoryKeyOrNull) {
        return new FsRootDirectory(this, pWatchedDirectoryKeyOrNull);
    }

    public FsDirectory newBranch(final FsBaseDirectory pParent, final WatchKey pKey) {
        return new FsDirectory(pParent, pKey);
    }

    Resource newResource(final Algorithm pAlgorithm, final Path pFile) {
        return resourcesFactory.create(pAlgorithm, pFile);
    }

    FileKey newKey(final Enum<?> pWatchedDirectoryKey, final Path pRelativePath) {
        return fileKeyFactory.newKey(pWatchedDirectoryKey, pRelativePath);
    }
}