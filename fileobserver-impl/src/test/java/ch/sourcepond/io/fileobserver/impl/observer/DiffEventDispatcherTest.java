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

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class DiffEventDispatcherTest {
    private final ObserverManager manager = mock(ObserverManager.class);
    private final DiffObserver observer = mock(DiffObserver.class);
    private final DiffEventDispatcher dispatcher = new DiffEventDispatcher(manager, observer);

    @Test
    public void close() {
        dispatcher.close();
        verify(observer).close();
    }
}
