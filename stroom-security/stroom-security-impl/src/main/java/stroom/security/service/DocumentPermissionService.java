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

package stroom.security.service;

import stroom.security.shared.DocumentPermissions;

import java.util.Set;

public interface DocumentPermissionService {

    Set<String> getPermissionsForDocumentForUser(String docRefUuid,String userUuid);

    DocumentPermissions getPermissionsForDocument(String docRefUuid);

    void addPermission(String docRefUuid, String userUuid, String permission);

    void removePermission(String docRefUuid, String userUuid, String permission);

    void clearDocumentPermissionsForUser(String docRefUuid, String userUuid);

    void clearDocumentPermissions(String docRefUuid);
}
