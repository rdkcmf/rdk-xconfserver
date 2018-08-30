/*
 * If not stated otherwise in this file or this component's Licenses.txt file the
 * following copyright and licenses apply:
 *
 * Copyright 2018 RDK Management
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2010 Martin Grotzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.comcast.hesperius.dataaccess.core.util;

import com.comcast.hesperius.data.annotation.CF;
import com.comcast.hesperius.dataaccess.core.ServiceInfo;
import com.comcast.hesperius.dataaccess.core.bindery.BindingFacility;
import com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor;
import com.comcast.hesperius.dataaccess.core.cache.CacheConsistencyProvider;
import com.comcast.hesperius.dataaccess.core.cache.CacheManager;
import com.comcast.hesperius.dataaccess.core.config.ConfigurationProvider;
import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration;
import com.comcast.hesperius.dataaccess.core.dao.mapper.ColumnRange;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.common.base.GuavaOptionalSerializer;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import de.javakaffee.kryoserializers.*;
import de.javakaffee.kryoserializers.jodatime.JodaDateTimeSerializer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.iq80.snappy.CorruptionException;
import org.iq80.snappy.Snappy;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DataBindingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

/**
 * Utility kit for appDiscovery DS
 * provides us with jaxb/jackson marshalling/unmarshalling facility
 * creates/enhances DAO objects
 *
 * @author PBura
 */
public final class CoreUtil {

    private static final ColumnRange cr = new ColumnRange(null, null, null);
    private static final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final ImmutableSet<Class<?>> CF_DEFINITIONS;

    public static final CacheConsistencyProvider cacheConsistencyProvider;
    public static final DataServiceConfiguration dsconfig = ConfigurationProvider.getConfiguration();

    private static final int threadsAvailable = Runtime.getRuntime().availableProcessors();
    private static final BlockingQueue<Runnable> asyncTaskQueue = new ArrayBlockingQueue<>(30000);
    private static final ExecutorService executor = new ThreadPoolExecutor(threadsAvailable, threadsAvailable*8, 100, TimeUnit.SECONDS, asyncTaskQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static final ListeningExecutorService asyncTaskProcessor = MoreExecutors.listeningDecorator(executor);

    private static final KryoPool kryoPool = new KryoPool.Builder(new KryoFactory() {
        @Override
        public Kryo create() {
            final Kryo kryo = new Kryo();
            kryo.setAsmEnabled(true);

            final Serializer<Collection> collectionSerializer = kryo.getSerializer(Collection.class);
            collectionSerializer.setAcceptsNull(true);
            kryo.register(Collection.class, collectionSerializer);

            kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
            kryo.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
            kryo.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
            kryo.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
            kryo.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
            kryo.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
            kryo.register(Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
            kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
            kryo.register(InvocationHandler.class, new JdkProxySerializer());
            UnmodifiableCollectionsSerializer.registerSerializers(kryo);
            SynchronizedCollectionsSerializer.registerSerializers(kryo);
            kryo.register(DateTime.class, new JodaDateTimeSerializer());
            GuavaOptionalSerializer.registerSerializers(kryo);
            return kryo;
        }
    }).softReferences().build();

    private static final Logger logger = LoggerFactory.getLogger(CoreUtil.class);

    static {
        mapper.registerModule(new AfterburnerModule());
        mapper.registerModule(new JodaModule());

        //  Logging service name commit-based version, etc. to make log analysis easier
        try {
            Configuration config = new PropertiesConfiguration(ServiceInfo.CONFIG_FILE_NAME);
            logger.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new ServiceInfo(config)));
        } catch (Exception e) {
            logger.error("failed to read version information", e);
        }

        final Iterable<Class<?>> cfDefs = AnnotationScanner.getAnnotatedClasses(new Class[]{CF.class}, dsconfig.getDomainClassesBasePackage());

        CF_DEFINITIONS = new ImmutableSet.Builder<Class<?>>().addAll(cfDefs).build();

        final DataServiceConfiguration.CacheConfiguration cacheConfig = dsconfig.getCacheConfiguration();
        long reloadPeriod = cacheConfig.getTickDuration();
        long now = DateTime.now(DateTimeZone.UTC).getMillis();
        cacheConsistencyProvider = new CacheConsistencyProvider(
                cacheConfig.getChangedKeysTimeWindowSize(),
                cacheConfig.getRetryCountUntilFullRefresh(),
                ( reloadPeriod - (now % reloadPeriod)),  // forcing window to be global across all hosts having the same reloadPeriod set
                reloadPeriod);
    }

    public static ObjectMapper getObjectMapper() {
        return mapper;
    }

    /**
     * Provides Jackson aided marshalling facility
     *
     * @param entity entity to marshall
     * @param <T>    inferred type parameter
     * @return string(json) marshaled representation of object supplied
     */
    public static <T> String toJSON(T entity) {
        final ObjectWriter jsonWriter = mapper.writer();
        try {
            return jsonWriter.writeValueAsString(entity);
        } catch (IOException e) {
            throw new DataBindingException(e);
        }
    }

