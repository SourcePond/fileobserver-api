package ch.sourcepond.io.fileobserver.impl;


import ch.sourcepond.io.fileobserver.impl.directory.Directories;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryScanner;
import ch.sourcepond.io.fileobserver.impl.directory.FsDirectoriesFactory;
import ch.sourcepond.io.fileobserver.impl.directory.FsDirectoryFactory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class ActivatorTest {
    private final FsDirectoryFactory fsDirectoryFactory = mock(FsDirectoryFactory.class);
    private final FsDirectoriesFactory fsDirectoriesFactory = mock(FsDirectoriesFactory.class);
    private final DirectoryScanner directoryScanner = mock(DirectoryScanner.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Path directory = mock(Path.class);
    private final Path secondDirectory = mock(Path.class);
    private final Directories directories = mock(Directories.class);
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final WatchedDirectory secondWatchedDirectory = mock(WatchedDirectory.class);
    private final ExecutorServices executorServices = mock(ExecutorServices.class);
    private final Activator manager = new Activator(executorServices, fsDirectoryFactory, fsDirectoriesFactory, directories, directoryScanner);

    @Before
    public void setup() throws IOException {
        when(directory.getFileSystem()).thenReturn(fs);
        when(secondDirectory.getFileSystem()).thenReturn(fs);
        when(fs.provider()).thenReturn(provider);
        when(provider.readAttributes(directory, BasicFileAttributes.class)).thenReturn(attrs);
        when(provider.readAttributes(secondDirectory, BasicFileAttributes.class)).thenReturn(attrs);
        when(attrs.isDirectory()).thenReturn(true);
        when(watchedDirectory.getKey()).thenReturn(TestKey.TEST_KEY);
        when(watchedDirectory.getDirectory()).thenReturn(directory);
        when(secondWatchedDirectory.getKey()).thenReturn(TestKey.TEST_KEY1);
        when(secondWatchedDirectory.getDirectory()).thenReturn(secondDirectory);
    }

    @Test(expected = NullPointerException.class)
    public void bindKeyIsNull() throws IOException {
        when(watchedDirectory.getKey()).thenReturn(null);
        manager.bind(watchedDirectory);
    }

    @Test(expected = NullPointerException.class)
    public void bindDirectoryIsNull() throws IOException {
        when(watchedDirectory.getDirectory()).thenReturn(null);
        manager.bind(watchedDirectory);
    }


    @Test(expected = IllegalArgumentException.class)
    public void bindPathIsNotADirectory() throws IOException {
        when(attrs.isDirectory()).thenReturn(false);
        manager.bind(watchedDirectory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bindKeyAlreadyOccupied() throws IOException {
        manager.bind(watchedDirectory);

        // This should cause an exception to be thrown
        manager.bind(watchedDirectory);
    }

    @Test(expected = IOException.class)
    public void bindFailed() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(directories).addRoot(TEST_KEY, directory);

        // This should not cause an exception
        manager.bind(watchedDirectory);
    }

    @Test
    public void bindNoRootAdditionNecessary() throws IOException {
        manager.bind(watchedDirectory);
        when(secondWatchedDirectory.getDirectory()).thenReturn(directory);
        manager.bind(secondWatchedDirectory);
        //verify(directories).addRoot();
    }
}
