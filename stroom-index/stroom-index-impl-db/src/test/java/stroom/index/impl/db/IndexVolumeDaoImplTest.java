package stroom.index.impl.db;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import stroom.index.dao.IndexVolumeDao;
import stroom.index.dao.IndexVolumeGroupDao;
import stroom.index.shared.IndexVolume;
import stroom.index.shared.IndexVolumeGroup;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class IndexVolumeDaoImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexVolumeDaoImplTest.class);

    private static MySQLContainer dbContainer = null;
    // = new MySQLContainer().withDatabaseName(Stroom.STROOM.getName());
    // = null;//

    private static IndexVolumeDao indexVolumeDao;
    private static IndexVolumeGroupDao indexVolumeGroupDao;

    @BeforeAll
    static void beforeAll() {
        LOGGER.info(() -> "Before All - Start Database");
        Optional.ofNullable(dbContainer).ifPresent(MySQLContainer::start);

        final Injector injector = Guice.createInjector(new IndexDbModule(), new TestModule(dbContainer));

        indexVolumeDao = injector.getInstance(IndexVolumeDao.class);
        indexVolumeGroupDao = injector.getInstance(IndexVolumeGroupDao.class);
    }

    @AfterAll
    static void afterAll() {
        LOGGER.info(() -> "After All - Stop Database");
        Optional.ofNullable(dbContainer).ifPresent(MySQLContainer::stop);
    }

    @Test
    void testCreate() {
        // Given
        final String nodeName = TestData.createNodeName();
        final String path = TestData.createPath();

        // When
        final IndexVolume created = indexVolumeDao.create(nodeName, path);
        assertThat(created).isNotNull();
        final IndexVolume retrieved = indexVolumeDao.getById(created.getId());
        assertThat(Stream.of(created, retrieved)).allSatisfy(i -> {
            assertThat(i.getNodeName()).isEqualTo(nodeName);
            assertThat(i.getPath()).isEqualTo(path);
        });
        indexVolumeDao.delete(retrieved.getId());
        final IndexVolume retrievedAfterDelete = indexVolumeDao.getById(created.getId());

        assertThat(retrievedAfterDelete).isNull();
    }

    @Test
    void testSimpleGroupMembership() {
        // Given
        final String groupName = TestData.createVolumeGroupName();
        final String nodeName = TestData.createNodeName();
        final Set<String> pathsInsideGroup = IntStream.range(0, 10)
                .mapToObj(TestData::createPath)
                .collect(Collectors.toSet());
        final Set<String> pathsOutsideGroup = IntStream.range(0, 10)
                .mapToObj(TestData::createPath)
                .collect(Collectors.toSet()); // used to ensure some 'noise' exists in the db.

        // When
        final IndexVolumeGroup group = indexVolumeGroupDao.create(groupName);
        final Set<IndexVolume> indexVolumesInsideGroup = pathsInsideGroup.stream()
                .map(p -> indexVolumeDao.create(nodeName, p))
                .collect(Collectors.toSet());
        pathsOutsideGroup.forEach(p -> indexVolumeDao.create(nodeName, p)); // noise
        indexVolumesInsideGroup.forEach(i -> indexVolumeDao.addVolumeToGroup(i.getId(), groupName));
        final List<IndexVolume> volumesInGroup = indexVolumeDao.getVolumesInGroup(groupName);

        // Then
        assertThat(group.getName()).isEqualTo(groupName);
        assertThat(volumesInGroup.stream().map(IndexVolume::getId))
                .containsOnlyElementsOf(indexVolumesInsideGroup.stream()
                        .map(IndexVolume::getId)
                        .collect(Collectors.toSet()));
        assertThat(volumesInGroup.stream().map(IndexVolume::getPath))
                .containsOnlyElementsOf(pathsInsideGroup);

    }

    @Test
    void testGroupMembershipRemove() {
        // Given
        final String groupName = TestData.createVolumeGroupName();
        final String nodeName = TestData.createNodeName();
        final String path = TestData.createPath();

        // When
        final IndexVolumeGroup group = indexVolumeGroupDao.create(groupName);
        final IndexVolume indexVolume = indexVolumeDao.create(nodeName, path);
        indexVolumeDao.addVolumeToGroup(indexVolume.getId(), groupName);
        final List<IndexVolume> found1 = indexVolumeDao.getVolumesInGroup(groupName);
        indexVolumeDao.removeVolumeFromGroup(indexVolume.getId(), groupName);
        final List<IndexVolume> found2 = indexVolumeDao.getVolumesInGroup(groupName);

        // Then
        assertThat(group.getName()).isEqualTo(groupName);
        assertThat(found1).hasSize(1);
        assertThat(found1.get(0).getNodeName()).isEqualTo(nodeName);
        assertThat(found1.get(0).getPath()).isEqualTo(path);
        assertThat(found2).hasSize(0);
    }

    @Test
    void testGroupMembershipClear() {
        // Given
        final Set<String> groupNames = IntStream.range(0, 5)
                .mapToObj(TestData::createVolumeGroupName)
                .collect(Collectors.toSet());
        final String nodeName = TestData.createNodeName();
        final String pathToRemove = TestData.createPath();
        final String pathToRemain = TestData.createPath();

        // When
        final IndexVolume volumeToRemove = indexVolumeDao.create(nodeName, pathToRemove);
        final IndexVolume volumeToRemain = indexVolumeDao.create(nodeName, pathToRemain);
        final Set<IndexVolumeGroup> groups = groupNames.stream()
                .map(indexVolumeGroupDao::create)
                .peek(g -> indexVolumeDao.addVolumeToGroup(volumeToRemove.getId(), g.getName()))
                .peek(g -> indexVolumeDao.addVolumeToGroup(volumeToRemain.getId(), g.getName()))
                .collect(Collectors.toSet());
        final List<IndexVolume> indexesInGroup1 = indexVolumeDao.getVolumesInGroup(groupNames.iterator().next());
        indexVolumeDao.clearVolumeGroupMemberships(volumeToRemove.getId());
        final List<IndexVolume> indexesInGroup2 = indexVolumeDao.getVolumesInGroup(groupNames.iterator().next());

        // Then
        assertThat(indexesInGroup1.stream().map(IndexVolume::getPath)).containsOnly(pathToRemain, pathToRemove);
        assertThat(indexesInGroup2.stream().map(IndexVolume::getPath)).containsOnly(pathToRemain);

    }
}
