package stroom.meta.impl.db;

import org.jooq.Condition;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.db.util.JooqUtil;
import stroom.query.api.v2.ExpressionOperator.Builder;
import stroom.util.shared.BaseResultList;
import stroom.util.shared.IdSet;
import stroom.util.shared.PageRequest;
import stroom.util.shared.Sort.Direction;
import stroom.meta.impl.db.ExpressionMapper.TermHandler;
import stroom.meta.impl.db.MetaExpressionMapper.MetaTermHandler;
import stroom.meta.impl.db.jooq.tables.MetaFeed;
import stroom.meta.impl.db.jooq.tables.MetaProcessor;
import stroom.meta.impl.db.jooq.tables.MetaType;
import stroom.meta.impl.db.jooq.tables.MetaVal;
import stroom.meta.shared.AttributeMap;
import stroom.meta.shared.EffectiveMetaDataCriteria;
import stroom.meta.shared.FindMetaCriteria;
import stroom.meta.shared.Meta;
import stroom.meta.shared.MetaFieldNames;
import stroom.meta.shared.MetaProperties;
import stroom.meta.shared.MetaRow;
import stroom.meta.shared.MetaSecurityFilter;
import stroom.meta.shared.MetaService;
import stroom.meta.shared.Status;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm;
import stroom.security.api.Security;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.PermissionNames;
import stroom.util.date.DateUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.selectDistinct;
import static stroom.meta.impl.db.jooq.tables.Meta.META;
import static stroom.meta.impl.db.jooq.tables.MetaFeed.META_FEED;
import static stroom.meta.impl.db.jooq.tables.MetaProcessor.META_PROCESSOR;
import static stroom.meta.impl.db.jooq.tables.MetaType.META_TYPE;
import static stroom.meta.impl.db.jooq.tables.MetaVal.META_VAL;

