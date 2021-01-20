/*
 * Copyright 2021 The Android Open Source Project
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
// @exportToFramework:skipFile()

package androidx.appsearch.localstorage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.PackageIdentifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO(b/169883602): figure out if we still need a VisibilityStore in localstorage depending on
 * how we refactor the AppSearchImpl-VisibilityStore relationship.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VisibilityStore {

    /** No-op user id that won't have any visibility settings. */
    public static final int NO_OP_USER_ID = -1;

    /**
     * These cannot have any of the special characters used by AppSearchImpl (e.g. {@code
     * AppSearchImpl#PACKAGE_DELIMITER} or {@code AppSearchImpl#DATABASE_DELIMITER}.
     */
    public static final String PACKAGE_NAME = "VS#Pkg";

    public static final String DATABASE_NAME = "VS#Db";

    /** No-op implementation in local storage. */
    public VisibilityStore(@NonNull AppSearchImpl appSearchImpl, @NonNull Context context,
            int userId, @NonNull String globalQuerierPackage) {
    }

    /** No-op implementation in local storage. */
    public void initialize() {
    }

    /** No-op implementation in local storage. */
    public void setVisibility(@NonNull String prefix,
            @NonNull Set<String> schemasNotPlatformSurfaceable,
            @NonNull Map<String, List<PackageIdentifier>> schemasPackageAccessible) {
    }

    /** No-op implementation in local storage. */
    public boolean isSchemaSearchableByCaller(@NonNull String prefix,
            @NonNull String prefixedSchema, int callerUid) {
        return false;
    }

    /** No-op implementation in local storage. */
    public void handleReset() {
    }
}
