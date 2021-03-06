/*
 * Copyright 2016 Crown Copyright
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

package stroom.security.impl;

import stroom.security.shared.User;
import stroom.security.shared.UserRef;

public final class UserRefFactory {
    private UserRefFactory() {
        // Factory class.
    }

    public static UserRef create(final User user) {
        if (user == null) {
            return null;
        }

        final String type = "User";
        final String uuid = user.getUuid();
        final String name = user.getName();

        return new UserRef(type, uuid, name, user.getIsGroup(), true);
    }
}
