package ch.sourcepond.io.fileobserver.impl;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class FsDirectoriesTest {
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final Registrar registrar = mock(Registrar.class);
    private final FsDirectory fsDirectory = mock(FsDirectory.class);
    private final FsDirectory fsSubDirectory = mock(FsDirectory.class);
    private final ObserverHandler handler = mock(ObserverHandler.class);
    private final Path directory = mock(Path.class);
    private final Path subDirectory = mock(Path.class);
    private final DirectoryStream<Path> stream = mock(DirectoryStream.class);
    private final WatchKey key = mock(WatchKey.class);
    private final Map<Path, FsDirectory> entries = new HashMap<>();
    private final FsDirectories fsDirectories = new FsDirectoriesFactory().newDirectories(registrar);

    @Before
    public void setup() throws IOException {
        when(fs.provider()).thenReturn(provider);
        when(directory.getFileSystem()).thenReturn(fs);
        when(provider.newDirectoryStream(same(directory), any())).thenReturn(stream);
        when(registrar.iterator()).thenReturn(asList(fsDirectory).iterator());
        when(fsDirectory.getPath()).thenReturn(directory);
        when(stream.iterator()).thenReturn(asList(subDirectory).iterator());
        entries.put(subDirectory, fsSubDirectory);
    }

    @Test
    public void initialyInformHandler() throws IOException {
        fsDirectories.initialyInformHandler(handler);
        verify(registrar).directoryCreated(subDirectory, handler);
    }

    @Test
    public void initialyInformHandlerDirectoryStreamCreationFailed() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(provider).newDirectoryStream(same(directory), any());

        // This should not cause an exception
        fsDirectories.initialyInformHandler(handler);
    }

    @Test
    public void initialyInformHandlerExceptionOccurred() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(registrar).directoryCreated(subDirectory, handler);

        // This should not cause an exception
        fsDirectories.initialyInformHandler(handler);
    }

    @Test
    public void directoryDeleted() {
        when(registrar.directoryDeleted(directory)).thenReturn(true);
        when(subDirectory.startsWith(directory)).thenReturn(true);
        assertTrue(fsDirectories.directoryDeleted(directory));
        assertTrue(entries.isEmpty());
        verify(fsDirectory).cancelKey();
        verify(fsSubDirectory).cancelKey();
    }

    @Test
    public void directoryDeletedDirUnknown() {
        fsDirectories.directoryDeleted(directory);
        verifyZeroInteractions(directory, subDirectory);
    }

    @Test
    public void directoryDeletedNoASubDirectory() {
        when(registrar.directoryDeleted(directory)).thenReturn(true);
        assertTrue(fsDirectories.directoryDeleted(directory));
        assertFalse(entries.isEmpty());
        verify(fsDirectory).cancelKey();
        verify(fsSubDirectory, never()).cancelKey();
    }

    @Test
    public void getDirectory() {
        when(subDirectory.getParent()).thenReturn(directory);
        when(registrar.get(directory)).thenReturn(fsDirectory);
        assertSame(fsDirectory, fsDirectories.getDirectory(subDirectory));
    }

    @Test(expected = NullPointerException.class)
    public void getDirectoryParentUnknown() {
        when(subDirectory.getParent()).thenReturn(directory);
        fsDirectories.getDirectory(subDirectory);
    }

    @Test
    public void close() throws IOException {
        fsDirectories.close();
        verify(registrar).close();
    }


    @Test
    public void closeIOException() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(registrar).close();

        // This should not cause an exception
        fsDirectories.close();
    }

    @Test
    public void poll() {
        when(registrar.poll()).thenReturn(key);
        assertSame(key, fsDirectories.poll());
    }
}
