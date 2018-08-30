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
package com.comcast.hesperius.dataaccess.core.dao;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.mapper.CompositeMapperHelper;
import com.comcast.hesperius.dataaccess.core.dao.mapper.ICompositeMapper;
import com.comcast.hesperius.dataaccess.core.dao.mapper.IRowMapper;
import com.comcast.hesperius.dataaccess.core.dao.mapper.ReflectionCompositeMapper;
import com.comcast.hesperius.dataaccess.core.dao.provider.DefaultCompositePropertyProvider;
import com.comcast.hesperius.dataaccess.core.dao.provider.ICompositePropertyProvider;
import com.comcast.hesperius.dataaccess.core.dao.util.DataUtils;
import com.comcast.hesperius.dataaccess.core.dao.util.ExecuteWithUncheckedException;
import com.comcast.hesperius.dataaccess.core.rest.query.Filter;
import com.comcast.hesperius.dataaccess.core.util.EntityValidationUtils;
import com.comcast.hesperius.dataaccess.core.util.bean.BeanUtils;
import com.comcast.hydra.astyanax.data.IKeyGenerator;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.comcast.hydra.astyanax.util.PersistableFactory;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Composite;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.query.RowSliceQuery;
import com.netflix.astyanax.recipes.reader.AllRowsReader;
import com.netflix.astyanax.serializers.CompositeSerializer;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.SyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * A data access object for storing data using a owner ID (row key) and composite columns.  Objects are stored
 * in the order of the chosen ID field.  The column names are composites containing the object ID and the property
 * name.  Example:
 * {
 *     "owner_ID_1": [
 *         "object_id_1/id":          "object_id_1",
 *         "object_id_1/name":        "object 1 name",
 *         "object_id_1/description": "object 1 description",
 *         "object_id_2/id":          "object_id_2",
 *         "object_id_2/name":        "object 2 name",
 *         "object_id_2/description": "object 2 description",
 *     ],
 *     "owner_ID_2": [
 *         "object_id_3/id":          "object_id_3",
 *         "object_id_3/name":        "object 3 name",
 *         "object_id_3/description": "object 3 description",
 *         "object_id_4/id":          "object_id_4",
 *         "object_id_4/name":        "object 4 name",
 *         "object_id_4/description": "object 4 description",
 *     ]
 * }
 * </p><p>
 * Subclasses have a fair amount of control over the data types and fields to use for the row keys and components
 * of the composite columns.  The template parameter K is used to allow a subclass to provide the type for the
 * row key.  Overriding createCompositeForObject() allows a subclass to determine which fields of the object
 * should be contained in the composite column.  For example, we override this function for the FavoriteDAO and
 * modify the composite column to be: entity_id/type/prop_name.
 * </p><p>
 * The function getComparatorTypeAlias() must be overridden to provide the data types the subclass plans to use
 * for each component of the composite column.  The most common case will be (UTF8Type, UTF8Type), but some use
 * cases call for other types.  For example, for LastNHistory we want to store the objects in the order that they
 * were created, so we choose (TimeUUIDType, UTF8Type) as our composite types.
 * </p><p>
 * TODO:
 * 1. Allow this class to work correctly with ColumnFamilyRowMapper.
 * 2. Use ColumnFamilyUpdater to save single objects (when it supports TTL).
 * 3. Quantify the performance degradation when the number of columns in a row grows large, particularly with
 * the getAll() function.
 * </p>
 */
public class CompositeDAO<K, T extends IPersistable> extends BaseDAO<K, Composite, T> implements ICompositeDAO<K, T> {
    private static Logger log = LoggerFactory.getLogger(CompositeDAO.class);

    /**
     * if true deleteAll operation will be optimized using {@link #createColumnNamesForGetAll}
     * instead of querying them by slice query
     */
    private boolean fastSliceMode;
    protected ICompositeMapper<K, T> mapper;
    protected CompositeMapperHelper<K, T> mapperHelper;

