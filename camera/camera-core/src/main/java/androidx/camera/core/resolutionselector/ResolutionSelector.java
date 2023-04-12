/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.resolutionselector;

import static androidx.camera.core.resolutionselector.AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.UseCase;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A set of requirements and priorities used to select a resolution for the {@link UseCase}.
 *
 * <p>The resolution selection mechanism is determined by the following three steps:
 * <ol>
 *     <li> Collect the supported output sizes and add them to the candidate resolution list.
 *     <li> Filter and sort the candidate resolution list according to the {@link Builder}
 *     resolution settings.
 *     <li> Consider all the resolution selector settings of bound {@link UseCase}s to find the
 *     resolution that best suits each {@link UseCase}.
 * </ol>
 *
 * <p>For the first step, all supported resolution output sizes are added to the candidate
 * resolution list as the starting point.
 *
 * <p>ResolutionSelector provides the following function for applications to adjust the candidate
 * resolution settings.
 * <ul>
 *     <li> {@link Builder#setHighResolutionEnabledFlag(int)}
 * </ul>
 *
 * <p>For the second step, ResolutionSelector provides the following three functions for
 * applications to determine which resolution should be selected with higher priority.
 * <ul>
 *     <li> {@link Builder#setAspectRatioStrategy(AspectRatioStrategy)}
 *     <li> {@link Builder#setResolutionStrategy(ResolutionStrategy)}
 *     <li> {@link Builder#setResolutionFilter(ResolutionFilter)}
 * </ul>
 *
 * <p>CameraX sorts the collected sizes according to the specified aspect ratio and resolution
 * strategies. The aspect ratio strategy has precedence over the resolution strategy for sorting
 * the resolution candidate list. If applications specify a custom resolution filter, CameraX
 * passes the resulting sizes list, sorted by the specified aspect ratio and resolution
 * strategies, to the resolution filter to get the final desired list.
 *
 * <p>Different types of {@link UseCase}s might have their own default settings. You can see the
 * {@link UseCase} builders’ {@code setResolutionSelector()} function to know the details for each
 * type of {@link UseCase}.
 *
 * <p>In the third step, CameraX selects the final resolution for the {@link UseCase} based on the
 * camera device's hardware level, capabilities, and the bound {@link UseCase} combination.
 * Applications can check which resolution is finally selected by using the {@link UseCase}'s
 * {@code getResolutionInfo()} function.
 *
 * <p>Note that a ResolutionSelector with more restricted settings may result in that no
 * resolution can be selected to use. Applications will receive {@link IllegalArgumentException}
 * when binding the {@link UseCase}s with such kind of ResolutionSelector. Applications can
 * specify the {@link AspectRatioStrategy} and {@link ResolutionStrategy} with proper fallback
 * rules to avoid the {@link IllegalArgumentException} or try-catch it and show a proper message
 * to the end users.
 *
 * <p>When creating a ResolutionSelector instance, the
 * {@link AspectRatioStrategy#RATIO_4_3_FALLBACK_AUTO_STRATEGY} will be the default
 * {@link AspectRatioStrategy} if it is not set.
 * {@link ResolutionSelector#HIGH_RESOLUTION_FLAG_OFF} is the default value of high resolution
 * enabled flag. However, if neither the {@link ResolutionStrategy} nor the
 * {@link ResolutionFilter} are set, there will be no default value specified.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ResolutionSelector {
    /**
     * This flag disables high resolution support.
     */
    public static final int HIGH_RESOLUTION_FLAG_OFF = 0;
    /**
     * This flag enables high resolution in the default sensor pixel mode.
     *
     * <p>This flag allows CameraX to select the highest resolution output sizes available on the
     * camera device. The high resolution is retrieved via the
     * {@link android.hardware.camera2.params.StreamConfigurationMap#getHighResolutionOutputSizes(int)}
     * from the stream configuration map obtained with the
     * {@link android.hardware.camera2.CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP}
     * camera characteristics. However, please note that using a high resolution may result in
     * slower capture times. Please see the javadoc of
     * {@link android.hardware.camera2.params.StreamConfigurationMap#getHighResolutionOutputSizes(int)}
     * for more details.
     *
     * <p>Since Android 12, some devices might support a maximum resolution sensor pixel mode,
     * which allows them to capture additional ultra high resolutions retrieved from
     * {@link android.hardware.camera2.CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION}
     * . Enabling high resolution with this flag does not allow applications to select those
     * ultra high resolutions.
     */
    public static final int HIGH_RESOLUTION_FLAG_ON = 1;

    @IntDef({HIGH_RESOLUTION_FLAG_OFF, HIGH_RESOLUTION_FLAG_ON})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface HighResolutionFlag {
    }
    @NonNull
    private final AspectRatioStrategy mAspectRatioStrategy;
    @Nullable
    private final ResolutionStrategy mResolutionStrategy;
    @Nullable
    private final ResolutionFilter mResolutionFilter;
    @HighResolutionFlag
    private final int mHighResolutionEnabledFlag;

    ResolutionSelector(
            @NonNull AspectRatioStrategy aspectRatioStrategy,
            @Nullable ResolutionStrategy resolutionStrategy,
            @Nullable ResolutionFilter resolutionFilter,
            @HighResolutionFlag int highResolutionEnabledFlag) {
        mAspectRatioStrategy = aspectRatioStrategy;
        mResolutionStrategy = resolutionStrategy;
        mResolutionFilter = resolutionFilter;
        mHighResolutionEnabledFlag = highResolutionEnabledFlag;
    }

    /**
     * Returns the specified {@link AspectRatioStrategy}, or
     * {@link AspectRatioStrategy#RATIO_4_3_FALLBACK_AUTO_STRATEGY} if none is specified when
     * creating the ResolutionSelector.
     */
    @NonNull
    public AspectRatioStrategy getAspectRatioStrategy() {
        return mAspectRatioStrategy;
    }

    /**
     * Returns the specified {@link ResolutionStrategy}, or null if not specified.
     */
    @Nullable
    public ResolutionStrategy getResolutionStrategy() {
        return mResolutionStrategy;
    }

    /**
     * Returns the specified {@link ResolutionFilter} implementation, or null if not specified.
     */
    @Nullable
    public ResolutionFilter getResolutionFilter() {
        return mResolutionFilter;
    }

    /**
     * Returns the specified high resolution enabled flag.
     */
    @HighResolutionFlag
    public int getHighResolutionEnabledFlag() {
        return mHighResolutionEnabledFlag;
    }

    /**
     * Builder for a {@link ResolutionSelector}.
     */
    public static final class Builder {
        @Nullable
        private AspectRatioStrategy mAspectRatioStrategy = RATIO_4_3_FALLBACK_AUTO_STRATEGY;
        @Nullable
        private ResolutionStrategy mResolutionStrategy = null;
        @Nullable
        private ResolutionFilter mResolutionFilter = null;
        @HighResolutionFlag
        private int mHighResolutionEnabledFlag = HIGH_RESOLUTION_FLAG_OFF;

        /**
         * Creates a Builder instance.
         */
        public Builder() {
        }

        private Builder(@NonNull ResolutionSelector resolutionSelector) {
            mAspectRatioStrategy = resolutionSelector.getAspectRatioStrategy();
            mResolutionStrategy = resolutionSelector.getResolutionStrategy();
            mResolutionFilter = resolutionSelector.getResolutionFilter();
            mHighResolutionEnabledFlag = resolutionSelector.getHighResolutionEnabledFlag();
        }

        /**
         * Creates a Builder from an existing resolution selector.
         */
        @NonNull
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static Builder fromResolutionSelector(
                @NonNull ResolutionSelector resolutionSelector) {
            return new Builder(resolutionSelector);
        }

        /**
         * Sets the aspect ratio selection strategy for the {@link UseCase}. The aspect ratio
         * selection strategy determines how the {@link UseCase} will choose the aspect ratio of
         * the captured image.
         *
         * <p>If the aspect ratio strategy is not specified,
         * {@link AspectRatioStrategy#RATIO_4_3_FALLBACK_AUTO_STRATEGY} will be used as the default.
         */
        @NonNull
        public Builder setAspectRatioStrategy(@NonNull AspectRatioStrategy aspectRatioStrategy) {
            mAspectRatioStrategy = aspectRatioStrategy;
            return this;
        }

        /**
         * Sets the resolution selection strategy for the {@link UseCase}. The resolution selection
         * strategy determines how the {@link UseCase} will choose the resolution of the captured
         * image.
         */
        @NonNull
        public Builder setResolutionStrategy(@NonNull ResolutionStrategy resolutionStrategy) {
            mResolutionStrategy = resolutionStrategy;
            return this;
        }

        /**
         * Sets the resolution filter to output the final desired sizes list. The resolution
         * filter will filter out unsuitable sizes and sort the resolution list in the preferred
         * order. The preferred order is the order in which the resolutions should be tried first.
         */
        @NonNull
        public Builder setResolutionFilter(@NonNull ResolutionFilter resolutionFilter) {
            mResolutionFilter = resolutionFilter;
            return this;
        }

        /**
         * Sets high resolutions enabled flag to allow the application to select high
         * resolutions for the {@link UseCase}s. This will enable the application to choose high
         * resolutions for the captured image, which may result in better quality images.
         *
         * <p>If not specified, the default setting is
         * {@link ResolutionSelector#HIGH_RESOLUTION_FLAG_OFF}.
         */
        @NonNull
        public Builder setHighResolutionEnabledFlag(@HighResolutionFlag int flag) {
            mHighResolutionEnabledFlag = flag;
            return this;
        }

        /**
         * Builds the resolution selector. This will create a resolution selector that can be
         * used to select the desired resolution for the captured image.
         */
        @NonNull
        public ResolutionSelector build() {
            return new ResolutionSelector(mAspectRatioStrategy, mResolutionStrategy,
                    mResolutionFilter, mHighResolutionEnabledFlag);
        }
    }
}