@Singleton
class MetaServiceImpl implements MetaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaServiceImpl.class);

    private final ConnectionProvider connectionProvider;
    private final MetaFeedService feedService;
    private final MetaTypeService dataTypeService;
    private final MetaProcessorService processorService;
    private final MetaKeyService metaKeyService;
    private final MetaValueService metaValueService;
    private final MetaSecurityFilter dataSecurityFilter;
    private final Security security;

    private final stroom.meta.impl.db.jooq.tables.Meta meta = META.as("m");
    private final MetaFeed metaFeed = META_FEED.as("f");
    private final MetaType metaType = META_TYPE.as("t");
    private final MetaProcessor metaProcessor = META_PROCESSOR.as("p");
    private final MetaVal metaVal = META_VAL.as("v");

    private final ExpressionMapper expressionMapper;
    private final MetaExpressionMapper metaExpressionMapper;

    @Inject
    MetaServiceImpl(final ConnectionProvider connectionProvider,
                    final MetaFeedService feedService,
                    final MetaTypeService dataTypeService,
                    final MetaProcessorService processorService,
                    final MetaKeyService metaKeyService,
                    final MetaValueService metaValueService,
                    final MetaSecurityFilter dataSecurityFilter,
                    final Security security) {
        this.connectionProvider = connectionProvider;
        this.feedService = feedService;
        this.dataTypeService = dataTypeService;
        this.processorService = processorService;
        this.metaKeyService = metaKeyService;
        this.metaValueService = metaValueService;
        this.dataSecurityFilter = dataSecurityFilter;
        this.security = security;

        // Standard fields.
        final Map<String, TermHandler<?>> termHandlers = new HashMap<>();
        termHandlers.put(MetaFieldNames.ID, new TermHandler<>(meta.ID, Long::valueOf));
        termHandlers.put(MetaFieldNames.FEED_NAME, new TermHandler<>(meta.FEED_ID, feedService::getOrCreate));
        termHandlers.put(MetaFieldNames.FEED_ID, new TermHandler<>(meta.FEED_ID, Integer::valueOf));
        termHandlers.put(MetaFieldNames.TYPE_NAME, new TermHandler<>(meta.TYPE_ID, dataTypeService::getOrCreate));
        termHandlers.put(MetaFieldNames.PIPELINE_UUID, new TermHandler<>(metaProcessor.PIPELINE_UUID, value -> value));
        termHandlers.put(MetaFieldNames.PARENT_ID, new TermHandler<>(meta.PARENT_ID, Long::valueOf));
        termHandlers.put(MetaFieldNames.TASK_ID, new TermHandler<>(meta.TASK_ID, Long::valueOf));
        termHandlers.put(MetaFieldNames.PROCESSOR_ID, new TermHandler<>(meta.PROCESSOR_ID, Integer::valueOf));
        termHandlers.put(MetaFieldNames.STATUS, new TermHandler<>(meta.STATUS, value -> MetaStatusId.getPrimitiveValue(Status.valueOf(value.toUpperCase()))));
        termHandlers.put(MetaFieldNames.STATUS_TIME, new TermHandler<>(meta.STATUS_TIME, DateUtil::parseNormalDateTimeString));
        termHandlers.put(MetaFieldNames.CREATE_TIME, new TermHandler<>(meta.CREATE_TIME, DateUtil::parseNormalDateTimeString));
        termHandlers.put(MetaFieldNames.EFFECTIVE_TIME, new TermHandler<>(meta.EFFECTIVE_TIME, DateUtil::parseNormalDateTimeString));
        expressionMapper = new ExpressionMapper(termHandlers);


        // Extended meta fields.
        final Map<String, MetaTermHandler> metaTermHandlers = new HashMap<>();

//        metaTermHandlers.put(StreamDataSource.NODE, createMetaTermHandler(StreamDataSource.NODE));
        addMetaTermHandler(metaTermHandlers, MetaFieldNames.REC_READ);
        addMetaTermHandler(metaTermHandlers, MetaFieldNames.REC_WRITE);
        addMetaTermHandler(metaTermHandlers, MetaFieldNames.REC_INFO);
        addMetaTermHandler(metaTermHandlers, MetaFieldNames.REC_WARN);
        addMetaTermHandler(metaTermHandlers, MetaFieldNames.REC_ERROR);
        addMetaTermHandler(metaTermHandlers, MetaFieldNames.REC_FATAL);
        addMetaTermHandler(metaTermHandlers, MetaFieldNames.DURATION);
        addMetaTermHandler(metaTermHandlers, MetaFieldNames.FILE_SIZE);
        addMetaTermHandler(metaTermHandlers, MetaFieldNames.RAW_SIZE);

        metaExpressionMapper = new MetaExpressionMapper(metaTermHandlers);
    }

    private void addMetaTermHandler(final Map<String, MetaTermHandler> metaTermHandlers, final String fieldName) {
        final Optional<Integer> optional = metaKeyService.getIdForName(fieldName);
        optional.ifPresent(keyId -> {
            final MetaTermHandler handler = new MetaTermHandler(metaVal.META_KEY_ID,
                    keyId,
                    new TermHandler<>(metaVal.VAL, Long::valueOf));
            metaTermHandlers.put(fieldName, handler);
        });
    }

    @Override
    public Long getMaxId() {
        return JooqUtil.contextResult(connectionProvider, context -> context
                .select(max(meta.ID))
                .from(meta)
                .fetchOptional()
                .map(Record1::value1)
                .orElse(null));
    }

    @Override
    public Meta create(final MetaProperties metaProperties) {
        final Integer feedId = feedService.getOrCreate(metaProperties.getFeedName());
        final Integer typeId = dataTypeService.getOrCreate(metaProperties.getTypeName());
        final Integer processorId = processorService.getOrCreate(metaProperties.getProcessorUuid(), metaProperties.getProcessorFilterUuid(), metaProperties.getPipelineUuid());

        final long id = JooqUtil.contextResult(connectionProvider, context -> context
                .insertInto(META,
                        META.CREATE_TIME,
                        META.EFFECTIVE_TIME,
                        META.PARENT_ID,
                        META.STATUS,
                        META.STATUS_TIME,
                        META.TASK_ID,
                        META.FEED_ID,
                        META.TYPE_ID,
                        META.PROCESSOR_ID)
                .values(
                        metaProperties.getCreateMs(),
                        metaProperties.getEffectiveMs(),
                        metaProperties.getParentId(),
                        MetaStatusId.LOCKED,
                        metaProperties.getStatusMs(),
                        metaProperties.getProcessorTaskId(),
                        feedId,
                        typeId,
                        processorId)
                .returning(META.ID)
                .fetchOne()
                .getId()
        );

        return new Meta.Builder().id(id)
                .feedName(metaProperties.getFeedName())
                .typeName(metaProperties.getTypeName())
                .processorUuid(metaProperties.getProcessorUuid())
                .pipelineUuid(metaProperties.getPipelineUuid())
                .parentDataId(metaProperties.getParentId())
                .processorTaskId(metaProperties.getProcessorTaskId())
                .status(Status.LOCKED)
                .statusMs(metaProperties.getStatusMs())
                .createMs(metaProperties.getCreateMs())
                .effectiveMs(metaProperties.getEffectiveMs())
                .build();
    }

    @Override
    public Meta getMeta(final long id) {
        return getMeta(id, false);
    }

    @Override
    public Meta getMeta(final long id, final boolean anyStatus) {
        final Condition condition = getIdCondition(id, anyStatus, DocumentPermissionNames.READ);
        final List<Meta> list = find(condition, 0, 1);
        if (list == null || list.size() == 0) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public Meta updateStatus(final Meta meta, final Status currentStatus, final Status newStatus) {
        Objects.requireNonNull(meta, "Null data");

        final long now = System.currentTimeMillis();
        final int result = updateStatus(meta.getId(), newStatus, currentStatus, now, DocumentPermissionNames.UPDATE);
        if (result > 0) {
            return new Meta.Builder(meta)
                    .status(newStatus)
                    .statusMs(now)
                    .build();
        } else {
            final Meta existingMeta = getMeta(meta.getId());
            if (existingMeta == null) {
                throw new RuntimeException("Meta with id=" + meta.getId() + " does not exist");
            }

            if (currentStatus != existingMeta.getStatus()) {
                throw new RuntimeException("Unexpected status " +
                        existingMeta.getStatus() +
                        " (expected " +
                        currentStatus +
                        ")");
            }

            return null;
        }
    }

    private int updateStatus(final long id, final Status newStatus, final Status currentStatus, final long statusTime, final String permission) {
        Condition condition = getIdCondition(id, true, permission);

        // Add a condition if we should check current status.
        if (currentStatus != null) {
            condition = condition.and(meta.STATUS.eq(MetaStatusId.getPrimitiveValue(currentStatus)));
        }

        final Condition c = condition;

        return JooqUtil.contextResult(connectionProvider, context -> context
                .update(meta)
                .set(meta.STATUS, MetaStatusId.getPrimitiveValue(newStatus))
                .set(meta.STATUS_TIME, statusTime)
                .where(c)
                .execute());
//                    .returning(data.ID,
//                            metaFeed.NAME,
//                            metaType.NAME,
//                            metaProcessor.PIPELINE_UUID,
//                            meta.PARNT_STRM_ID,
//                            meta.STRM_TASK_ID,
//                            meta.FK_STRM_PROC_ID,
//                            meta.STAT,
//                            meta.STAT_MS,
//                            meta.CRT_MS,
//                            meta.EFFECT_MS)
//                    .fetchOptional()
//                    .map(r -> new Builder().id(data.ID.get(r))
//                            .feedName(metaFeed.NAME.get(r))
//                            .typeName(metaType.NAME.get(r))
//                            .pipelineUuid(metaProcessor.PIPELINE_UUID.get(r))
//                            .parentDataId(meta.PARNT_STRM_ID.get(r))
//                            .processorTaskId(meta.STRM_TASK_ID.get(r))
//                            .processorId(meta.FK_STRM_PROC_ID.get(r))
//                            .status(StreamStatusId.getStatus(data.STAT.get(r)))
//                            .statusMs(meta.STAT_MS.get(r))
//                            .createMs(meta.CRT_MS.get(r))
//                            .effectiveMs(meta.EFFECT_MS.get(r))
//                            .build())
//                    .orElse(null);

    }

    @Override
    public int updateStatus(final FindMetaCriteria criteria, final Status status) {
        // Decide which permission is needed for this update as logical deletes require delete permissions.
        String permission = DocumentPermissionNames.UPDATE;
        if (Status.DELETED.equals(status)) {
            permission = DocumentPermissionNames.DELETE;
        }

        final Condition condition = createCondition(criteria, permission);

        return JooqUtil.contextResult(connectionProvider, context -> context
                .update(meta)
                .set(meta.STATUS, MetaStatusId.getPrimitiveValue(status))
                .set(meta.STATUS_TIME, System.currentTimeMillis())
                .where(condition)
                .execute());
    }

    @Override
    public void addAttributes(final Meta meta, final AttributeMap attributes) {
        metaValueService.addAttributes(meta, attributes);
    }

    @Override
    public int delete(final long id) {
        return security.secureResult(PermissionNames.DELETE_DATA_PERMISSION, () -> doLogicalDelete(id, true));
    }

    @Override
    public int delete(final long id, final boolean lockCheck) {
        return security.secureResult(PermissionNames.DELETE_DATA_PERMISSION, () -> doLogicalDelete(id, lockCheck));
    }

    private int doLogicalDelete(final long id, final boolean lockCheck) {
        if (lockCheck) {
            final Meta meta = getMeta(id, true);

            // Don't bother to try and set the status of deleted data to be deleted.
            if (Status.DELETED.equals(meta.getStatus())) {
                return 0;
            }

            // Don't delete if the data is not unlocked and we are checking for unlocked.
            if (!Status.UNLOCKED.equals(meta.getStatus())) {
                return 0;
            }
        }

        // Ensure the user has permission to delete this data.
        final long now = System.currentTimeMillis();
        return updateStatus(id, Status.DELETED, null, now, DocumentPermissionNames.DELETE);
    }

    private SelectConditionStep<Record1<Long>> getMetaCondition(final ExpressionOperator expression) {
        if (expression == null) {
            return null;
        }

        final Condition condition = metaExpressionMapper.apply(expression);
        if (condition == null) {
            return null;
        }

        return selectDistinct(metaVal.META_ID)
                .from(metaVal)
                .where(condition);
    }

    @Override
    public BaseResultList<Meta> find(final FindMetaCriteria criteria) {
        final boolean fetchRelationships = criteria.isFetchRelationships();
        final PageRequest pageRequest = criteria.getPageRequest();
        if (fetchRelationships) {
            criteria.setPageRequest(null);
        }

        final IdSet idSet = criteria.getSelectedIdSet();
        // If for some reason we have been asked to match nothing then return nothing.
        if (idSet != null && idSet.getMatchNull() != null && idSet.getMatchNull()) {
            return BaseResultList.createPageResultList(Collections.emptyList(), criteria.getPageRequest(), null);
        }

        final Condition condition = createCondition(criteria, DocumentPermissionNames.READ);

        int offset = 0;
        int numberOfRows = 1000000;

        if (pageRequest != null) {
            offset = pageRequest.getOffset().intValue();
            numberOfRows = pageRequest.getLength();
        }

        List<Meta> results = find(condition, offset, numberOfRows);





        // Only return back children or parents?
        if (fetchRelationships) {
            final List<Meta> workingList = results;
            results = new ArrayList<>();

            for (final Meta stream : workingList) {
                Meta parent = stream;
                Meta lastParent = parent;

                // Walk up to the root of the tree
                while (parent.getParentMetaId() != null && (parent = findParent(parent)) != null) {
                    lastParent = parent;
                }

                // Add the match
                results.add(lastParent);

                // Add the children
                List<Meta> children = findChildren(criteria, Collections.singletonList(lastParent));
                while (children.size() > 0) {
                    results.addAll(children);
                    children = findChildren(criteria, children);
                }
            }

            final long maxSize = results.size();
            if (pageRequest != null && pageRequest.getOffset() != null) {
                // Move by an offset?
                if (pageRequest.getOffset() > 0) {
                    results = results.subList(pageRequest.getOffset().intValue(), results.size());
                }
            }
            if (pageRequest != null && pageRequest.getLength() != null) {
                if (results.size() > pageRequest.getLength()) {
                    results = results.subList(0, pageRequest.getLength() + 1);
                }
            }
            criteria.setPageRequest(pageRequest);
            return BaseResultList.createCriterialBasedList(results, criteria, maxSize);
        } else {
            return BaseResultList.createCriterialBasedList(results, criteria);
        }
    }

    private List<Meta> find(final Condition condition, final int offset, final int numberOfRows) {
        return JooqUtil.contextResult(connectionProvider, context -> context
                .select(
                        meta.ID,
                        metaFeed.NAME,
                        metaType.NAME,
                        metaProcessor.PROCESSOR_UUID,
                        metaProcessor.PROCESSOR_FILTER_UUID,
                        metaProcessor.PIPELINE_UUID,
                        meta.PARENT_ID,
                        meta.TASK_ID,
                        meta.STATUS,
                        meta.STATUS_TIME,
                        meta.CREATE_TIME,
                        meta.EFFECTIVE_TIME
                )
                .from(meta)
                .join(metaFeed).on(meta.FEED_ID.eq(metaFeed.ID))
                .join(metaType).on(meta.TYPE_ID.eq(metaType.ID))
                .leftOuterJoin(metaProcessor).on(meta.PROCESSOR_ID.eq(metaProcessor.ID))
                .where(condition)
                .orderBy(meta.ID)
                .limit(offset, numberOfRows)
                .fetch()
                .map(r -> new Meta.Builder()
                        .id(r.component1())
                        .feedName(r.component2())
                        .typeName(r.component3())
                        .processorUuid(r.component4())
                        .processorFilterUuid(r.component5())
                        .pipelineUuid(r.component6())
                        .parentDataId(r.component7())
                        .processorTaskId(r.component8())
                        .status(MetaStatusId.getStatus(r.component9()))
                        .statusMs(r.component10())
                        .createMs(r.component11())
                        .effectiveMs(r.component12())
                        .build()));
    }

    private List<Meta> findChildren(final FindMetaCriteria parentCriteria, final List<Meta> streamList) {
        final Set<String> excludedFields = Set.of(MetaFieldNames.ID, MetaFieldNames.PARENT_ID);
        final Builder builder = copyExpression(parentCriteria.getExpression(), excludedFields);

        final String parentIds = streamList.stream()
                .map(meta -> String.valueOf(meta.getId()))
                .collect(Collectors.joining(","));
        builder.addTerm(MetaFieldNames.PARENT_ID, ExpressionTerm.Condition.IN, parentIds);

        return simpleFind(builder.build());
    }


    private Meta findParent(final Meta meta) {
        final ExpressionOperator expression = new ExpressionOperator.Builder()
                .addTerm(MetaFieldNames.ID, ExpressionTerm.Condition.EQUALS, String.valueOf(meta.getParentMetaId()))
                .build();
        final List<Meta> parentList = simpleFind(expression);
        if (parentList != null && parentList.size() > 0) {
            return parentList.get(0);
        }
        return new Meta.Builder()
                .id(meta.getParentMetaId())
                .build();
    }



    private List<Meta> simpleFind(final ExpressionOperator expression) {
        final FindMetaCriteria criteria = new FindMetaCriteria(expression);
        final Condition condition = createCondition(criteria, DocumentPermissionNames.READ);

        int offset = 0;
        int numberOfRows = 1000000;

        PageRequest pageRequest = criteria.getPageRequest();
        if (pageRequest != null) {
            offset = pageRequest.getOffset().intValue();
            numberOfRows = pageRequest.getLength();
        }

        return find(condition, offset, numberOfRows);
    }

    private Builder copyExpression(final ExpressionOperator expressionOperator, final Set<String> excludedFields) {
        final Builder builder = new Builder(expressionOperator.getEnabled(), expressionOperator.getOp());
        if (expressionOperator.getChildren() != null) {
            expressionOperator.getChildren().forEach(expressionItem -> {
                if (expressionItem instanceof ExpressionTerm) {
                    final ExpressionTerm expressionTerm = (ExpressionTerm) expressionItem;
                    if (!excludedFields.contains(expressionTerm.getField())) {
                        builder.addTerm(expressionTerm);
                    }
                } else if (expressionItem instanceof ExpressionOperator) {
                    final ExpressionOperator operator = (ExpressionOperator) expressionItem;
                    builder.addOperator(copyExpression(operator, excludedFields).build());
                }
            });
        }
        return builder;
    }

    public int delete(final FindMetaCriteria criteria) {
        final Condition condition = createCondition(criteria, DocumentPermissionNames.DELETE);

        return JooqUtil.contextResult(connectionProvider, context -> context
                .deleteFrom(meta)
                .where(condition)
                .execute());
    }

    @Override
    public Set<Meta> findEffectiveData(final EffectiveMetaDataCriteria criteria) {
        // See if we can find a data that exists before the earliest specified time.
        final Optional<Long> optionalId = getMaxEffectiveDataIdBeforePeriod(criteria);

        final Set<Meta> set = new HashSet<>();
        if (optionalId.isPresent()) {
            // Get the data that occurs just before or ast the start of the period.
            final ExpressionOperator expression = new ExpressionOperator.Builder(Op.AND)
                    .addTerm(MetaFieldNames.ID, ExpressionTerm.Condition.EQUALS, String.valueOf(optionalId.get()))
                    .build();
            // There is no need to apply security here are is has been applied when finding the data id above.
            final Condition condition = expressionMapper.apply(expression);
            set.addAll(find(condition, 0, 1000));
        }

        // Now add all data that occurs within the requested period.
        final ExpressionOperator expression = new ExpressionOperator.Builder(Op.AND)
                .addTerm(MetaFieldNames.EFFECTIVE_TIME, ExpressionTerm.Condition.GREATER_THAN, DateUtil.createNormalDateTimeString(criteria.getEffectivePeriod().getFromMs()))
                .addTerm(MetaFieldNames.EFFECTIVE_TIME, ExpressionTerm.Condition.LESS_THAN, DateUtil.createNormalDateTimeString(criteria.getEffectivePeriod().getToMs()))
                .addTerm(MetaFieldNames.FEED_NAME, ExpressionTerm.Condition.EQUALS, criteria.getFeed())
                .addTerm(MetaFieldNames.TYPE_NAME, ExpressionTerm.Condition.EQUALS, criteria.getType())
                .addTerm(MetaFieldNames.STATUS, ExpressionTerm.Condition.EQUALS, Status.UNLOCKED.getDisplayValue())
                .build();

        final ExpressionOperator secureExpression = addPermissionConstraints(expression, DocumentPermissionNames.READ);
        final Condition condition = expressionMapper.apply(secureExpression);
        set.addAll(find(condition, 0, 1000));

        return set;
    }

    private Optional<Long> getMaxEffectiveDataIdBeforePeriod(final EffectiveMetaDataCriteria criteria) {
        final ExpressionOperator expression = new ExpressionOperator.Builder(Op.AND)
                .addTerm(MetaFieldNames.EFFECTIVE_TIME, ExpressionTerm.Condition.LESS_THAN_OR_EQUAL_TO, DateUtil.createNormalDateTimeString(criteria.getEffectivePeriod().getFromMs()))
                .addTerm(MetaFieldNames.FEED_NAME, ExpressionTerm.Condition.EQUALS, criteria.getFeed())
                .addTerm(MetaFieldNames.TYPE_NAME, ExpressionTerm.Condition.EQUALS, criteria.getType())
                .addTerm(MetaFieldNames.STATUS, ExpressionTerm.Condition.EQUALS, Status.UNLOCKED.getDisplayValue())
                .build();

        final ExpressionOperator secureExpression = addPermissionConstraints(expression, DocumentPermissionNames.READ);
        final Condition condition = expressionMapper.apply(secureExpression);

        return JooqUtil.contextResult(connectionProvider, context -> context
                .select(max(meta.ID))
                .from(meta)
                .where(condition)
                .fetchOptional()
                .map(Record1::value1));
    }

    @Override
    public List<String> getFeeds() {
        return feedService.list();
    }

    @Override
    public List<String> getTypes() {
        return dataTypeService.list();
    }

    @Override
    public int getLockCount() {
        return JooqUtil.contextResult(connectionProvider, context -> context
                .selectCount()
                .from(meta)
                .where(meta.STATUS.eq(MetaStatusId.LOCKED))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(0));
    }

    @Override
    public BaseResultList<MetaRow> findRows(final FindMetaCriteria criteria) {
        return security.useAsReadResult(() -> {
            // Cache Call


            final FindMetaCriteria findMetaCriteria = new FindMetaCriteria();
            findMetaCriteria.copyFrom(criteria);
            findMetaCriteria.setSort(MetaFieldNames.CREATE_TIME, Direction.DESCENDING, false);

//            findDataCriteria.setFetchSet(new HashSet<>());

            // Share the page criteria
            final BaseResultList<Meta> list = find(findMetaCriteria);

            if (list.size() > 0) {
//                // We need to decorate data with retention rules as a processing user.
//                final List<StreamDataRow> result = security.asProcessingUserResult(() -> {
//                    // Create a data retention rule decorator for adding data retention information to returned data attribute maps.
//                    List<DataRetentionRule> rules = Collections.emptyList();
//
//                    final DataRetentionService dataRetentionService = dataRetentionServiceProvider.get();
//                    if (dataRetentionService != null) {
//                        final DataRetentionPolicy dataRetentionPolicy = dataRetentionService.load();
//                        if (dataRetentionPolicy != null && dataRetentionPolicy.getRules() != null) {
//                            rules = dataRetentionPolicy.getRules();
//                        }
//                        final AttributeMapRetentionRuleDecorator ruleDecorator = new AttributeMapRetentionRuleDecorator(dictionaryStore, rules);

                // Query the database for the attribute values
//                        if (criteria.isUseCache()) {
                LOGGER.info("Loading attribute map from DB");
                final List<MetaRow> result = metaValueService.decorateDataWithAttributes(list);
//                        } else {
//                            LOGGER.info("Loading attribute map from filesystem");
//                            loadAttributeMapFromFileSystem(criteria, result, result, ruleDecorator);
//                        }
//                    }
//                });

                return new BaseResultList<>(result, list.getPageResponse().getOffset(),
                        list.getPageResponse().getTotal(), list.getPageResponse().isExact());
            }

            return new BaseResultList<>(Collections.emptyList(), list.getPageResponse().getOffset(),
                    list.getPageResponse().getTotal(), list.getPageResponse().isExact());
        });
    }

    @Override
    public List<MetaRow> findRelatedData(final long id, final boolean anyStatus) {
        // Get the starting row.
        final FindMetaCriteria findDataCriteria = new FindMetaCriteria(getIdExpression(id, anyStatus));
        BaseResultList<Meta> rows = find(findDataCriteria);
        final List<Meta> result = new ArrayList<>(rows);

        if (rows.size() > 0) {
            Meta row = rows.getFirst();
            addChildren(row, anyStatus, result);
            addParents(row, anyStatus, result);
        }

        result.sort(Comparator.comparing(Meta::getId));

        return metaValueService.decorateDataWithAttributes(result);
    }

    private void addChildren(final Meta parent, final boolean anyStatus, final List<Meta> result) {
        final BaseResultList<Meta> children = find(new FindMetaCriteria(getParentIdExpression(parent.getId(), anyStatus)));
        children.forEach(child -> {
            result.add(child);
            addChildren(child, anyStatus, result);
        });
    }

    private void addParents(final Meta child, final boolean anyStatus, final List<Meta> result) {
        if (child.getParentMetaId() != null) {
            final BaseResultList<Meta> parents = find(new FindMetaCriteria(getIdExpression(child.getParentMetaId(), anyStatus)));
            if (parents != null && parents.size() > 0) {
                parents.forEach(parent -> {
                    result.add(parent);
                    addParents(parent, anyStatus, result);
                });
            } else {
                // Add a dummy parent data as we don't seem to be able to get the real parent.
                // This might be because it is deleted or the user does not have access permissions.
                final Meta meta = new Meta.Builder()
                        .id(child.getParentMetaId())
                        .build();
                result.add(meta);
            }
        }
    }

    void clear() {
        deleteAll();
    }

    int deleteAll() {
        return JooqUtil.contextResult(connectionProvider, context -> context
                .delete(META)
                .execute());
    }

    private Condition getIdCondition(final long id, final boolean anyStatus, final String permission) {
        final ExpressionOperator secureExpression = addPermissionConstraints(getIdExpression(id, anyStatus), permission);
        return expressionMapper.apply(secureExpression);
    }

    private ExpressionOperator getIdExpression(final long id, final boolean anyStatus) {
        if (anyStatus) {
            return new ExpressionOperator.Builder(Op.AND)
                    .addTerm(MetaFieldNames.ID, ExpressionTerm.Condition.EQUALS, String.valueOf(id))
                    .build();
        }

        return new ExpressionOperator.Builder(Op.AND)
                .addTerm(MetaFieldNames.ID, ExpressionTerm.Condition.EQUALS, String.valueOf(id))
                .addTerm(MetaFieldNames.STATUS, ExpressionTerm.Condition.EQUALS, Status.UNLOCKED.getDisplayValue())
                .build();
    }

    private ExpressionOperator getParentIdExpression(final long id, final boolean anyStatus) {
        if (anyStatus) {
            return new ExpressionOperator.Builder(Op.AND)
                    .addTerm(MetaFieldNames.PARENT_ID, ExpressionTerm.Condition.EQUALS, String.valueOf(id))
                    .build();
        }

        return new ExpressionOperator.Builder(Op.AND)
                .addTerm(MetaFieldNames.PARENT_ID, ExpressionTerm.Condition.EQUALS, String.valueOf(id))
                .addTerm(MetaFieldNames.STATUS, ExpressionTerm.Condition.EQUALS, Status.UNLOCKED.getDisplayValue())
                .build();
    }

    private ExpressionOperator addPermissionConstraints(final ExpressionOperator expression, final String permission) {
        final ExpressionOperator filter = dataSecurityFilter.getExpression(permission).orElse(null);

        if (expression == null) {
            return filter;
        }

        if (filter != null) {
            final ExpressionOperator.Builder builder = new ExpressionOperator.Builder(Op.AND);
            builder.addOperator(expression);
            builder.addOperator(filter);
            return builder.build();
        }

        return expression;
    }

    private Condition createCondition(final FindMetaCriteria criteria, final String permission) {
        Condition condition;

        IdSet idSet = null;
        ExpressionOperator expression = null;

        if (criteria != null) {
            idSet = criteria.getSelectedIdSet();
            expression = criteria.getExpression();
        }

        final ExpressionOperator secureExpression = addPermissionConstraints(expression, permission);
        condition = expressionMapper.apply(secureExpression);

        // If we aren't being asked to match everything then add constraints to the expression.
        if (idSet != null && (idSet.getMatchAll() == null || !idSet.getMatchAll())) {
            condition = and(condition, meta.ID.in(idSet.getSet()));
        }

        // Get additional selection criteria based on meta data attributes;
        final SelectConditionStep<Record1<Long>> metaConditionStep = getMetaCondition(secureExpression);
        if (metaConditionStep != null) {
            condition = and(condition, meta.ID.in(metaConditionStep));
        }

        return condition;
    }

    private Condition and(final Condition c1, final Condition c2) {
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }
        return c1.and(c2);
    }
}
