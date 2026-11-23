#
# Licensed to Crate.io GmbH ("Crate") under one or more contributor
# license agreements.  See the NOTICE file distributed with this work for
# additional information regarding copyright ownership.  Crate licenses
# this file to you under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.  You may
# obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations
# under the License.
#
# However, if you have executed another commercial license agreement
# with Crate these terms will supersede the license and you may use the
# software solely pursuant to the terms of the relevant commercial agreement.

import base64
import json
import os
import unittest
import uuid
from urllib.request import Request, urlopen


class OrderWorkflowSystemTest(unittest.TestCase):
    def execute_sql(self, statement, args=()):
        # Use the same server settings
        endpoint = os.environ.get("CRATE_HTTP_URL", "http://127.0.0.1:4200")
        payload = json.dumps({"stmt": statement, "args": args}).encode("utf-8")
        authorization = base64.b64encode(b"crate:").decode("ascii")
        request = Request(
            endpoint.rstrip("/") + "/_sql",
            data=payload,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Basic {authorization}",
            },
            method="POST",
        )
        with urlopen(request, timeout=30) as response:
            self.assertEqual(response.status, 200)
            result = json.load(response)
        self.assertNotIn("error", result)
        return result

    def setUp(self):
        # Give each test its own table so tests can run in any order.
        self.table_name = f"doc.qa_orders_{uuid.uuid4().hex}"
        # Register cleanup first so it also runs if setup or an assertion fails.
        self.addCleanup(
            self.execute_sql,
            f"DROP TABLE IF EXISTS {self.table_name}",
        )
        self.execute_sql(
            f"CREATE TABLE {self.table_name} "
            "(id INTEGER PRIMARY KEY, status TEXT, amount INTEGER) "
            "CLUSTERED INTO 1 SHARDS WITH (number_of_replicas = 0)"
        )
        empty_orders = self.execute_sql(
            f"SELECT id, status, amount FROM {self.table_name} ORDER BY id"
        )
        self.assertEqual(empty_orders["cols"], ["id", "status", "amount"])
        self.assertEqual(empty_orders["rows"], [])
        self.assertEqual(empty_orders["rowcount"], 0)

        # Every test starts with two paid orders and one pending order.
        insert_result = self.execute_sql(
            f"INSERT INTO {self.table_name} (id, status, amount) "
            "VALUES (?, ?, ?), (?, ?, ?), (?, ?, ?)",
            [1, "paid", 40, 2, "paid", 60, 3, "pending", 25],
        )
        self.assertEqual(insert_result["rowcount"], 3)
        # Refresh makes the inserted rows visible to the following queries.
        self.execute_sql(f"REFRESH TABLE {self.table_name}")

    def test_paid_order_summary(self):
        orders = self.execute_sql(
            f"SELECT id, status, amount FROM {self.table_name} ORDER BY id"
        )
        self.assertEqual(
            orders["rows"],
            [[1, "paid", 40], [2, "paid", 60], [3, "pending", 25]],
        )
        self.assertEqual(orders["rowcount"], 3)

        paid_orders = self.execute_sql(
            f"SELECT COUNT(*), SUM(amount) FROM {self.table_name} WHERE status = ?",
            ["paid"],
        )
        self.assertEqual(paid_orders["rows"], [[2, 100]])
        # The count and total are returned together in one summary row.
        self.assertEqual(paid_orders["rowcount"], 1)

    def test_mark_pending_order_as_paid(self):
        update_result = self.execute_sql(
            f"UPDATE {self.table_name} SET status = ? WHERE id = ?",
            ["paid", 3],
        )
        self.assertEqual(update_result["rowcount"], 1)
        self.execute_sql(f"REFRESH TABLE {self.table_name}")

        paid_orders = self.execute_sql(
            f"SELECT COUNT(*), SUM(amount) FROM {self.table_name} WHERE status = ?",
            ["paid"],
        )
        self.assertEqual(paid_orders["rows"], [[3, 125]])
        self.assertEqual(paid_orders["rowcount"], 1)

    def test_delete_paid_order(self):
        delete_result = self.execute_sql(
            f"DELETE FROM {self.table_name} WHERE id = ?",
            [1],
        )
        self.assertEqual(delete_result["rowcount"], 1)
        self.execute_sql(f"REFRESH TABLE {self.table_name}")

        remaining_orders = self.execute_sql(
            f"SELECT id, status, amount FROM {self.table_name} ORDER BY id"
        )
        self.assertEqual(remaining_orders["rows"], [[2, "paid", 60], [3, "pending", 25]])
        self.assertEqual(remaining_orders["rowcount"], 2)

        # Order 3 is still pending in this test's fresh fixture.
        paid_orders = self.execute_sql(
            f"SELECT COUNT(*), SUM(amount) FROM {self.table_name} WHERE status = ?",
            ["paid"],
        )
        self.assertEqual(paid_orders["rows"], [[1, 60]])
        self.assertEqual(paid_orders["rowcount"], 1)


if __name__ == "__main__":
    unittest.main()
