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
package ch.sourcepond.io.fileobserver.api;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DispatchKeyTest {
    private static final String ANY_NAME = "anyName";
    private static final String DIRECTORY_KEY_1 = "directoryKey1";
    private static final String DIRECTORY_KEY_2 = "directoryKey2";
    private final Path path = mock(Path.class, withSettings().name("root"));
    private Path otherPath = mock(Path.class);
    private final Path fileName = mock(Path.class, withSettings().name(ANY_NAME));
    private final DispatchKey key1 = mock(DispatchKey.class);
    private final DispatchKey key2 = mock(DispatchKey.class);
    private final DispatchKey key3 = mock(DispatchKey.class);
    private final DispatchKey key4 = mock(DispatchKey.class);

    private void setup(final DispatchKey pKey, final String pDirectoryKey, final Path pPath) {
        doCallRealMethod().when(pKey).isSubKeyOf(any());
        doCallRealMethod().when(pKey).isParentKeyOf(any());
        doCallRealMethod().when(pKey).findSubKeys(any());
        doCallRealMethod().when(pKey).removeSubKeys(any());
        when(pKey.getRelativePath()).thenReturn(pPath);
        when(pKey.getDirectoryKey()).thenReturn(pDirectoryKey);
    }

    @Before
    public void setup() {
        when(path.getFileName()).thenReturn(fileName);
        setup(key1, DIRECTORY_KEY_1, path);
        setup(key2, DIRECTORY_KEY_1, path);
        setup(key3, DIRECTORY_KEY_1, otherPath);
        setup(key4, DIRECTORY_KEY_2, otherPath);
    }

    @Test
    public void getFileName() {
        doCallRealMethod().when(key1).getFileName();
        assertEquals(ANY_NAME, key1.getFileName());
    }

    @Test
    public void isParentKey() {
        when(path.startsWith(path)).thenReturn(true);
        assertTrue(key1.isParentKeyOf(key2));
        when(otherPath.startsWith(path)).thenReturn(true);
        assertTrue(key1.isParentKeyOf(key3));
        when(path.startsWith(otherPath)).thenReturn(true);
        assertFalse(key1.isParentKeyOf(key4));
    }

    @Test
    public void isSubKey() {
        when(path.startsWith(path)).thenReturn(true);
        assertTrue(key1.isSubKeyOf(key2));
        when(path.startsWith(otherPath)).thenReturn(true);
        assertTrue(key1.isSubKeyOf(key3));
        when(otherPath.startsWith(path)).thenReturn(true);
        assertFalse(key1.isSubKeyOf(key4));
    }

    @Test
    public void findSubKeys() {
        when(otherPath.startsWith(path)).thenReturn(true);
        final Collection<DispatchKey> subKeys = key1.findSubKeys(asList(key2, key3, key4));
        assertEquals(1, subKeys.size());
        assertSame(key3, subKeys.iterator().next());
    }

    @Test
    public void removeKeys() {
        when(otherPath.startsWith(path)).thenReturn(true);
        final List<DispatchKey> keys = new LinkedList<>();
        keys.add(key2);
        keys.add(key3);
        keys.add(key4);
        key1.removeSubKeys(keys);
        assertEquals(2, keys.size());
        final Iterator<DispatchKey> it = keys.iterator();
        assertSame(key2, it.next());
        assertSame(key4, it.next());
    }
}