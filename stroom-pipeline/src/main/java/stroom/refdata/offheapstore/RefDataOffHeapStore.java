/*
 * Copyright 2018 Crown Copyright
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

package stroom.refdata.offheapstore;

import com.google.common.base.Preconditions;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.entity.shared.Range;
import stroom.refdata.lmdb.BasicLmdbDb;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RefDataOffHeapStore implements RefDataStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefDataOffHeapStore.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(RefDataOffHeapStore.class);

    private static final String KEY_VALUE_STORE_DB_NAME = "KeyValueStore";
    private static final String RANGE_STORE_DB_NAME = "RangeStore";
    private static final String VALUE_STORE_DB_NAME = "ValueStore";
    private static final String MAP_UID_STORE_FORWARD_DB_NAME = "MapUidStoreForward";
    private static final String MAP_UID_STORE_BACKWARD_DB_NAME = "MapUidStoreBackward";
    private static final String PROCESSING_INFO_DB_NAME = "ProcessingInfoMapsStore";

    private final Path dbDir;
    private final long maxSize;

    private final Env<ByteBuffer> env;

    // the DBs that make up the store
    private final BasicLmdbDb<KeyValueStoreKey, ValueStoreKey> keyValueStoreDb;
    private final BasicLmdbDb<RangeStoreKey, ValueStoreKey> rangeStoreDb;

    private final BasicLmdbDb<ValueStoreKey, RefDataValue> valueStoreDb;

    private final BasicLmdbDb<MapDefinition, UID> mapUidStoreForwardDb;
    private final BasicLmdbDb<UID, MapDefinition> mapUidStoreBackwardDb;

    private final BasicLmdbDb<RefStreamDefinition, RefDataProcessingInfo> processedMapsStoreDb;

    /**
     * @param dbDir   The directory the LMDB environment will be created in, it must already exist
     * @param maxSize The max size in bytes of the environment. This should be less than the available
     *                disk space for dbDir. This size covers all DBs created in this environment.
     */
    RefDataOffHeapStore(final Path dbDir, final long maxSize) {
        this.dbDir = dbDir;
        this.maxSize = maxSize;

        LOGGER.debug("Creating LMDB environment with maxSize: {}, dbDir {}", maxSize, dbDir.toAbsolutePath().toString());
        env = Env.<ByteBuffer>create()
                .setMapSize(maxSize)
                .setMaxDbs(1)
                .open(dbDir.toFile());

        // create all the databases
        this.keyValueStoreDb = new BasicLmdbDb<>(
                env, new KeyValueStoreKeySerde(), new ValueStoreKeySerde(), KEY_VALUE_STORE_DB_NAME);
        this.rangeStoreDb = new BasicLmdbDb<>(
                env, new RangeStoreKeySerde(), new ValueStoreKeySerde(), RANGE_STORE_DB_NAME);
        this.valueStoreDb = new BasicLmdbDb<>(
                env, new ValueStoreKeySerde(), new RefDataValueSerde(), VALUE_STORE_DB_NAME);
        this.mapUidStoreForwardDb = new BasicLmdbDb<>(
                env, new MapDefinitionSerde(), new UIDSerde(), MAP_UID_STORE_FORWARD_DB_NAME);
        this.mapUidStoreBackwardDb = new BasicLmdbDb<>(
                env, new UIDSerde(), new MapDefinitionSerde(), MAP_UID_STORE_BACKWARD_DB_NAME);
        this.processedMapsStoreDb = new BasicLmdbDb<>(
                env, new RefStreamDefinitionSerde(), new ProcessingInfoSerde(), PROCESSING_INFO_DB_NAME);
    }

    /**
     * Returns the {@link RefDataProcessingInfo} for the passed {@link MapDefinition}, or an empty
     * {@link Optional} if there isn't one.
     */
    @Override
    public Optional<RefDataProcessingInfo> getProcessingInfo(final RefStreamDefinition refStreamDefinition) {
        return Optional.empty();
    }

    @Override
    public boolean isDataLoaded(final RefStreamDefinition refStreamDefinition) {

        // TODO we could optimise this so that it doesn't deser the whole object, instead just
        // extract the state value while in a txn
        return getProcessingInfo(refStreamDefinition)
                .map(RefDataProcessingInfo::getProcessingState)
                .filter(Predicate.isEqual(RefDataProcessingInfo.ProcessingState.COMPLETE))
                .isPresent();
    }

    @Override
    public void put(final MapDefinition mapDefinition,
                    final String key,
                    final Supplier<RefDataValue> refDataValueSupplier,
                    final boolean overwriteExistingValue) {

        boolean keyExists = false;

        if (!overwriteExistingValue && keyExists) {
            throw new RuntimeException("key exists");
        }

    }

    @Override
    public void put(final MapDefinition mapDefinition,
                    final Range<Long> keyRange,
                    final Supplier<RefDataValue> refDataValueSupplier,
                    final boolean overwriteExistingValue) {

        boolean keyExists = false;

        if (!overwriteExistingValue && keyExists) {
            throw new RuntimeException("key exists");
        }

    }

    @Override
    public Optional<RefDataValue> getValue(final MapDefinition mapDefinition,
                                           final String key) {
        return Optional.empty();
    }

