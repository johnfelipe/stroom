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

package stroom.meta.impl.db;

import stroom.db.util.JooqUtil;
import stroom.meta.shared.MetaFieldNames;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static stroom.meta.impl.db.jooq.tables.MetaKey.META_KEY;

@Singleton
class MetaKeyServiceImpl implements MetaKeyService {
//    private static final Map<String, MetaFieldUse> SYSTEM_ATTRIBUTE_FIELD_TYPE_MAP;

    private static final String REC_READ = MetaFieldNames.REC_READ;
    private static final String REC_WRITE = MetaFieldNames.REC_WRITE;
    private static final String REC_INFO = MetaFieldNames.REC_INFO;
    private static final String REC_WARN = MetaFieldNames.REC_WARN;
    private static final String REC_ERROR = MetaFieldNames.REC_ERROR;
    private static final String REC_FATAL = MetaFieldNames.REC_FATAL;
    private static final String DURATION = MetaFieldNames.DURATION;
    //    private static final String NODE = StreamDataSource.NODE;
//    private static final String FEED = StreamDataSource.FEED;
    private static final String FILE_SIZE = MetaFieldNames.FILE_SIZE;
    private static final String STREAM_SIZE = MetaFieldNames.RAW_SIZE;

//    static {
//        final HashMap<String, MetaFieldUse> map = new HashMap<>();
//        map.put(REC_READ, MetaFieldUse.COUNT_IN_DURATION_FIELD);
//        map.put(REC_WRITE, MetaFieldUse.COUNT_IN_DURATION_FIELD);
//        map.put(REC_INFO, MetaFieldUse.COUNT_IN_DURATION_FIELD);
//        map.put(REC_WARN, MetaFieldUse.COUNT_IN_DURATION_FIELD);
//        map.put(REC_ERROR, MetaFieldUse.COUNT_IN_DURATION_FIELD);
//        map.put(REC_FATAL, MetaFieldUse.COUNT_IN_DURATION_FIELD);
//        map.put(DURATION, MetaFieldUse.DURATION_FIELD);
//        map.put(NODE, MetaFieldUse.FIELD);
//        map.put(FEED, MetaFieldUse.FIELD);
//        map.put(FILE_SIZE, MetaFieldUse.SIZE_FIELD);
//        map.put(RAW_SIZE, MetaFieldUse.SIZE_FIELD);
//
//        SYSTEM_ATTRIBUTE_FIELD_TYPE_MAP = Collections.unmodifiableMap(map);
//    }
//
//
//

    private final ConnectionProvider connectionProvider;
    private final Map<Integer, String> idToNameCache = new HashMap<>();
    private final Map<String, Integer> nameToIdCache = new HashMap<>();

    @Inject
    MetaKeyServiceImpl(final ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        setup();
    }

    @Override
    public Optional<String> getNameForId(final int keyId) {
        return Optional.ofNullable(idToNameCache.get(keyId));
    }

    @Override
    public Optional<Integer> getIdForName(final String name) {
        return Optional.ofNullable(nameToIdCache.get(name));
    }

    private void setup() {
        create(REC_READ, MetaType.COUNT_IN_DURATION_FIELD);
        create(REC_WRITE, MetaType.COUNT_IN_DURATION_FIELD);
        create(REC_INFO, MetaType.COUNT_IN_DURATION_FIELD);
        create(REC_WARN, MetaType.COUNT_IN_DURATION_FIELD);
        create(REC_ERROR, MetaType.COUNT_IN_DURATION_FIELD);
        create(REC_FATAL, MetaType.COUNT_IN_DURATION_FIELD);
        create(DURATION, MetaType.DURATION_FIELD);
//        create(NODE, MetaFieldUse.FIELD);
//        create(FEED, MetaFieldUse.FIELD);
        create(FILE_SIZE, MetaType.SIZE_FIELD);
        create(STREAM_SIZE, MetaType.SIZE_FIELD);

        fillCache();
    }

