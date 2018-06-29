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

package stroom.refdata;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import stroom.docref.DocRef;
import stroom.docstore.Persistence;
import stroom.docstore.Store;
import stroom.docstore.memory.MemoryPersistence;
import stroom.entity.DocumentPermissionCache;
import stroom.entity.shared.DocRefUtil;
import stroom.feed.MockFeedService;
import stroom.feed.shared.Feed;
import stroom.pipeline.PipelineStore;
import stroom.pipeline.PipelineStoreImpl;
import stroom.pipeline.shared.PipelineDoc;
import stroom.pipeline.shared.data.PipelineReference;
import stroom.pipeline.state.FeedHolder;
import stroom.refdata.offheapstore.AbstractRefDataOffHeapStoreTest;
import stroom.refdata.offheapstore.MapDefinition;
import stroom.refdata.offheapstore.RefStreamDefinition;
import stroom.refdata.offheapstore.StringValue;
import stroom.security.MockSecurityContext;
import stroom.security.Security;
import stroom.security.SecurityContext;
import stroom.streamstore.shared.StreamType;
import stroom.util.cache.CacheManager;
import stroom.util.date.DateUtil;
import stroom.util.test.StroomJUnit4ClassRunner;
import stroom.xml.event.EventList;
import stroom.xml.event.EventListBuilder;
import stroom.xml.event.EventListBuilderFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(StroomJUnit4ClassRunner.class)
public class TestReferenceData extends AbstractRefDataOffHeapStoreTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestReferenceData.class);

    private final MockFeedService feedService = new MockFeedService();

    private final SecurityContext securityContext = new MockSecurityContext();
    private final Persistence persistence = new MemoryPersistence();
    private final PipelineStore pipelineStore = new PipelineStoreImpl(
            new Store<>(persistence, securityContext), securityContext, persistence);

    @Mock
    private DocumentPermissionCache mockDocumentPermissionCache;
    @Mock
    private ReferenceDataLoader mockReferenceDataLoader;
