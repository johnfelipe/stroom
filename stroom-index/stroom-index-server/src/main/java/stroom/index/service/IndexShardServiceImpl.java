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
 *
 */

package stroom.index.service;

import stroom.docref.DocRef;
import stroom.entity.shared.PermissionException;
import stroom.index.IndexStructure;
import stroom.index.IndexStructureCache;
import stroom.index.LuceneVersionUtil;
import stroom.index.dao.IndexShardDao;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.IndexDoc;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShardKey;
import stroom.security.api.Security;
import stroom.security.api.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.PermissionNames;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.ModelStringUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class IndexShardServiceImpl implements IndexShardService {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(IndexShardServiceImpl.class);

    private final Security security;
    private final IndexVolumeService indexVolumeService;
    private final IndexStructureCache indexStructureCache;
    private final SecurityContext securityContext;
    private final IndexShardDao indexShardDao;

    @Inject
    IndexShardServiceImpl(final Security security,
                          final IndexVolumeService indexVolumeService,
                          final IndexStructureCache indexStructureCache,
                          final SecurityContext securityContext,
                          final IndexShardDao indexShardDao) {
        this.security = security;
        this.indexVolumeService = indexVolumeService;
        this.indexStructureCache = indexStructureCache;
        this.securityContext = securityContext;
        this.indexShardDao = indexShardDao;
    }

    @Override
    public IndexShard loadById(final Long id) {
        return security.secureResult(() -> indexShardDao.loadById(id));
    }

    @Override
    public List<IndexShard> find(final FindIndexShardCriteria criteria) {
        return security.secureResult(() -> indexShardDao.find(criteria));
    }

    @Override
    public IndexShard createIndexShard(final IndexShardKey indexShardKey,
                                       final String ownerNodeName) {
        return security.secureResult(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            final IndexStructure indexStructure = indexStructureCache.get(new DocRef(IndexDoc.DOCUMENT_TYPE, indexShardKey.getIndexUuid()));
            final IndexDoc index = indexStructure.getIndex();

            return indexShardDao.create(indexShardKey, index.getVolumeGroupName(), ownerNodeName, LuceneVersionUtil.getCurrentVersion());
        });
    }

    @Override
    public Boolean delete(final IndexShard entity) {
        return security.secureResult(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            if (!securityContext.hasDocumentPermission(IndexDoc.DOCUMENT_TYPE, entity.getIndexUuid(), DocumentPermissionNames.DELETE)) {
                throw new PermissionException(securityContext.getUserId(), "You do not have permission to delete index shard");
            }

            indexShardDao.delete(entity.getId());

            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean setStatus(final Long id,
                             final IndexShard.IndexShardStatus status) {
        return security.secureResult(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            indexShardDao.setStatus(id, status);
            return Boolean.TRUE;
        });
    }

    @Override
    public void update(final long indexShardId,
                       final Integer documentCount,
                       final Long commitDurationMs,
                       final Long commitMs,
                       final Long fileSize) {
        security.secure(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            // Output some debug so we know how long commits are taking.
            LOGGER.debug(() -> String.format("Documents written %s (%s)",
                    documentCount,
                    ModelStringUtil.formatDurationString(commitDurationMs)));
            indexShardDao.update(indexShardId, documentCount, commitDurationMs, commitMs, fileSize);
        });
    }
}