    private void fillCache() {
        JooqUtil.context(connectionProvider, context -> context
                .select(META_KEY.ID, META_KEY.NAME)
                .from(META_KEY)
                .fetch()
                .forEach(r -> {
                    idToNameCache.put(r.value1(), r.value2());
                    nameToIdCache.put(r.value2(), r.value1());
                }));
    }
//
//    private Integer get(final String name) {
//        Integer id = idToNameCache.get(name);
//        if (id != null) {
//            return id;
//        }
//
//        JooqUtil.context(connectionProvider, context -> context
//
//            id = create
//                    .select(STRM_TYPE.ID)
//                    .from(STRM_TYPE)
//                    .where(STRM_TYPE.NAME.eq(name))
//                    .fetchOne(STRM_TYPE.ID);
//
//        } catch (final SQLException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw new RuntimeException(e.getMessage(), e);
//        }
//
//        if (id != null) {
//            idToNameCache.put(name, id);
//        }
//
//        return id;
//    }

    private void create(final String name, final MetaType type) {
        JooqUtil.context(connectionProvider, context -> context
                .insertInto(META_KEY, META_KEY.NAME, META_KEY.FIELD_TYPE)
                .values(name, type.getPrimitiveValue())
                .onDuplicateKeyIgnore()
                .execute());
    }
//
//    @Override
//    public void clear() {
//        idToNameCache.clear();
//    }


//
//
//
//
//
//
//
//    final BaseResultList<MetaKey> list = metaKeyService
//            .find(new FindMetaKeyCriteria());
//    final HashSet<String> existingItems = new HashSet<>();
//        for (final MetaKey metaKey : list) {
//        existingItems.add(metaKey.getName());
//    }
//        for (final String name : MetaConstants.SYSTEM_ATTRIBUTE_FIELD_TYPE_MAP.keySet()) {
//        if (!existingItems.contains(name)) {
//            try {
//                metaKeyService.save(new MetaKey(name,
//                        MetaConstants.SYSTEM_ATTRIBUTE_FIELD_TYPE_MAP.get(name)));
//            } catch (final RuntimeException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//

//        extends SystemEntityServiceImpl<MetaKey, FindMetaKeyCriteria>
//        implements MetaKeyService, Clearable {
//    private static final int MAX_CACHE_ENTRIES = 1000;
//
//    private final LoadingCache<String, BaseResultList<MetaKey>> idToNameCache;
//
//    @Inject
//    @SuppressWarnings("unchecked")
//    MetaKeyServiceImpl(final StroomEntityManager entityManager,
//                                  final Security security,
//                                  final CacheManager cacheManager) {
//        super(entityManager, security);
//        final CacheLoader<String, BaseResultList<MetaKey>> cacheLoader = CacheLoader.from(k -> find(new FindMetaKeyCriteria()));
//        final CacheBuilder cacheBuilder = CacheBuilder.newBuilder()
//                .maximumSize(MAX_CACHE_ENTRIES)
//                .expireAfterAccess(10, TimeUnit.MINUTES);
//        idToNameCache = cacheBuilder.build(cacheLoader);
//        cacheManager.registerCache("Stream Attribute Key Cache", cacheBuilder, idToNameCache);
//    }
//
//    @Override
//    public Class<MetaKey> getEntityClass() {
//        return MetaKey.class;
//    }
//
//    @Override
//    public BaseResultList<MetaKey> findAll() {
//        return idToNameCache.getUnchecked("findAll");
//    }
//
//    @Override
//    public FindMetaKeyCriteria createCriteria() {
//        return new FindMetaKeyCriteria();
//    }
//
//    @Override
//    protected FieldMap createFieldMap() {
//        return super.createFieldMap()
//                .add(FindMetaKeyCriteria.FIELD_NAME, MetaKey.NAME, "name");
//    }
//
//    @Override
//    public void clear() {
//        CacheUtil.clear(idToNameCache);
//    }
//
//    @Override
//    protected String permission() {
//        return null;
//    }

    void clear() {
        idToNameCache.clear();
        nameToIdCache.clear();
        deleteAll();
        setup();
    }

    int deleteAll() {
        return JooqUtil.contextResult(connectionProvider, context -> context
                .delete(META_KEY)
                .execute());
    }
}