    public CompositeDAO(final String columnFamilyName, Class<K> keyClass, Class<T> valueClass) {
        this(columnFamilyName, BeanUtils.getSerializer(keyClass), new PersistableFactory<T>(valueClass),
                null);
    }

    public CompositeDAO(final String columnFamilyName, Serializer<K> keySerializer, IPersistable.Factory<T> factory,
                        IKeyGenerator keyGenerator) {
        //  set fastSliceMode to false by default for backward compatibility
        this(columnFamilyName, keySerializer, factory, keyGenerator, false);
    }

    public CompositeDAO(final String columnFamilyName, Serializer<K> keySerializer, IPersistable.Factory<T> factory,
                        IKeyGenerator keyGenerator, boolean fastSliceMode) {
        this(columnFamilyName,keySerializer, CompositeSerializer.get(), factory, keyGenerator, fastSliceMode);
    }

    public CompositeDAO(final String columnFamilyName, Serializer<K> keySerializer, Serializer<Composite> nameSerializer,
                        IPersistable.Factory<T> factory, IKeyGenerator keyGenerator, boolean fastSliceMode) {

        super(columnFamilyName, keySerializer, nameSerializer, factory);
        this.fastSliceMode = fastSliceMode;
        this.mapper = new ReflectionCompositeMapper<K, T>(new DefaultCompositePropertyProvider<K, T>(factory.getClassObject(), keyGenerator));
        this.mapperHelper = new CompositeMapperHelper<K, T>(mapper, factory);
        //XXX: set a column count based on number of fields of target bean and configured property
        //TODO: replace setCount with paging of the result of a slice query to make sure we load whole column list
        setPageSizeInternal(getPageSize());
    }

    public CompositeDAO(final String columnFamilyName, Serializer<K> keySerializer, IPersistable.Factory<T> factory,
                        IKeyGenerator keyGenerator, ICompositeMapper<K, T> mapper) {
        this(columnFamilyName, keySerializer, factory, keyGenerator, mapper, false);
    }

    /**
     * @param mapper should contain injected propertyProvider
     */
    public CompositeDAO(final String columnFamilyName, Serializer<K> keySerializer, IPersistable.Factory<T> factory,
                        IKeyGenerator keyGenerator, ICompositeMapper<K, T> mapper, boolean fastSliceMode) {
        this(columnFamilyName, keySerializer, CompositeSerializer.get(), factory, keyGenerator, mapper, fastSliceMode);
    }

    /**
     * @param mapper should contain injected propertyProvider
     */
    public CompositeDAO(final String columnFamilyName, Serializer<K> keySerializer, Serializer<Composite> nameSerializer,
                        IPersistable.Factory<T> factory, IKeyGenerator keyGenerator, ICompositeMapper<K, T> mapper,
                        boolean fastSliceMode) {

        super(columnFamilyName, keySerializer, nameSerializer, factory);
        this.fastSliceMode = fastSliceMode;
        this.mapper = mapper;
        this.mapperHelper = new CompositeMapperHelper<K, T>(mapper, factory);
        //XXX: set a column count based on number of fields of target bean and configured property
        //TODO: replace setCount with paging of the result of a slice query to make sure we load whole column list
        setPageSizeInternal(getPageSize());
    }

