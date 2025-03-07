// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.catalog.external;

import org.apache.commons.lang.NotImplementedException;
import org.apache.doris.catalog.DatabaseIf;
import org.apache.doris.catalog.DatabaseProperty;
import org.apache.doris.catalog.OlapTable;
import org.apache.doris.catalog.Table;
import org.apache.doris.common.AnalysisException;
import org.apache.doris.common.DdlException;
import org.apache.doris.common.MetaNotFoundException;
import org.apache.doris.datasource.ExternalDataSource;
import org.apache.doris.qe.ConnectContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Base class of external database.
 *
 * @param <T> External table type is ExternalTable or its subclass.
 */
public class ExternalDatabase<T extends ExternalTable> implements DatabaseIf<T> {

    private static final Logger LOG = LogManager.getLogger(ExternalDatabase.class);

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    protected long id;
    protected String name;
    protected ExternalDataSource extDataSource;
    protected DatabaseProperty dbProperties;

    /**
     * Create external database.
     *
     * @param extDataSource The data source this database belongs to.
     * @param id Database id.
     * @param name Database name.
     */
    public ExternalDatabase(ExternalDataSource extDataSource, long id, String name) {
        this.extDataSource = extDataSource;
        this.id = id;
        this.name = name;
    }

    @Override
    public void readLock() {
        this.rwLock.readLock().lock();
    }

    @Override
    public void readUnlock() {
        this.rwLock.readLock().unlock();
    }

    @Override
    public void writeLock() {
        this.rwLock.writeLock().lock();
    }

    @Override
    public void writeUnlock() {
        this.rwLock.writeLock().unlock();
    }

    @Override
    public boolean tryWriteLock(long timeout, TimeUnit unit) {
        try {
            return this.rwLock.writeLock().tryLock(timeout, unit);
        } catch (InterruptedException e) {
            LOG.warn("failed to try write lock at external db[" + id + "]", e);
            return false;
        }
    }

    @Override
    public boolean isWriteLockHeldByCurrentThread() {
        return this.rwLock.writeLock().isHeldByCurrentThread();
    }

    @Override
    public boolean writeLockIfExist() {
        writeLock();
        return true;
    }

    @Override
    public <E extends Exception> void writeLockOrException(E e) throws E {
        writeLock();
    }

    @Override
    public void writeLockOrDdlException() throws DdlException {
        writeLock();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getFullName() {
        return name;
    }

    @Override
    public DatabaseProperty getDbProperties() {
        return dbProperties;
    }

    @Override
    public boolean isTableExist(String tableName) {
        return extDataSource.tableExist(ConnectContext.get().getSessionContext(), name, tableName);
    }

    @Override
    public List<T> getTables() {
        throw new NotImplementedException();
    }

    @Override
    public List<T> getTablesOnIdOrder() {
        throw new NotImplementedException();
    }

    @Override
    public List<T> getViews() {
        throw new NotImplementedException();
    }

    @Override
    public List<T> getTablesOnIdOrderIfExist(List<Long> tableIdList) {
        throw new NotImplementedException();
    }

    @Override
    public List<T> getTablesOnIdOrderOrThrowException(List<Long> tableIdList) throws MetaNotFoundException {
        throw new NotImplementedException();
    }

    @Override
    public Set<String> getTableNamesWithLock() {
        throw new NotImplementedException();
    }

    @Override
    public T getTableNullable(String tableName) {
        throw new NotImplementedException();
    }

    @Override
    public T getTableNullable(long tableId) {
        throw new NotImplementedException();
    }

    @Override
    public Optional<T> getTable(String tableName) {
        throw new NotImplementedException();
    }

    @Override
    public Optional<T> getTable(long tableId) {
        throw new NotImplementedException();
    }

    @Override
    public <E extends Exception> T getTableOrException(String tableName, Function<String, E> e) throws E {
        throw new NotImplementedException();
    }

    @Override
    public <E extends Exception> T getTableOrException(long tableId, Function<Long, E> e) throws E {
        throw new NotImplementedException();
    }

    @Override
    public T getTableOrMetaException(String tableName) throws MetaNotFoundException {
        throw new NotImplementedException();
    }

    @Override
    public T getTableOrMetaException(long tableId) throws MetaNotFoundException {
        throw new NotImplementedException();
    }

    @Override
    public T getTableOrMetaException(String tableName, Table.TableType tableType)
            throws MetaNotFoundException {
        throw new NotImplementedException();
    }

    @Override
    public T getTableOrMetaException(long tableId, Table.TableType tableType)
            throws MetaNotFoundException {
        throw new NotImplementedException();
    }

    @Override
    public T getTableOrDdlException(String tableName) throws DdlException {
        throw new NotImplementedException();
    }

    @Override
    public T getTableOrDdlException(long tableId) throws DdlException {
        throw new NotImplementedException();
    }

    @Override
    public T getTableOrAnalysisException(String tableName) throws AnalysisException {
        throw new NotImplementedException();
    }

    @Override
    public T getTableOrAnalysisException(long tableId) throws AnalysisException {
        throw new NotImplementedException();
    }

    @Override
    public OlapTable getOlapTableOrAnalysisException(String tableName) throws AnalysisException {
        throw new NotImplementedException();
    }
}
