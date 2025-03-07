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

package org.apache.doris.nereids.analyzer;

import org.apache.doris.nereids.exceptions.UnboundException;
import org.apache.doris.nereids.trees.NodeType;
import org.apache.doris.nereids.trees.expressions.ExprId;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.NamedExpression;
import org.apache.doris.nereids.trees.expressions.UnaryExpression;

import java.util.List;

/**
 * Expression for unbound alias.
 */
public class UnboundAlias<CHILD_TYPE extends Expression>
        extends NamedExpression
        implements UnaryExpression<CHILD_TYPE> {

    public UnboundAlias(CHILD_TYPE child) {
        super(NodeType.UNBOUND_ALIAS, child);
    }

    @Override
    public String sql() {
        return null;
    }

    @Override
    public String getName() throws UnboundException {
        return null;
    }

    @Override
    public ExprId getExprId() throws UnboundException {
        return null;
    }

    @Override
    public List<String> getQualifier() throws UnboundException {
        return null;
    }

    @Override
    public String toString() {
        return "UnboundAlias(" + child() + ", None)";
    }
}
