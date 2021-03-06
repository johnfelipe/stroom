package stroom.security.dao;

import stroom.docref.DocRef;
import stroom.security.shared.DocumentPermissionJooq;

import java.util.Set;

public interface DocumentPermissionDao {

    Set<String> getPermissionsForDocumentForUser(String docRefUuid, String userUuid);

    DocumentPermissionJooq getPermissionsForDocument(String docRefUuid);

    void addPermission(String docRefUuid, String userUuid, String permission);

    void removePermission(String docRefUuid, String userUuid, String permission);

    void clearDocumentPermissionsForUser(String docRefUuid, String userUuid);

    void clearDocumentPermissions(String docRefUuid);

}