//    @Mock
//    private PipelineStore mockPipelineStore;

    @Before
    public void setup() {
        super.setup();
        MockitoAnnotations.initMocks(this);

        Mockito.when(mockDocumentPermissionCache.hasDocumentPermission(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
    }

    private PipelineDoc buildPipelineDoc(PipelineReference pipelineReference) {
        PipelineDoc pipelineDoc = new PipelineDoc();
        pipelineDoc.setUuid(pipelineReference.getPipeline().getUuid());
        pipelineDoc.setVersion(UUID.randomUUID().toString());
        return pipelineDoc;
    }

    @Test
    public void testSimple() {
        final Feed feed1 = feedService.create("TEST_FEED_1");
        final Feed feed2 = feedService.create("TEST_FEED_2");

        final DocRef pipeline1Ref = pipelineStore.createDocument("TEST_PIPELINE_1");
        final DocRef pipeline2Ref = pipelineStore.createDocument("TEST_PIPELINE_2");
        final PipelineDoc pipeline1Doc = new PipelineDoc();

        final List<PipelineReference> pipelineReferences = Arrays.asList(
                new PipelineReference(pipeline1Ref, DocRefUtil.create(feed1), StreamType.REFERENCE.getName()),
                new PipelineReference(pipeline2Ref, DocRefUtil.create(feed2), StreamType.REFERENCE.getName()));

        // build pipelineDoc objects for each pipelineReference
        final List<PipelineDoc> pipelineDocs = pipelineReferences.stream()
                .map(this::buildPipelineDoc)
                .collect(Collectors.toList());

        // make the PipelineStore return the appropriate PipelineDoc when called
//        for (int i = 0; i < pipelineReferences.size(); i++) {
//            PipelineReference pipelineReference = pipelineReferences.get(i);
//            Mockito.when(mockPipelineStore.readDocument(pipelineReference.getPipeline()))
//                    .thenReturn(pipelineDocs.get(i));
//        }

        // Set up the effective streams to be used for each
        final TreeSet<EffectiveStream> streamSet = new TreeSet<>();
        streamSet.add(new EffectiveStream(1, DateUtil.parseNormalDateTimeString("2008-01-01T09:47:00.000Z")));
        streamSet.add(new EffectiveStream(2, DateUtil.parseNormalDateTimeString("2009-01-01T09:47:00.000Z")));
        streamSet.add(new EffectiveStream(3, DateUtil.parseNormalDateTimeString("2010-01-01T09:47:00.000Z")));

        try (CacheManager cacheManager = new CacheManager()) {
            final EffectiveStreamCache effectiveStreamCache = new EffectiveStreamCache(
                    cacheManager, null, null, null) {
                @Override
                protected TreeSet<EffectiveStream> create(final EffectiveStreamKey key) {
                    return streamSet;
                }
            };

            final ReferenceData referenceData = new ReferenceData(
                    effectiveStreamCache,
                    new FeedHolder(),
                    null,
                    null,
                    mockDocumentPermissionCache,
                    mockReferenceDataLoader,
                    refDataStore,
                    new RefDataLoaderHolder(),
                    new Security(new MockSecurityContext()),
                    pipelineStore);

            Map<RefStreamDefinition, Runnable> mockLoaderActionsMap = new HashMap<>();

            // Add multiple reference data items to prove that looping over maps works.
            addDataToMockReferenceDataLoader(
                    pipeline1Ref,
                    pipelineDocs.get(0),
                    streamSet,
                    Arrays.asList("SID_TO_PF_1", "SID_TO_PF_2"),
                    mockLoaderActionsMap);

            addDataToMockReferenceDataLoader(
                    pipeline2Ref,
                    pipelineDocs.get(1),
                    streamSet,
                    Arrays.asList("SID_TO_PF_3", "SID_TO_PF_4"),
                    mockLoaderActionsMap);

            Mockito.doAnswer(invocation -> {
                RefStreamDefinition refStreamDefinition = invocation.getArgumentAt(0, RefStreamDefinition.class);

                Runnable action = mockLoaderActionsMap.get(refStreamDefinition);
                action.run();
                return null;
            }).when(mockReferenceDataLoader).load(Mockito.any(RefStreamDefinition.class));

            checkData(referenceData, pipelineReferences, "SID_TO_PF_1");
//            checkData(referenceData, pipelineReferences, "SID_TO_PF_2");
//            checkData(referenceData, pipelineReferences, "SID_TO_PF_3");
//            checkData(referenceData, pipelineReferences, "SID_TO_PF_4");
        } catch (final RuntimeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void addDataToMockReferenceDataLoader(final DocRef pipelineRef,
                                                  final PipelineDoc pipelineDoc,
                                                  final TreeSet<EffectiveStream> effectiveStreams,
                                                  final List<String> mapNames,
                                                  final Map<RefStreamDefinition, Runnable> mockLoaderActions) {

        for (EffectiveStream effectiveStream : effectiveStreams) {

            RefStreamDefinition refStreamDefinition = new RefStreamDefinition(
//                    pipelineRef, pipelineDoc.getVersion(), effectiveStream.getStreamId());
                    pipelineRef, pipelineStore.readDocument(pipelineRef).getVersion(), effectiveStream.getStreamId());


            mockLoaderActions.put(refStreamDefinition, () -> {
                refDataStore.doWithLoaderUnlessComplete(
                        refStreamDefinition, effectiveStream.getEffectiveMs(), refDataLoader -> {

                            refDataLoader.initialise(false);
                            for (String mapName : mapNames) {
                                MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, mapName);
                                refDataLoader.put(
                                        mapDefinition,
                                        "user1",
                                        buildValue(mapDefinition, "value1"));
                                refDataLoader.put(
                                        mapDefinition,
                                        "user2",
                                        buildValue(mapDefinition, "value2"));
                            }
                            refDataLoader.completeProcessing();
                        });
            });

//            // set up mockito to load the required data when called
//            Mockito.doAnswer(invocation -> {
//                refDataStore.doWithLoaderUnlessComplete(
//                        refStreamDefinition, effectiveStream.getEffectiveMs(), refDataLoader -> {
//
//                            refDataLoader.initialise(false);
//                            for (String mapName : mapNames) {
//                                MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, mapName);
//                                refDataLoader.put(
//                                        mapDefinition,
//                                        "user1",
//                                        buildValue(mapDefinition, "value1"));
//                                refDataLoader.put(
//                                        mapDefinition,
//                                        "user2",
//                                        buildValue(mapDefinition, "value2"));
//                            }
//                            refDataLoader.completeProcessing();
//                        });
//                return null;
////                }).when(mockReferenceDataLoader).load(Mockito.same(refStreamDefinition));
//            }).when(mockReferenceDataLoader).load(Mockito.any());
        }

//        for (final String mapName : mapNames) {
//            mapStoreBuilder.setEvents(mapName, "user1", getEventsFromString("1111"), false);
//            mapStoreBuilder.setEvents(mapName, "user2", getEventsFromString("2222"), false);
//        }
//        referenceData.put(new MapStoreCacheKey(DocRefUtil.create(pipeline), 1), mapStoreBuilder.getMapStore());

//        mapStoreBuilder = new MapStoreBuilderImpl(null);
//        for (final String mapName : mapNames) {
//            mapStoreBuilder.setEvents(mapName, "user1", getEventsFromString("A1111"), false);
//            mapStoreBuilder.setEvents(mapName, "user2", getEventsFromString("A2222"), false);
//        }
//        referenceData.put(new MapStoreCacheKey(DocRefUtil.create(pipeline), 2), mapStoreBuilder.getMapStore());

//        mapStoreBuilder = new MapStoreBuilderImpl(null);
//        for (final String mapName : mapNames) {
//            mapStoreBuilder.setEvents(mapName, "user1", getEventsFromString("B1111"), false);
//            mapStoreBuilder.setEvents(mapName, "user2", getEventsFromString("B2222"), false);
//        }
//        referenceData.put(new MapStoreCacheKey(DocRefUtil.create(pipeline), 3), mapStoreBuilder.getMapStore());
    }

    private StringValue buildValue(MapDefinition mapDefinition, String value) {
        return StringValue.of(
                mapDefinition.getRefStreamDefinition().getPipelineDocRef().getUuid() + "|" +
                        mapDefinition.getRefStreamDefinition().getStreamId() + "|" +
                        mapDefinition.getMapName() + "|" +
                        value
        );
    }

    private void checkData(final ReferenceData data, final List<PipelineReference> pipelineReferences,
                           final String mapName) {
        final ReferenceDataResult result = new ReferenceDataResult();

//        data.ensureReferenceDataAvailability(
//                pipelineReferences,
//                LookupIdentifier.of(mapName, "user1", "2010-01-01T09:47:00.111Z"),
//                result);

        String expectedValuePart = "value1";

        Optional<String> optFoundValue;

        optFoundValue = lookup(data, pipelineReferences, "2010-01-01T09:47:00.111Z", mapName, "user1");
        doValueAsserts(optFoundValue, 3, mapName, expectedValuePart);

        optFoundValue = lookup(data, pipelineReferences, "2015-01-01T09:47:00.000Z", mapName, "user1");
        doValueAsserts(optFoundValue, 2, mapName, expectedValuePart);

        Assert.assertEquals("B1111", lookup(data, pipelineReferences, "", mapName, "user1"));
        Assert.assertEquals("A1111", lookup(data, pipelineReferences, "2009-10-01T09:47:00.000Z", mapName, "user1"));
        Assert.assertEquals("A1111", lookup(data, pipelineReferences, "2009-01-01T09:47:00.000Z", mapName, "user1"));
        Assert.assertEquals("1111", lookup(data, pipelineReferences, "2008-01-01T09:47:00.000Z", mapName, "user1"));

        Assert.assertEquals("B1111", lookup(data, pipelineReferences, "2010-01-01T09:47:00.111Z", mapName, "user1"));
        Assert.assertEquals("B1111", lookup(data, pipelineReferences, "2015-01-01T09:47:00.000Z", mapName, "user1"));
        Assert.assertEquals("A1111", lookup(data, pipelineReferences, "2009-10-01T09:47:00.000Z", mapName, "user1"));
        Assert.assertEquals("A1111", lookup(data, pipelineReferences, "2009-01-01T09:47:00.000Z", mapName, "user1"));
        Assert.assertEquals("1111", lookup(data, pipelineReferences, "2008-01-01T09:47:00.000Z", mapName, "user1"));

        Assert.assertNull(lookup(data, pipelineReferences, "2006-01-01T09:47:00.000Z", mapName, "user1"));
        Assert.assertNull(lookup(data, pipelineReferences, "2009-01-01T09:47:00.000Z", mapName, "user1_X"));
        Assert.assertNull(lookup(data, pipelineReferences, "2009-01-01T09:47:00.000Z", "SID_TO_PF_X", "user1"));
    }

    private void doValueAsserts(final Optional<String> optFoundValue,
                                final long expectedStreamId,
                                final String expectedMapName,
                                final String expectedValuePart) {
        assertThat(optFoundValue).isNotEmpty();
        String[] parts = optFoundValue.get().split("\\|");
        assertThat(parts).hasSize(4);
        assertThat(Long.parseLong(parts[1])).isEqualTo(expectedStreamId);
        assertThat(parts[2]).isEqualTo(expectedMapName);
        assertThat(parts[3]).isEqualTo(expectedValuePart);
    }

    @Test
    public void testNestedMaps() {
        Feed feed1 = feedService.create("TEST_FEED_V1");
        feed1.setReference(true);
        feed1 = feedService.save(feed1);

        final DocRef pipelineRef = new DocRef(PipelineDoc.DOCUMENT_TYPE, "12345");
        final List<PipelineReference> pipelineReferences = new ArrayList<>();

        pipelineReferences.add(new PipelineReference(pipelineRef,
                DocRefUtil.create(feed1), StreamType.REFERENCE.getName()));

        final TreeSet<EffectiveStream> streamSet = new TreeSet<>();
        streamSet.add(new EffectiveStream(0, 0L));
        try (CacheManager cacheManager = new CacheManager()) {
            final EffectiveStreamCache effectiveStreamCache = new EffectiveStreamCache(cacheManager, null, null, null) {
                @Override
                protected TreeSet<EffectiveStream> create(final EffectiveStreamKey key) {
                    return streamSet;
                }
            };
//            final MapStoreCache mapStoreCache = new MapStoreCache(cacheManager, new MockReferenceDataLoader(), null, null);
//            final ReferenceData referenceData = new ReferenceData(effectiveStreamCache, mapStoreCache, null, null, null, null, refDataStore);

            final MapStoreBuilder mapStoreBuilder = new MapStoreBuilderImpl(null);
//            mapStoreBuilder.setEvents("CARD_NUMBER_TO_PF_NUMBER", "011111", getEventsFromString("091111"), false);
//            mapStoreBuilder.setEvents("NUMBER_TO_SID", "091111", getEventsFromString("user1"), false);
//            referenceData.put(new MapStoreCacheKey(DocRefUtil.create(pipelineEntity), 0), mapStoreBuilder.getMapStore());

//            Assert.assertEquals("091111", lookup(referenceData, pipelineReferences, 0, "CARD_NUMBER_TO_PF_NUMBER", "011111"));
//            Assert.assertEquals("user1", lookup(referenceData, pipelineReferences, 0, "NUMBER_TO_SID", "091111"));
//
//            Assert.assertEquals("user1", lookup(referenceData, pipelineReferences, 0, "CARD_NUMBER_TO_PF_NUMBER/NUMBER_TO_SID", "011111"));
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Test
    public void testRange() {
        Feed feed1 = feedService.create("TEST_FEED_V1");
        feed1.setReference(true);
        feed1 = feedService.save(feed1);

        final DocRef pipelineRef = new DocRef(PipelineDoc.DOCUMENT_TYPE, "12345");
        final List<PipelineReference> pipelineReferences = new ArrayList<>();

        pipelineReferences.add(new PipelineReference(pipelineRef, DocRefUtil.create(feed1), StreamType.REFERENCE.getName()));

        final TreeSet<EffectiveStream> streamSet = new TreeSet<>();
        streamSet.add(new EffectiveStream(0, 0L));
        try (CacheManager cacheManager = new CacheManager()) {
            final EffectiveStreamCache effectiveStreamCache = new EffectiveStreamCache(cacheManager, null, null, null) {
                @Override
                protected TreeSet<EffectiveStream> create(final EffectiveStreamKey key) {
                    return streamSet;
                }
            };
//            final MapStoreCache mapStoreCache = new MapStoreCache(cacheManager, new MockReferenceDataLoader(), null, null);
//            final ReferenceData referenceData = new ReferenceData(effectiveStreamCache, mapStoreCache, null, null, null, null, refDataStore);

            final MapStoreBuilder mapStoreBuilder = new MapStoreBuilderImpl(null);
//            mapStoreBuilder.setEvents("IP_TO_LOC", new Range<>(2L, 30L), getEventsFromString("here"), false);
//            mapStoreBuilder.setEvents("IP_TO_LOC", new Range<>(500L, 2000L), getEventsFromString("there"), false);
//            referenceData.put(new MapStoreCacheKey(DocRefUtil.create(pipelineEntity), 0), mapStoreBuilder.getMapStore());

//            Assert.assertEquals("here", lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC", "10"));
//            Assert.assertEquals("here", lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC", "30"));
//            Assert.assertEquals("there", lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC", "500"));
//            Assert.assertEquals("there", lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC", "1000"));
//            Assert.assertEquals("there", lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC", "2000"));
//            Assert.assertEquals(null, lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC", "2001"));
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private EventList getEventsFromString(final String string) {
        final EventListBuilder builder = EventListBuilderFactory.createBuilder();
        final char[] ch = string.toCharArray();
        try {
            builder.characters(ch, 0, ch.length);
        } catch (final SAXException e) {
            LOGGER.error(e.getMessage(), e);
        }
        final EventList eventList = builder.getEventList();
        builder.reset();

        return eventList;
    }


    private Optional<String> lookup(final ReferenceData referenceData,
                                    final List<PipelineReference> pipelineReferences,
                                    final String time,
                                    final String mapName,
                                    final String key) {
        LOGGER.debug("Looking up {}, {}, {}", time, mapName, key);
        Optional<String> optValue = lookup(referenceData, pipelineReferences, DateUtil.parseNormalDateTimeString(time), mapName, key);
        LOGGER.debug("Found {}", optValue.orElse("EMPTY"));
        return optValue;
    }

    private Optional<String> lookup(final ReferenceData referenceData,
                          final List<PipelineReference> pipelineReferences,
                          final long time,
                          final String mapName,
                          final String key) {
        final ReferenceDataResult result = new ReferenceDataResult();

        referenceData.ensureReferenceDataAvailability(pipelineReferences, LookupIdentifier.of(mapName, key, time), result);

        return result.getRefDataValueProxy()
                .supplyValue()
                .flatMap(val -> Optional.of(((StringValue) val).getValue()));
    }
}