//    @Override
//    public Optional<RefDataValue> getValue(final ValueStoreKey valueStoreKey) {
//        return Optional.empty();
//    }

    @Override
    public Optional<RefDataValueProxy> getValueProxy(final MapDefinition mapDefinition, final String key) {
        return Optional.empty();
    }

    @Override
    public void consumeValue(final MapDefinition mapDefinition,
                             final String key,
                             final Consumer<RefDataValue> valueConsumer) {


    }

    @Override
    public void consumeValue(final ValueStoreKey valueStoreKey, final Consumer<RefDataValue> valueConsumer) {

    }

    @Override
    public void consumeBytes(final ValueStoreKey valueStoreKey, final Consumer<ByteBuffer> valueConsumer) {

    }

    @Override
    public <T> Optional<T> map(final MapDefinition mapDefinition,
                               final String key,
                               final Function<RefDataValue, T> valueMapper) {

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> map(final ValueStoreKey valueStoreKey, final Function<RefDataValue, T> valueMapper) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> mapBytes(final ValueStoreKey valueStoreKey, final Function<ByteBuffer, T> valueMapper) {
        return Optional.empty();
    }


    public RefDataLoader loader(final RefStreamDefinition refStreamDefinition, final long effectiveTimeMs) {
        return new RefDataLoaderImpl(this, env, refStreamDefinition, effectiveTimeMs);
    }

    private static Dbi<ByteBuffer> openDbi(final Env<ByteBuffer> env, final String name) {
        LOGGER.debug("Opening LMDB database with name: {}", name);
        return env.openDbi(name, DbiFlags.MDB_CREATE);
    }

    private UID createMapUID(final MapDefinition mapDefinition) {

        //Build a new UID based on +1 from the highest current UID
        //create forward mapping
        //create reverse mapping

        return null;
    }

    private void setProcessingInfo(final RefStreamDefinition refStreamDefinition, final RefDataProcessingInfo refDataProcessingInfo) {

    }


    /**
     * Class for adding multiple items to the {@link RefDataOffHeapStore} within a single
     * write transaction.  Must be used inside a try-with-resources block to ensure the transaction
     * is closed, e.g.
     * try (RefDataLoader refDataLoader = refDataOffHeapStore.getLoader(...)) { ... }
     * The transaction will be committed when the loader is closed
     */
    public static class RefDataLoaderImpl implements RefDataLoader {

        private Txn<ByteBuffer> txn = null;
        private final RefDataOffHeapStore refDataOffHeapStore;
        private final Env<ByteBuffer> lmdbEnvironment;
        private boolean initialised = false;
        private final RefStreamDefinition refStreamDefinition;
        private final long effectiveTimeMs;
        private int maxPutsBeforeCommit = Integer.MAX_VALUE;
        private int putsCounter = 0;

        // TODO we could just hit lmdb each time, but there may be serde costs
        private final Map<MapDefinition, UID> mapDefinitionToUIDMap = new HashMap<>();

        RefDataLoaderImpl(final RefDataOffHeapStore refDataOffHeapStore,
                          final Env<ByteBuffer> lmdbEnvironment,
                          final RefStreamDefinition refStreamDefinition,
                          final long effectiveTimeMs) {
            this.refDataOffHeapStore = refDataOffHeapStore;
            this.lmdbEnvironment = lmdbEnvironment;
            this.refStreamDefinition = refStreamDefinition;
            this.effectiveTimeMs = effectiveTimeMs;
        }

        public void initialise() {
            throwExceptionIfAlreadyInitialised();

            // TODO create processed streams entry if it doesn't exist with a state of IN_PROGRESS
            // TODO if it does exist update the update time

            beginTxn();
            this.initialised = true;
        }

        public void completeProcessing() {
            throwExceptionIfNotInitialised();

            // Set the processing info record to COMPLETE and update the last update time
        }

        @Override
        public void setCommitInterval(final int maxPutsBeforeCommit) {
            Preconditions.checkArgument(maxPutsBeforeCommit >= 1);
            this.maxPutsBeforeCommit = maxPutsBeforeCommit;
        }


        private void beginTxn() {
            if (txn != null) {
                throw new RuntimeException("Transaction is already open");
            }
            this.txn = lmdbEnvironment.txnWrite();
        }


        private void commit() {
            if (txn != null) {
                try {
                    txn.commit();
                    txn = null;
                } catch (Exception e) {
                    throw new RuntimeException("Error committing write transaction", e);
                }
            }
        }

        @Override
        public void put(final String key,
                        final RefDataValue refDataValue,
                        final boolean overwriteExistingValue) {
            throwExceptionIfNotInitialised();
            beginTxnIfRequired();


            //create forward+reverse map ID entries if they don't exist
            //hold them in the mapDefinitionToUIDMap if we do create them

            //see if key exists, then put value based on overwriteExistingValue

            //throw exception if entry exists and overwriteExistingValue is false

        }

        @Override
        public void put(final Range<Long> keyRange,
                        final RefDataValue refDataValue,
                        final boolean overwriteExistingValue) {
            throwExceptionIfNotInitialised();
            beginTxnIfRequired();

            //create forward+reverse map ID entries if they don't exist
            //hold them in the mapDefinitionToUIDMap if we do create them

            //see if key exists, then put value based on overwriteExistingValue

            //throw exception if entry exists and overwriteExistingValue is false

        }

        private UID getOrCrteateMapUID(final MapDefinition mapDefinition) {
            return mapDefinitionToUIDMap.computeIfAbsent(mapDefinition, refDataOffHeapStore::createMapUID);
        }

        private void commitIfRequired() {
            putsCounter++;
            if (putsCounter >= maxPutsBeforeCommit) {
                //
                commit();
            }
        }


        private void beginTxnIfRequired() {
            if (txn == null) {
                beginTxn();
            }
        }

        private void throwExceptionIfNotInitialised() {
            if (!initialised) {
                throw new RuntimeException("Loader not initialised");
            }
        }

        private void throwExceptionIfAlreadyInitialised() {
            if (initialised) {
                throw new RuntimeException("Loader is already initialised");
            }
        }

        @Override
        public void close() throws Exception {
            if (txn != null) {
                txn.commit();
                txn.close();
            }
        }
    }

}
