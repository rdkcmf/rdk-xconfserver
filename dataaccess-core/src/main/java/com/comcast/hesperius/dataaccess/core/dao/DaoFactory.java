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
 *
 * Author: phoenix
 * Created: 30/04/2014  07:25
 */
package com.comcast.hesperius.dataaccess.core.dao;

import com.comcast.hesperius.data.annotation.CompositeCF;
import com.comcast.hesperius.data.annotation.ListingCF;
import com.comcast.hesperius.dataaccess.core.cache.CacheManager;
import com.comcast.hesperius.dataaccess.core.dao.util.CFPersistenceDefinition;
import com.comcast.hesperius.dataaccess.core.dao.util.CompressingJsonSerializer;
import com.comcast.hesperius.dataaccess.core.dao.util.JsonSerializer;
import com.comcast.hesperius.dataaccess.core.util.bean.BeanProperty;
import com.comcast.hesperius.dataaccess.core.util.bean.BeanUtils;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.comcast.hydra.astyanax.util.PersistableFactory;
import com.netflix.astyanax.Serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DaoFactory {
    public static final class Simple {
        private static final Map<Integer, ISimpleCachedDAO<?, ? extends IPersistable>> cachedDaoCache = new ConcurrentHashMap<>();

        /**
         * Creates DAO typed with classes provided
         *
         * @param keyClass   key class for DAO
         * @param valueClass value class for DAO
         * @param <K>        inferred key type
         * @param <T>        inferred value type
         * @return return desired DAO instance
         */
        @SuppressWarnings("unchecked")
        public static <K, T extends IPersistable> IADSSimpleDAO<K, T> createDAO(Class<K> keyClass, Class<T> valueClass) {
            try {
                return RowMappingPartialDAO.createImpl(valueClass);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public static <K, T extends IPersistable> IADSSimpleDAO<K, T> createDAO(Class<T> valueClass) {
            try {
                return addToCachedDaoCacheAndReturn(CacheManager.augmentWithCache(RowMappingPartialDAO.<K, T>createImpl(valueClass)));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public static <K, T extends IPersistable> IADSSimpleDAO.Builder createDAOBuilder() {
            return new RowMappingPartialDAO.Builder<K, T>();
        }

        public static <K, T extends IPersistable> IADSSimpleDAO<K, T> createDAO(final CFPersistenceDefinition cfPersDef, Class<T> valueClass) {
            try {
                return RowMappingPartialDAO.createImpl(cfPersDef, valueClass);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public static <K, T extends IPersistable> ISimpleCachedDAO<K, T> createCachedDAO(final CFPersistenceDefinition cfPersDef, Class<T> valueClass) {
            try {
                final IADSSimpleDAO<K, T> dao = createDAO(cfPersDef, valueClass);
                return addToCachedDaoCacheAndReturn(CacheManager.augmentWithCache(dao));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public static <K, T extends IPersistable> ISimpleCachedDAO<K, T> createCachedDAO(Class<K> keyClass, Class<T> valueClass) {
            return addToCachedDaoCacheAndReturn(CacheManager.augmentWithCache(createDAO(keyClass, valueClass)));
        }

        public static <K, T extends IPersistable> ISimpleCachedDAO<K, T> createCachedDAO(IADSSimpleDAO<K, T> source) {
            return addToCachedDaoCacheAndReturn(CacheManager.augmentWithCache(source));
        }

        private static <K, T extends IPersistable> ISimpleCachedDAO<K, T> addToCachedDaoCacheAndReturn(final ISimpleCachedDAO<K, T> source) {
            final int id = source.id();
            if (cachedDaoCache.get(id) == null){
                cachedDaoCache.put(id, source);
            }
            return source;
        }

        public static <K, T extends IPersistable> ISimpleCachedDAO<K, T> findDaoInCachedDaoCacheById(final int id) {
            return (ISimpleCachedDAO<K, T>) cachedDaoCache.get(id);
        }
    }

    public static final class Composite {
        @SuppressWarnings("unchecked")
        public static <K, T extends IPersistable> ICompositeDAO<K, T> createDAO(Class<T> valueClass) {
            CompositeCF compositeCF = valueClass.getAnnotation(CompositeCF.class);
            if (compositeCF == null) {
                throw new IllegalArgumentException("valueClass must be annotated with @CompositeCF");
            }
            return new CompositeDAO<K, T>(compositeCF.cfName(), (Class<K>) compositeCF.keyType(), valueClass);
        }
    }

    public static final class Listing {
        @SuppressWarnings("unchecked")
        public static <K, N, T extends IPersistable> IListingDAO<K, N, T> createDAO(Class<T> valueClass) {
            ListingCF listingCF = valueClass.getAnnotation(ListingCF.class);
            if (listingCF == null) {
                throw new IllegalArgumentException("valueClass must be annotated with @ListingCF");
            }
            String cfName = listingCF.cfName();
            Serializer<K> keySerializer = BeanUtils.getSerializer((Class<K>) listingCF.keyType());
            Serializer<T> valueSerializer = listingCF.compress() ? new CompressingJsonSerializer<T>(valueClass) : new JsonSerializer<T>(valueClass);
            BeanProperty<T, N> beanProperty = BeanUtils.getOrCreateBeanProperty(valueClass, listingCF.columnNameField());
            IPersistable.Factory<T> factory = new PersistableFactory<T>(valueClass);
            int ttl = listingCF.ttl();
            return new ListingDAO<K, N, T>(cfName, keySerializer, valueSerializer, beanProperty, factory, ttl);
        }
    }

    public static final class RotatingListing {
        @SuppressWarnings("unchecked")
        public static <K, T extends IPersistable> IListingDAO<K, String, T> createDAO(Class<T> valueClass) {
            ListingCF listingCF = valueClass.getAnnotation(ListingCF.class);
            if (listingCF == null) {
                throw new IllegalArgumentException("valueClass must be annotated with @ListingCF");
            }
            String cfName = listingCF.cfName();
            Serializer<K> keySerializer = BeanUtils.getSerializer((Class<K>) listingCF.keyType());
            Serializer<T> valueSerializer = listingCF.compress() ? new CompressingJsonSerializer<T>(valueClass) : new JsonSerializer<T>(valueClass);
            BeanProperty<T, String> beanProperty = BeanUtils.getOrCreateBeanProperty(valueClass, listingCF.columnNameField());
            IPersistable.Factory<T> factory = new PersistableFactory<T>(valueClass);
            int ttl = listingCF.ttl();
            byte bounds = listingCF.bounds();
            return new RotatingListingDAO<K, T>(cfName, keySerializer, valueSerializer, beanProperty, factory, ttl, bounds);
        }
    }
}
