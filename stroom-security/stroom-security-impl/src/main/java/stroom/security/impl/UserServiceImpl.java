/*
 * Copyright 2017 Crown Copyright
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
import stroom.security.dao.UserDao;
import stroom.security.service.UserService;
import stroom.security.shared.FindUserCriteria;
import stroom.security.shared.PermissionNames;
import stroom.security.shared.User;
import stroom.security.shared.UserRef;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
class UserServiceImpl implements UserService {
    private final Security security;

    private final AuthenticationConfig securityConfig;
    private final UserDao userDao;

    @Inject
    UserServiceImpl(final Security security,
                    final AuthenticationConfig securityConfig,
                    final UserDao userDao) {
        this.security = security;
        this.securityConfig = securityConfig;
        this.userDao = userDao;
    }

    @Override
    public UserRef getUserByName(final String name) {
        if (name != null && name.trim().length() > 0) {
            final User user = userDao.getUserByName(name);
            if (user != null) {
                // Make sure this is the user that was requested.
                if (!user.getName().equals(name)) {
                    throw new RuntimeException("Unexpected: returned user name does not match requested user name");
                }
                return UserRefFactory.create(user);
            }
        }

        return null;
    }

    @Override
    public List<User> find(final FindUserCriteria criteria) {
        return userDao.find(criteria.getGroup(), criteria.getName().getString());
    }

    @Override
    public List<UserRef> findUsersInGroup(final String groupUuid) {
        return userDao.findUsersInGroup(groupUuid).stream()
                .map(UserRefFactory::create)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRef> findGroupsForUser(final String userUuid) {
        return userDao.findGroupsForUser(userUuid).stream()
                .map(UserRefFactory::create)
                .collect(Collectors.toList());
    }

    @Override
    public UserRef createUser(final String name) {
        return security.secureResult(PermissionNames.MANAGE_USERS_PERMISSION,
                () -> Optional.of(userDao.createUser(name)).map(UserRefFactory::create).orElse(null)
        );
    }

    @Override
    public UserRef createUserGroup(final String name) {
        return security.secureResult(PermissionNames.MANAGE_USERS_PERMISSION,
                () -> Optional.of(userDao.createUserGroup(name)).map(UserRefFactory::create).orElse(null)
        );
    }

    @Override
    public User loadByUuid(final String uuid) {
        return userDao.getByUuid(uuid);
    }

    @Override
    public User save(User user) {
        if (null != user.getUuid()) {
            return security.secureResult(PermissionNames.MANAGE_USERS_PERMISSION,
                    () -> userDao.getByUuid(user.getUuid()));
        } else {
            return security.secureResult(PermissionNames.MANAGE_USERS_PERMISSION,
                    () -> user.getIsGroup() ?
                            userDao.createUser(user.getName()) :
                            userDao.createUserGroup(user.getName()));
        }
    }

    @Override
    public Boolean delete(final String userUuid) {
        security.secure(PermissionNames.MANAGE_USERS_PERMISSION,
                () -> userDao.deleteUser(userUuid));
        return true;
    }

    @Override
    public void addUserToGroup(final String userUuid, final String groupUuid) {
        security.secure(PermissionNames.MANAGE_USERS_PERMISSION, () -> {
            userDao.addUserToGroup(userUuid, groupUuid);
        });
    }

    @Override
    public void removeUserFromGroup(final String userUuid, final String groupUuid) {
        security.secure(PermissionNames.MANAGE_USERS_PERMISSION, () -> {
            userDao.removeUserFromGroup(userUuid, groupUuid);
        });
    }

    @Override
    public String getNamePattern() {
        return securityConfig.getUserNamePattern();
    }
}
