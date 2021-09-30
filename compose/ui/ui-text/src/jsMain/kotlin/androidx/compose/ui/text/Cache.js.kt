/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.text

// Extremely simple Cache interface which is enough for ui.text needs
internal interface Cache<K, V> {
    // get a value for [key] or load it by [loader] if doesn't exist
    fun get(key: K, loader: (K) -> V): V
}

// expire cache entries after `expireAfter` after last access
internal class ExpireAfterAccessCache<K, V>() : Cache<K, V> {
    internal val map = HashMap<K, V>()

    override fun get(key: K, loader: (K) -> V): V = map.getOrPut(key) {
        loader(key)
    }
}
