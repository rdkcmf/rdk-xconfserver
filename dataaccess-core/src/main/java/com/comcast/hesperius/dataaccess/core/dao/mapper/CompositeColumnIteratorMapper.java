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
package com.comcast.hesperius.dataaccess.core.dao.mapper;

import com.comcast.hesperius.data.annotation.CF;
import com.comcast.hesperius.dataaccess.core.cache.CacheManager;
import com.comcast.hesperius.dataaccess.core.dao.DaoFactory;
import com.comcast.hesperius.dataaccess.core.dao.util.DataUtils;
import com.comcast.hesperius.dataaccess.core.dao.provider.ICompositePropertyProvider;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.Composite;
import com.netflix.astyanax.serializers.ObjectSerializer;
import com.netflix.astyanax.serializers.SerializerTypeInferer;
import com.netflix.astyanax.serializers.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implementation of {@link ColumnIteratorMapper} for {@link com.comcast.hesperius.dataaccess.core.dao.CompositeDAO}.
 *
 * @param <T> persistable object type
 */
public class CompositeColumnIteratorMapper<T extends IPersistable> implements ColumnIteratorMapper<T> {
    private static final Logger log = LoggerFactory.getLogger(CompositeColumnIteratorMapper.class);
    private final ICompositePropertyProvider<T> provider;
    private final IPersistable.Factory<T> factory;
    private final String persistableObjectName;
    private static final Serializer<String> stringSerializer = StringSerializer.get();

    /**
     * @param provider object that helps to deal with Composite columns and appropriated object fields
     * @param factory  persistable object factory
     */
    public CompositeColumnIteratorMapper(final ICompositePropertyProvider<T> provider, final IPersistable.Factory<T> factory) {
        this.provider = provider;
        this.factory = factory;
        persistableObjectName = factory.getClassObject().getSimpleName();
    }

    /**
     * Maps iterator over {@link Column}'s to iterator over persistable objects of generic type {@code T}.
     * Pass iterator over all required columns or some mapped objects will be partially initialized.
     *
     * @param columnIterator {@link Iterator#remove()} is not supported
     */
    public Iterator<T> mapColumnIterator(final Iterator<Column> columnIterator) {
        return new IteratorImpl(columnIterator);
    }

    private class IteratorImpl implements Iterator<T> {
        private final Iterator<Column> columnIterator;
        private Column<Composite> nextObjectFirstColumn;

        private IteratorImpl(Iterator<Column> columnIterator) {
            this.columnIterator = columnIterator;
            nextObjectFirstColumn = columnIterator.hasNext() ? columnIterator.next() : null;
        }

        @Override
        public boolean hasNext() {
            return nextObjectFirstColumn != null;
        }

        @Override
        public T next() {
            if (nextObjectFirstColumn == null) {
                throw new NoSuchElementException();
            }

            final T result = factory.newObject();
            initObjectProperty(result, nextObjectFirstColumn);
            final Object id = provider.getIdComponent(nextObjectFirstColumn.getName());

            // note that we can return from the below loop if we reach a first column of next object
            while (columnIterator.hasNext()) {
                Column<Composite> column = columnIterator.next();
                if (!id.equals(provider.getIdComponent(column.getName()))) {
                    // columns id are not equal, thus we reached a first column of next object
                    nextObjectFirstColumn = column;
                    log.trace("Mapped " + persistableObjectName + " with id " + id);
                    return result;
                }
                initObjectProperty(result, column);
            }
            // if we are here it means that columnIterator has no more elements
            nextObjectFirstColumn = null;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void initObjectProperty(T obj, Column<Composite> column) {
            String fieldName = provider.getPropertyComponent(column.getName());
            if (fieldName != null) {
                try {
                    final Method m = DataUtils.getSetter(obj, fieldName, obj.getClass());
                    if (m == null) {
                        log.warn("Unable to get setter method for {} field of changed data", fieldName, obj);
                        if (fieldName.equals("DAOid")) {
                            final Integer daoId =  (Integer) column.getValue(SerializerTypeInferer.getSerializer(Integer.class));
                            CF clazz = (CF)((Class) CacheManager.findTypeParamsByDAOId(daoId).toArray()[1]).getAnnotation(CF.class);
                            log.warn("Changed data belongs to {} CF", clazz.cfName());
                        }
                    } else {
                        m.setAccessible(true);
                        final Class valueType = m.getParameterTypes()[0];
                        final Serializer valueSerializer = SerializerTypeInferer.getSerializer(valueType);
                        if (valueSerializer instanceof ObjectSerializer) {
                            m.invoke(obj, CoreUtil.fromJSON(valueType, column.getValue(stringSerializer)));
                        } else {
                            m.invoke(obj, column.getValue(valueSerializer));
                        }
                        if (column.getTtl() != 0) {
                            obj.setTTL(fieldName, column.getTtl());
                        }
                    }
                } catch (NoSuchMethodException e) {
                    log.error("", e);
                } catch (IllegalAccessException e) {
                    log.error("", e);
                } catch (InvocationTargetException e) {
                    log.error("", e);
                }
            }
        }
    }
}
