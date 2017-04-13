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

/**
 * A {@link FileObserver} is able to specify which file changes should be observed. To do the necessary setup,
 * an object implementing this interface is passed to the {@link FileObserver#setup(DeliveryRestriction)} method
 * of a file-observer when it is being registered. Note:
 * <ul>
 * <li>If {@link #accept(Object...)} has never been called during the observer setup, the observer will then accept
 * any directory-key.</li>
 * <li>If {@link #accept(Object...)} has been called at least once, the observer will only accept
 * those directory-keys which has been passed as arguments.</li>
 * <li>When none of the {@code add*} methods has been called during the observer setup, any path modification or discard
 * will be delivered if it it has an accepted directory-key.</li>
 * <li>When one of the {@code add*} methods has been called at least once, a path modification or discard will only be
 * delivered it it has an accepted directory-key, and, {@link FileKey#getRelativePath()} matches at least one added
 * rule.</li>
 * </ul>
 * It is optional to restrict the file-observer so that it receives certain events only.
 * If nothing is set up, it will receive everything.
 */
public interface DeliveryRestriction {

    /**
     * <p>Determines, which directory-keys should be accepted by the {@link FileObserver}. This means, that a path
     * modification or discard is only delivered to the observer, if the directory-key of the associated {@link FileKey}
     * is contained in the keys specified, see {@link FileKey#getDirectoryKey()}.</p>
     * <p>
     * Note: if a key is specified as accepted (this method) and ignored (see {@link #ignore(Object...)}) at the same
     * time, then ignorance takes precedence, i.e. the key will not be accepted by the observer.</p>
     *
     * @param pDirectoryKeys Directory-keys to be accepted by the file-observer, must not be {@code null}
     * @throws NullPointerException Thrown, if a key is {@code null}
     */
    void accept(Object... pDirectoryKeys);

    /**
     *
     * @param pDirectoryKeys
     */
    void ignore(Object... pDirectoryKeys);

    void addGlob(String... pPatterns);

    void addRegex(String... pPatterns);

    void add(String pSyntax, String... pPatterns);

    void addGlob(int pStartIndexInclusive, int pEndIndexExlusive, String... pPatterns);

    void addRegex(int pStartIndexInclusive, int pEndIndexExlusive, String... pPatterns);

    void add(int pStartIndexInclusive, int pEndIndexExlusive, String pSyntax, String... pPatterns);
}
