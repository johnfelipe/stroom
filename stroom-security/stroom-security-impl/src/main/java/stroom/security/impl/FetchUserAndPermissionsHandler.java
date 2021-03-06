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

import stroom.security.api.Security;
import stroom.security.api.SecurityContext;
import stroom.security.impl.exception.AuthenticationException;
import stroom.security.shared.FetchUserAndPermissionsAction;
import stroom.security.shared.UserAndPermissions;
import stroom.security.shared.UserRef;
import stroom.security.util.UserTokenUtil;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;


class FetchUserAndPermissionsHandler extends AbstractTaskHandler<FetchUserAndPermissionsAction, UserAndPermissions> {
    private final Security security;
    private final SecurityContext securityContext;
    private final UserAndPermissionsHelper userAndPermissionsHelper;
    private final AuthenticationConfig securityConfig;

    @Inject
    FetchUserAndPermissionsHandler(final Security security,
                                   final SecurityContext securityContext,
                                   final UserAndPermissionsHelper userAndPermissionsHelper,
                                   final AuthenticationConfig securityConfig) {
        this.security = security;
        this.securityContext = securityContext;
        this.userAndPermissionsHelper = userAndPermissionsHelper;
        this.securityConfig = securityConfig;
    }

    @Override
    public UserAndPermissions exec(final FetchUserAndPermissionsAction task) {
        return security.insecureResult(() -> {
            final UserRef userRef = CurrentUserState.currentUserRef();
            if (userRef == null) {
                return null;
            }

            final boolean preventLogin = securityConfig.isPreventLogin();
            if (preventLogin) {
                security.asUser(UserTokenUtil.create(userRef.getName()), () -> {
                    if (!securityContext.isAdmin()) {
                        throw new AuthenticationException("Stroom is down for maintenance. Please try again later.");
                    }
                });
            }

            return new UserAndPermissions(userRef, userAndPermissionsHelper.get(userRef));
        });
    }
}
