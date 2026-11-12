/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.integrationtests;

import static io.crate.testing.Asserts.assertThat;

import org.elasticsearch.test.IntegTestCase;
import org.junit.Test;

public class FilteredSelectIntegrationTest extends IntegTestCase {

    @Test
    public void testSelectFiltersRowsAndReturnsAliasedColumnInOrder() throws Exception {
        // Use the real database path so table creation, storage, and query execution interact.
        execute("create table users (name string, department string) clustered into 1 shards with (number_of_replicas=0)");

        // Include matching and non-matching departments to verify that WHERE filters the stored rows.
        execute("insert into users (name, department) values (?, ?)", new Object[][]{
            new Object[]{"Alice", "IT"},
            new Object[]{"Bob", "IT"},
            new Object[]{"Cara", "HR"}
        });

        // Make the inserted rows visible before running the SELECT query.
        execute("refresh table users");

        // Test filtering, column aliasing, ordering, and result delivery together.
        execute("select name as employee from users where department = 'IT' order by name");

        // Cara must be excluded, while the matching names must be returned in order.
        assertThat(response).hasRows("Alice", "Bob");
    }
}