    /**
     * Provides Jackson aided JSON unmarshalling facility
     *
     * @param clazz      class to unmarshall against
     * @param marshalled marshalled representation
     * @param <T>        inferred from class type
     * @return instance if given class
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromJSON(final Class<T> clazz, final String marshalled) {
        return fromJSON(mapper.getTypeFactory().constructType(clazz), marshalled);
    }

    public static <T> T fromJSON(final TypeReference<T> tref, final String marshalled) {
        return fromJSON(mapper.getTypeFactory().constructType(tref), marshalled);
    }

    private static <T> T fromJSON(final JavaType javaType, final String marshalled) {
        if (null == marshalled || marshalled.isEmpty()) {
            return null;
        } else {
            try {
                return mapper.reader(javaType).readValue(new MappingJsonFactory(mapper).createParser(marshalled));
            } catch (IOException e) {
                throw new DataBindingException(e);
            }
        }
    }

    public static <T> T fromJSON(final Class<T> clazz, final InputStream marshalled) {
        return fromJSON(mapper.getTypeFactory().constructType(clazz), marshalled);
    }

    public static <T> T fromJSON(final TypeReference<T> tref, final InputStream marshalled) {
        return fromJSON(mapper.getTypeFactory().constructType(tref), marshalled);
    }

    private static <T> T fromJSON(final JavaType javaType, final InputStream marshalled) {
        if (null == marshalled) {
            return null;
        } else {
            try {
                return mapper.reader(javaType).readValue(new MappingJsonFactory().createParser(marshalled));
            } catch (IOException e) {
                throw new DataBindingException(e);
            }
        }
    }

    public static int getThreadsAvailable() {
        return threadsAvailable;
    }

    public static ExecutorService getAsyncTaskProcessor() {
        return asyncTaskProcessor;
    }

    public static void doAsync(final Runnable task) {
        asyncTaskProcessor.submit(task);
    }

    public static <T> ListenableFuture<T> computeAsync(final Callable<T> task) {
        return asyncTaskProcessor.submit(task);
    }

    /**
     * processes large amounts of data in mapreduce like fashion
     *
     * @param source     source iterable that must be processed
     * @param sourceSize size (since iterable does not allow to obtain size)
     * @param predicate  predicate to do filtering on
     * @return filtered on predicate param subset source
     */
    public static <T> Iterable<T> doParallelFilter(final Iterable<T> source, final int sourceSize, final Predicate<? super T> predicate) {
        if (sourceSize == 0) return Lists.newArrayList();
        else if (sourceSize > dsconfig.getCacheConfiguration().getNumberOfEntriesToProcessSequentially()) {
            final List<ListenableFuture<List<T>>> partitionFutures = new ArrayList<>(sourceSize / threadsAvailable);

            for (final List<T> partition : Iterables.partition(source, sourceSize / threadsAvailable)) {
                partitionFutures.add(asyncTaskProcessor.submit(new Callable<List<T>>() {
                    @Override
                    public List<T> call() throws Exception {
                        return Lists.newArrayList(Iterables.filter(partition, predicate));
                    }
                }));
            }
            try {
                return Iterables.concat(Futures.successfulAsList(partitionFutures).get());
            } catch (InterruptedException e) {
                logger.error("Error happened during async processing ", e);
            } catch (ExecutionException e) {
                logger.error("Error happened during async processing ", e);
            }
            return Lists.newArrayList();
        } else return Iterables.filter(source, predicate);
    }

    public static <T> T clone(final T t) {
        return kryoPool.run(new KryoCallback<T>() {
            @Override
            public T execute(Kryo kryo) {
                return kryo.copy(t);
            }
        });
    }

    /**
     * Creates an utility for byte oriented compressing/decompressing and joining/splitting.
     *
     * @return new Archiver instance
     */
    public static Archiver createArchiver() {
        return new SnappyArchiver();
    }

    //----- archivers section - see Archiver interface for methods description ---------//

    /**
     * Base class for archivers. Implements {@link Archiver#split(java.nio.ByteBuffer, int)} and
     * {@link Archiver#join(java.nio.ByteBuffer[])} methods.
     */
    private static abstract class BaseArchiver implements Archiver {
        @Override
        public final ByteBuffer[] split(final ByteBuffer data, final int chunkSize) {
            final int chunksCount = data.remaining() / chunkSize + 1;
            final ByteBuffer[] result = new ByteBuffer[chunksCount];

            int offset = data.position();
            for (int i = 0; i < chunksCount - 1; i++) {
                result[i] = ByteBuffer.wrap(data.array(), offset, chunkSize);
                offset += chunkSize;
            }
            result[chunksCount - 1] = ByteBuffer.wrap(data.array(), offset, data.remaining() - offset);

            return result;
        }

        @Override
        public final ByteBuffer join(final ByteBuffer[] data) {
            final ByteArrayOutputStream builder = new ByteArrayOutputStream();
            for (ByteBuffer byteBuffer : data) {
                builder.write(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
            }
            return ByteBuffer.wrap(builder.toByteArray());
        }
    }

    /**
     * Wrapper over a fast compressor/decompressor snappy library.
     */
    private static class SnappyArchiver extends BaseArchiver {
        @Override
        public ByteBuffer compress(ByteBuffer data) {
            byte[] array = data.array();
            int offset = data.position();
            int length = data.remaining();

            int maxCompressedLength = Snappy.maxCompressedLength(length);
            byte[] rawResult = new byte[maxCompressedLength];
            int compressedBytesCount = Snappy.compress(array, offset, length, rawResult, 0);
            return ByteBuffer.wrap(rawResult, 0, compressedBytesCount);
        }

        @Override
        public ByteBuffer decompress(ByteBuffer data) throws DataFormatException {
            byte[] array = data.array();
            int offset = data.position();
            int length = data.remaining();
            try {
                byte[] rawResult = Snappy.uncompress(array, offset, length);
                return ByteBuffer.wrap(rawResult);
            } catch (CorruptionException ex) {
                throw new DataFormatException(ex);
            }
        }
    }

    /**
     * trigger loading class
     */
    public static void init() {
    }
}
