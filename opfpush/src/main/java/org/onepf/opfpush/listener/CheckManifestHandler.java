/*
 * Copyright 2012-2015 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.opfpush.listener;

import android.support.annotation.NonNull;

/**
 * The interface for handling check manifest errors.
 *
 * @author Roman Savin
 * @since 22.04.2015
 */
public interface CheckManifestHandler {

    /**
     * Called when some check manifest error occurred.
     *
     * @param reportMessage The information about error.
     */
    void onCheckManifestError(@NonNull final String reportMessage);
}
