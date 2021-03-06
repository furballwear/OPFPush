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

package org.onepf.opfpush.model;

/**
 * The type of messages that are received by {@link org.onepf.opfpush.receiver.OPFPushReceiver}.
 *
 * @author Roman Savin
 * @since 25.12.14
 */
public enum MessageType {

    /**
     * Indicates that the server deleted some pending messages because they were collapsible.
     */
    MESSAGE_TYPE_DELETED,

    /**
     * Indicates a regular message.
     */
    MESSAGE_TYPE_MESSAGE
}
