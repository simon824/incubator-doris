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

package org.apache.doris.datasource;

import org.apache.doris.catalog.DatabaseIf;
import org.apache.doris.common.AnalysisException;
import org.apache.doris.common.DdlException;
import org.apache.doris.common.MetaNotFoundException;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 *
 */
public interface DataSourceIf {

    // Type of this data source
    String getType();

    long getId();

    // Name of this data source
    String getName();

    List<String> getDbNames();

    @Nullable
    DatabaseIf getDbNullable(String dbName);

    @Nullable
    DatabaseIf getDbNullable(long dbId);

    Optional<DatabaseIf> getDb(String dbName);

    Optional<DatabaseIf> getDb(long dbId);

    <E extends Exception> DatabaseIf getDbOrException(String dbName, java.util.function.Function<String, E> e) throws E;

    <E extends Exception> DatabaseIf getDbOrException(long dbId, java.util.function.Function<Long, E> e) throws E;

    DatabaseIf getDbOrMetaException(String dbName) throws MetaNotFoundException;

    DatabaseIf getDbOrMetaException(long dbId) throws MetaNotFoundException;

    DatabaseIf getDbOrDdlException(String dbName) throws DdlException;

    DatabaseIf getDbOrDdlException(long dbId) throws DdlException;

    DatabaseIf getDbOrAnalysisException(String dbName) throws AnalysisException;

    DatabaseIf getDbOrAnalysisException(long dbId) throws AnalysisException;
}