    public static CompositeType parserFromString(final String source) {
        try {
            return CompositeType.getInstance(new TypeParser(source));
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPageSizeInternal(int pageSize) {
        int columnCount = pageSize * getFieldCount();
        if (log.isDebugEnabled()) {
            log.debug("Set default columnCount: {} for columnFamily: {}",
                    columnCount, getColumnFamilyName());
        }
    }

    protected int getPageColumnNumber() {
        return getPageSize() * getFieldCount();
    }

    //==================================================================================================================
    //============================   ICompositeDAO implementation   ============================================

    @Override
    public boolean isFastSliceMode() {
        return fastSliceMode;
    }

    @Override
    public void setFastSliceMode(boolean fastSliceMode) {
        this.fastSliceMode = fastSliceMode;
    }

    @Override
    public List<T> getAll(final int maxResults) {
        final List<T> result = new ArrayList<T>();

        AllRowsReader<K, Composite> allRowsReader = new AllRowsReader.Builder<K, Composite>(getKeyspace(), columnFamily)
                .withPartitioner(ExecuteWithUncheckedException.getPartitioner(getKeyspace()))
                .withColumnRange(null,null, false, maxResults * getPageSize())
                .forEachRow(new Function<Row<K, Composite>, Boolean>() { // Function.apply() should be thread safe
                    private ReentrantLock lock = new ReentrantLock();
                    private int rowsProcessed = 0;

                    @Override
                    public Boolean apply(Row<K, Composite> row) {
                        List<T> partialResult = mapperHelper.mapToObjectList(row.getKey(), row.getColumns());
                        try {
                            lock.lock();
                            if (rowsProcessed < maxResults) {
                                result.addAll(partialResult);
                                rowsProcessed++;
                            }
                            return (rowsProcessed < maxResults);
                        } finally {
                            lock.unlock();
                        }
                    }
                })
                .build();

        ExecuteWithUncheckedException.execute(allRowsReader);

        return result;
    }

    /**
     * TODO: is not supported
     */
    @Override
    public List<T> getAll(List<Filter> filters, int maxResults, String deduplicateField) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> getPage(K rowKey, Composite start, int maxResults) {
        return getRange(rowKey, start, null, maxResults);
    }

    @Override
    public List<T> getRange(K rowKey, Composite start, Composite end, int maxResults) {
        return internalGetAll(rowKey, start, end, false, maxResults * getFieldCount());
    }

    @Override
    public List<T> getAll(K rowKey, List<Filter> filters, int maxResults, String deduplicateField) {

        final int pageSize = getPageSize();
        final long startTime = System.currentTimeMillis();

        List<T> results = new ArrayList<T>();
        Map<String, T> dedupeMap = new HashMap<String, T>();
        Composite start = null;
        int pageCount = 0;

        while (results.size() < maxResults) {
            long pageStartTime = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("Fetching page::: rowKey={} page={}", rowKey, ++pageCount);
            List<T> curPage = getPage(rowKey, start, pageSize);
            long retrievedPageSize = curPage.size();
            // the last object can be partially initialized, just ignore it
            //TODO: it will be replaced with  {@link me.prettyprint.cassandra.service.ColumnSliceIterator} in schedulator branch
            checkPartialResult(curPage, retrievedPageSize);
            T lastOne = !curPage.isEmpty() ? curPage.get(curPage.size() - 1) : null;
            // if a deduplicateField field is present we must deduplicate the results using the specified field.  we'll ask
            // for data one page at a time, and store the first instance of each duplciate, and when we have enough results
            // we'll return them to the client
            addResultChunk(results, curPage, dedupeMap, filters, deduplicateField);

            log.debug("Fetched page::: rowKey={} duration={}", rowKey, (pageStartTime - System.currentTimeMillis()));
            // we can not index our composite columns because the composite contains the object (or entity) ID, so
            // instead we need to implement filters manually.

            if (retrievedPageSize < pageSize || lastOne == null)
                break; // there are no more pages of data, so return what we have at this point

            // construct a composite that represents the start of the next page of objects
            start = createStartCompositeForObject(lastOne);
        }

        results = results.subList(0, Math.min(results.size(), maxResults));

        if (log.isDebugEnabled()) {
            log.debug("Fetched composite data {} duration={}", rowKey, (System.currentTimeMillis() - startTime));
        }
        return results;
    }

    @Override
    public List<T> getAll(K rowKey, List<Filter> filters, int maxResults) {
        return getAll(rowKey, filters, maxResults, null);
    }

    @Override
    public List<T> getAll(K rowKey, int maxResults) {
        return getAll(rowKey, new ArrayList<Filter>(), maxResults);
    }

    @Override
    public List<T> getAll(K rowKeyStart) {
        return getAll(rowKeyStart, Integer.MAX_VALUE);
    }

    @Override
    public List<T> getAll(K rowKey, List<String> ids) {
        if (rowKey == null) {
            throw new IllegalArgumentException();
        }

        if (ids != null && !ids.isEmpty()) {
            ArrayList<Composite> columnNames = createColumnNamesForGetAll(ids);
            return internalGetAll(rowKey, columnNames);
        } else {
            return internalGetAll(rowKey, null, null, false, getPageColumnNumber());
        }
    }

    @Override
    public List<T> getRange(List<K> rowKeys, Composite start, Composite end, int maxResults) {
        if (rowKeys.size() == 1) {
            return getRange(rowKeys.get(0), start, end, maxResults);
        }

        return internalGetAll(rowKeys, start, end, false, maxResults * getFieldCount());
    }

    /**
     *
     * @param rowKeys Must not be null.
     * @param ids If null, this only gets the first page of each row.
     * @param filters
     * @return
     */
    @Override
    public List<T> getAll(List<K> rowKeys, List<String> ids, List<Filter> filters) {
        if (rowKeys == null || rowKeys.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (log.isDebugEnabled()) {
            log.debug("Getting objects by list of rows {}, ids {}", rowKeys, ids);
        }

        final List<T> results;
        if (rowKeys.size() == 1) {
            results = getAll(rowKeys.get(0), ids);
        } else if (ids != null && !ids.isEmpty()) {
            List<Composite> columnNames = createColumnNamesForGetAll(ids);
            results = internalGetAll(rowKeys, columnNames);
        } else {
            //well, it looks like we have to query all columns
            //lets set column name count at least
            results = internalGetAll(rowKeys, null, null, false, getPageColumnNumber());
        }

        applyServerSideFilters(filters, results);

        return results;
    }

    /**
     * Lyle sez: This code is fundamentally flawed, and should be removed or replaced. You can't page column slices
     * over multiple rows unless all the rows have identical column names. To write a query like this you have to
     * page over each row independently, or do something like I did in getAllStreamed(), or what they did in the
     * schedulator branch. Does this query even get used by anything?
     */
    @Override
    public List<T> getAll(List<K> rowKeys, List<Filter> filters, int maxResults, String deduplicateField) {

        int pageSize = getPageSize();

        List<T> results = new ArrayList<T>();
        Map<String, T> dedupeMap = new HashMap<String, T>();
        Composite start = null;
        int pageCount = 0;

        while (results.size() < maxResults) {
            if (log.isDebugEnabled())
                log.debug("Fetching page::: rowKeys={} page={}", rowKeys, ++pageCount);
            List<T> curPage = getRange(rowKeys, start, null, Math.min(pageSize, maxResults));
            int retrievedPageSize = curPage.size();
            checkPartialResult(curPage, retrievedPageSize);
            T lastOne = !curPage.isEmpty() ? curPage.get(curPage.size() - 1) : null;
            addResultChunk(results, curPage, dedupeMap, filters, deduplicateField);

            if (retrievedPageSize < pageSize || lastOne == null)
                break;    // there are no more pages of data, so return what we have at this point

            // construct a composite that represents the start of the next page of objects
            start = createStartCompositeForObject(lastOne);
        }

        results = results.subList(0, Math.min(results.size(), maxResults));

        return results;
    }

    public List<T> getAll(List<K> rowKeys) {
        return internalGetAll(rowKeys, null, null, false, Integer.MAX_VALUE);
    }

    protected List<T> internalGetAll(K rowKey, Composite startColumn, Composite endColumn, boolean reversed, int maxColumns) {
        RowQuery<K, Composite> query = getKeyspace().prepareQuery(columnFamily).getRow(rowKey);
        query.withColumnRange(startColumn, endColumn, reversed, maxColumns);

        ColumnList<Composite> columnList = ExecuteWithUncheckedException.execute(query);
        return mapperHelper.mapToObjectList(rowKey, columnList);
    }

    protected List<T> internalGetAll(K rowKey, Collection<Composite> columns) {
        RowQuery<K, Composite> query = getKeyspace().prepareQuery(columnFamily).getRow(rowKey);
        query.withColumnSlice(columns);

        ColumnList<Composite> columnList = ExecuteWithUncheckedException.execute(query);
        return mapperHelper.mapToObjectList(rowKey, columnList);
    }

    protected List<T> internalGetAll(Iterable<K> rowKeys, Composite startColumn, Composite endColumn, boolean reversed, int maxColumnsPerRow) {
        RowSliceQuery<K, Composite> query = getKeyspace().prepareQuery(columnFamily).getRowSlice(rowKeys);
        query.withColumnRange(startColumn, endColumn, reversed, maxColumnsPerRow);

        Rows<K, Composite> rows = ExecuteWithUncheckedException.execute(query);

        List<T> result = new ArrayList<T>();
        for (Row<K, Composite> currRow : rows) {
            List<T> partialResult = mapperHelper.mapToObjectList(currRow.getKey(), currRow.getColumns());
            result.addAll(partialResult);
        }
        return result;
    }

    protected List<T> internalGetAll(Iterable<K> rowKeys, Collection<Composite> columns) {
        RowSliceQuery<K, Composite> query = getKeyspace().prepareQuery(columnFamily).getRowSlice(rowKeys);
        query.withColumnSlice(columns);

        Rows<K, Composite> rows = ExecuteWithUncheckedException.execute(query);

        List<T> result = new ArrayList<T>();
        for (Row<K, Composite> currRow : rows) {
            List<T> partialResult = mapperHelper.mapToObjectList(currRow.getKey(), currRow.getColumns());
            result.addAll(partialResult);
        }
        return result;
    }

    @Override
    public T getOne(K rowKey, String id) {
        if (id == null || rowKey == null) {
            throw new IllegalArgumentException();
        }

        Object idObject = getSafeObjectId(id);
        Composite startColumn = createComposite(idObject, getRangeStartComponent());
        Composite endColumn = createComposite(idObject, getRangeEndComponent());

        RowQuery<K, Composite> query = getKeyspace().prepareQuery(columnFamily).getRow(rowKey);
        query.withColumnRange(startColumn, endColumn, false, Integer.MAX_VALUE);

        ColumnList<Composite> columnList = ExecuteWithUncheckedException.execute(query);
        return mapperHelper.mapToObject(rowKey, columnList);
    }

    @Override
    public T setOne(K rowKey, T obj) throws ValidationException {
        ExecuteWithUncheckedException.execute(createMutationsForSetOne(rowKey, obj));
        return obj;
    }

    /**
     * Called by setOne(), which then just executes the mutations. Factored out to enable atomic updates across CFs.
     */
    protected MutationBatch createMutationsForSetOne(K rowKey, T obj) throws ValidationException {
        EntityValidationUtils.validateForSave(obj);

        if (obj.getId() == null) {
            obj.setId(generateID(obj));
        }

        MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        fillMutationBatch(mutationBatch, rowKey, obj);
        return mutationBatch;
    }

    @Override
    public List<T> setMultiple(K rowKey, List<T> list) throws ValidationException {
        ExecuteWithUncheckedException.execute(createMutationsForSetMultiple(rowKey, list));
        return list;
    }

    /**
     * Called by setMultiple(), which then just executes the mutations. Factored out to enable atomic updates across CFs.
     */
    protected MutationBatch createMutationsForSetMultiple(K rowKey, List<T> list) throws ValidationException {
        MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        for (T obj : list) {
            EntityValidationUtils.validateForSave(obj);

            if (obj.getId() == null) {
                obj.setId(generateID(obj));
            }

            fillMutationBatch(mutationBatch, rowKey, obj);
        }
        return mutationBatch;
    }

    @Override
    public void deleteAll(K rowKey) {
        MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        mutationBatch.withRow(columnFamily, rowKey).delete();
        ExecuteWithUncheckedException.execute(mutationBatch);
    }

    @Override
    public void deleteOne(K rowKey, String id) {
        MutationBatch mutations = createMutationsForDeleteOne(rowKey, id);
        ExecuteWithUncheckedException.execute(mutations);
    }

    /**
     * Called by deleteOne(), which then just executes the mutations. Factored out to enable atomic updates across CFs.
     */
    protected MutationBatch createMutationsForDeleteOne(K rowKey, String id) {
        Object idObject = getSafeObjectId(id);
        List<Composite> columnsToBeDeleted = queryColumnsForDeletion(rowKey, Collections.singletonList(idObject));
        return applyDeletion(rowKey, columnsToBeDeleted);
    }

    @Override
    public void deleteAll(K rowKey, List<T> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        Iterable<Object> ids = Iterables.transform(objects, new Function<T, Object>() {
            @Override
            public Object apply(T input) {
                return input.getId();
            }
        });

        List<Composite> slice = queryColumnsForDeletion(rowKey, Lists.newArrayList(ids));
        MutationBatch mutations = applyDeletion(rowKey, slice);
        ExecuteWithUncheckedException.execute(mutations);
    }

    //==================================================================================================================
    // ===================================      helper methods      ====================================================

    protected Object getSafeObjectId(Object id) {
        if (id instanceof String) {
            throw new UnsupportedOperationException();
        }
        return id;
    }


    @Override
    public String generateID(T obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates the start component of column name for paging.
     * This is simply a column name for a fictional field of lastOne named "~", the last graphic ASCII character.
     *
     * @param lastOne last loaded object
     * @return
     */
    protected Composite createStartCompositeForObject(T lastOne) {
        return getProvider().createCompositeForObject(lastOne, getRangeEndComponent());
    }

    protected ICompositePropertyProvider<T> getProvider() {
        return mapper.getProvider();
    }

    /*
     * TODO: The test case CompositeDAOTest.demonstrateGetAllFailures() shows how this is broken. This whole approach
     * of doing explicit paging should be discarded and replaced with use of ColumnRangeIterator, which is simpler and
     * works correctly.
     *
     * Lyle sez: The description of this method explains that if |constructed objects| > |requested objects|, the
     * last object may be partial. But then the coded criterion is |constructed objects| >= |requested objects|.
     * Why is '>=' used?
     *
     * Also: This is not safe under a schema change where a field is removed from an object. For example, an object
     * had 4 fields and data was stored that way, then a field is removed or no longer persisted. If you request 10
     * objects, that's 30 columns, but the old data will return 30/4=7.5 objects with a partial 8th object. But
     * since 8 < 10, this method does not discard the 8th object. The only way to do this correctly is to operate on
     * column counts, not object counts, i.e. discard the last object in page unless receivedCols < requestedCols.
     */
    protected void checkPartialResult(List<T> curPage, long retrievedPageSize) {
        /**
         * At point we need to decide whether this result has partial initialized objects.
         * It will have iff we construct from the column list more objects than want
         * because there were some objects with null values and all logic of column calculation(maxResults* getFieldCount)
         * becomes invalid due to the number of column is not divisible by the getFieldCount.
         * taking into account it the last object can be partially initialized,
         * let's remove the last one and it will be retrieved with next page
         */
        if (retrievedPageSize >= getPageSize()
                // we nee ignore valuesless-based object and object with one field because we may remove valid object
                && getFieldCount() > 1) { // single-field objects can never be partial
            curPage.remove(curPage.size() - 1);
        }
    }

    protected void applyServerSideFilters(List<Filter> filters, List<T> results) {
        throw new UnsupportedOperationException();
    }

    protected void addResultChunk(List<T> results, List<T> curPage, Map<String, T> dedupeMap, List<Filter> filters, String deduplicateField) {
        // if a deduplicateField field is present we must deduplicate the results using the specified field.  we'll ask
        // for data one page at a time, and store the first instance of each duplicate, and when we have enough results
        // we'll return them to the client
        List<T> chunkResult = new ArrayList<T>();
        if (deduplicateField != null) {
            if (curPage.size() > 0) {
                for (T obj : curPage) {
                    Object dedupeValue = DataUtils.invokeGetter(obj, deduplicateField);
                    if (dedupeValue != null && !dedupeMap.containsKey(dedupeValue.toString())) {
                        // this one isn't already in our list, we want to keep it
                        dedupeMap.put(dedupeValue.toString(), obj); // obj is never used
                        chunkResult.add(obj);
                    }
                }
            }
        } else {
            chunkResult = curPage;
        }

        // we can not index our composite columns because the composite contains the object (or entity) ID, so
        // instead we need to implement filters manually.
        if (filters != null && filters.size() > 0) {
            applyServerSideFilters(filters, chunkResult);
        }
        results.addAll(chunkResult);
    }

    protected ArrayList<Composite> createColumnNamesForGetAll(List ids) {
        ArrayList<Composite> composites = new ArrayList<Composite>();
        for (Object id : ids) {
            Object idObject = getSafeObjectId(id);
            List<Composite> compositesForId = buildCompositesForId(idObject);
            composites.addAll(compositesForId);
        }

        return composites;
    }

    protected List<Composite> buildCompositesForId(Object idObject) {
        List<Composite> composites = new ArrayList<Composite>();
        String[] columnNames = getProvider().getColumnNames();
        for (int i = 0; i < columnNames.length; i++) {
            composites.add(createComposite(idObject, columnNames[i]));
        }
        return composites;
    }

    protected void fillMutationBatch(MutationBatch mutationBatch, K key, T entity) {
        ColumnListMutation<Composite> columnListMutation = mutationBatch.withRow(columnFamily, key);
        entity.setUpdated(new Date());
        mapper.mapToMutation(entity, columnListMutation);
    }

    protected MutationBatch applyDeletion(K rowKey, List<Composite> slice) {
        MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        ColumnListMutation<Composite> columnListMutation = mutationBatch.withRow(columnFamily, rowKey);
        for (Composite column : slice) {
            columnListMutation.deleteColumn(column);
        }
        return mutationBatch;
    }

    protected List<Composite> queryColumnsForDeletion(K rowKey, List<Object> ids) {
        if (isFastSliceMode()) {
            return createColumnNamesForGetAll(ids);
        }

        if (ids.size() > 1) {
            throw new UnsupportedOperationException("Can't delete by multiple ids. use fast slice mode");
        }

        Object idObject = getSafeObjectId(ids.get(0).toString());
        Composite startColumn = createComposite(idObject, getRangeStartComponent());
        Composite endColumn = createComposite(idObject, getRangeEndComponent());

        RowQuery<K, Composite> query = getKeyspace().prepareQuery(columnFamily).getRow(rowKey);
        query.withColumnRange(startColumn, endColumn, false, Integer.MAX_VALUE);

        ColumnList<Composite> columnList = ExecuteWithUncheckedException.execute(query);
        return new ArrayList<Composite>(columnList.getColumnNames());
    }

    protected List<T> dedupliate(List<T> list) {
        Map<String, T> resultsMap = new HashMap<String, T>();
        for (T obj : list) {
            if (!resultsMap.containsKey(obj.getId())) {
                resultsMap.put(obj.getId(), obj);
            }
        }
        return new ArrayList<T>(resultsMap.values());
    }

    /**
     * Creates a new Composite column using just an ID and property name.
     *
     * @param id                The ID to use in the Composite column.
     * @param propertyComponent The property name to use in the Composite column.
     * @return The newly created Composite column name.
     */
    protected Composite createComposite(Object id, String propertyComponent) {
        return new Composite(id, propertyComponent);
    }


    protected String getRangeStartComponent() {
        return ""; // the first ASCII character (null)
    }

    protected String getRangeEndComponent() {
        return "~"; // the penultimate ASCII character
    }
}
