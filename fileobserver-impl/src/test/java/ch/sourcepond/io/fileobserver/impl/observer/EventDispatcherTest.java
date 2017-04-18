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
package ch.sourcepond.io.fileobserver.impl.observer;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import org.junit.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class EventDispatcherTest {
    private final ObserverManager manager = mock(ObserverManager.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private final FileKey key = mock(FileKey.class);
    private final Collection<FileKey> keys = asList(key);
    private final Collection<FileKey> parentKeys = mock(Collection.class);
    private final Path file = mock(Path.class);
    private EventDispatcher dispatcher = new EventDispatcher(manager, observers);

    @Test
    public void hasObservers() {
        assertTrue(dispatcher.hasObservers());
        dispatcher = new EventDispatcher(manager, new ArrayList<>());
        assertFalse(dispatcher.hasObservers());
    }

    @Test
    public void singleKeyModified() {
        dispatcher.modified(key, file, parentKeys);
        verify(manager).modified(observers, key, file, parentKeys);
    }

    @Test
    public void multipleKeysModified() {
        dispatcher.modified(keys, file, parentKeys);
        verify(manager).modified(observers, keys, file, parentKeys);
    }

    @Test
    public void discard() {
        dispatcher.discard(key);
        verify(manager).discard(observers, key);
    }

    @Test
    public void verifySingleObserverConstructor() {
        dispatcher = new EventDispatcher(manager, observer);
        dispatcher.discard(key);
        verify(manager).discard(argThat(inv -> inv.size() == 1 && inv.contains(observer)), same(key));
    }
}