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
package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.commons.smartswitch.lib.SmartSwitchActivatorBase;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryScanner;
import ch.sourcepond.io.fileobserver.impl.fs.VirtualRoot;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static java.time.Clock.systemUTC;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newWorkStealingPool;

/**
 * Bundle activator; this class manages the lifecycle of the bundle.
 */
public class Activator extends SmartSwitchActivatorBase {
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
    private final VirtualRoot virtualRoot;
    private final DirectoryScanner directoryScanner;

    // Constructor for OSGi framework
    public Activator() {
        virtualRoot = new VirtualRoot();
        directoryScanner = new DirectoryScanner(systemUTC(), virtualRoot);
    }

    // Constructor for testing
    public Activator(final VirtualRoot pVirtualRoot,
                     final DirectoryScanner pDirectoryScanner) {
        virtualRoot = pVirtualRoot;
        directoryScanner = pDirectoryScanner;
    }

    @Override
    public void init(final BundleContext bundleContext, final DependencyManager dependencyManager) throws Exception {
        dependencyManager.add(createComponent().
                setImplementation(directoryScanner));
        dependencyManager.add(createComponent().
                setImplementation(virtualRoot).
                setComposition("getComposition").
                add(createServiceDependency().
                        setService(FileObserver.class).
                        setCallbacks("addObserver", "removeObserver")
                ).
                add(createServiceDependency().
                        setService(WatchedDirectory.class).
                        setCallbacks("addRoot", "removeRoot")
                ).
                add(createSmartSwitchBuilder(ExecutorService.class).
                        setFilter("(sourcepond.io.fileobserver.observerexecutor=*)").
                        setShutdownHook(e -> e.shutdown()).
                        build(() -> newWorkStealingPool(5)
                        ).setAutoConfig("observerExecutor")).
                add(createSmartSwitchBuilder(ExecutorService.class).
                        setFilter("(sourcepond.io.fileobserver.directorywalkerexecutor=*)").
                        setShutdownHook(e -> e.shutdown()).
                        build(() -> newCachedThreadPool()).setAutoConfig("directoryWalkerExecutor")
                ).
                add(createServiceDependency().
                    setService(ResourcesFactory.class).
                    setRequired(true)));
    }
}
