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
package ch.sourcepond.io.fileobserver.impl.diff;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;

import java.util.Collection;
import java.util.concurrent.Executor;

/**
 *
 */
public class DiffObserverFactory {

    // Injected by Felix DM; this field must not be renamed!
    private volatile Executor observerExecutor;
    private final DefaultFileKeyFactory keyFactory;

    // Constructor for Bundle-Activator
    public DiffObserverFactory(final DefaultFileKeyFactory pKeyFactory) {
        keyFactory = pKeyFactory;
    }

    // Constructor for testing
    DiffObserverFactory(final DefaultFileKeyFactory pKeyFactory, final Executor pObserverExecutor) {
        keyFactory = pKeyFactory;
        observerExecutor = pObserverExecutor;
    }

    public DiffObserver createObserver(final DedicatedFileSystem pFs,
                                       final Collection<FileObserver> pDelegates) {
        return new DiffObserver(pFs, observerExecutor, pDelegates);
    }
}