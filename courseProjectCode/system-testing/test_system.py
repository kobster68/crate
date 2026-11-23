"""Black-box workflows against a running CrateDB HTTP SQL endpoint."""

import json
import os
import unittest
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen


class CrateSystemTests(unittest.TestCase):
    # unittest runs each method whose name starts with test_ as a separate case.
    def sql(self, statement, args=None):
        """Send SQL through the public API and return the decoded JSON response."""
        # Read the server URL from the environment so the same tests can use
        # different local ports without changing the test code.
        endpoint = os.environ.get("CRATE_HTTP_URL", "http://127.0.0.1:4200")
        # CrateDB expects a JSON object with the SQL statement and its arguments.
        # Arguments bind to ? placeholders in order; encode() converts JSON text
        # into the bytes required for an HTTP request body.
        request = Request(
            endpoint.rstrip("/") + "/_sql",
            data=json.dumps({"stmt": statement, "args": args or []}).encode(),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            # The context manager closes the connection after reading the body.
            # A timeout prevents an unresponsive request from waiting forever.
            with urlopen(request, timeout=15) as response:
                self.assertEqual(response.status, 200)
                # Parse JSON into a Python dictionary so tests can check fields
                # such as cols (column names), rows, and rowcount.
                return json.load(response)
        except HTTPError as error:
            # Include the server's error body in unittest's failure report.
            # Connection errors propagate and are reported as test errors.
            self.fail(f"SQL request failed: HTTP {error.code}: {error.read().decode()}")

    def setUp(self):
        # unittest calls setUp before every test method, giving each case fresh data.
        # Each case owns a unique table; cleanup never targets existing user tables.
        self.table = "doc.system_test_" + uuid.uuid4().hex
        # One shard and no replicas let this table run on a single local node.
        self.sql(
            f"CREATE TABLE {self.table} "
            "(name STRING, course STRING) "
            "CLUSTERED INTO 1 SHARDS WITH (number_of_replicas = 0)"
        )
        # Register cleanup immediately after creation. unittest calls it after the
        # test finishes, including when an assertion fails or the test raises.
        self.addCleanup(self.sql, f"DROP TABLE IF EXISTS {self.table}")

    def test_filtered_student_search(self):
        """ST-01: Create, insert, filter, alias, and sort student records."""
        # Match the previous integration test: Alice and Bob take SWEN-777;
        # Kara takes SWEN-755 and must be excluded from the query results.
        inserted = self.sql(
            f"INSERT INTO {self.table} (name, course) VALUES "
            "(?, ?), (?, ?), (?, ?)",
            ["Alice", "SWEN-777", "Bob", "SWEN-777", "Kara", "SWEN-755"],
        )
        self.assertEqual(inserted["rowcount"], 3)
        # CrateDB searches see changes after a refresh. Refresh explicitly rather
        # than relying on the automatic refresh interval or an arbitrary sleep.
        self.sql(f"REFRESH TABLE {self.table}")
        result = self.sql(
            f"SELECT name AS student FROM {self.table} WHERE course = 'SWEN-777' ORDER BY name",
        )
        # Check the public response: the alias, exact values and order, and count.
        # Each nested list in rows represents one returned database record.
        self.assertEqual(result["cols"], ["student"])
        self.assertEqual(result["rows"], [["Alice"], ["Bob"]])
        self.assertEqual(result["rowcount"], 2)



if __name__ == "__main__":
    # Running this file directly starts unittest, which runs setUp, each test,
    # and its registered cleanup. Verbosity 2 prints individual case results;
    # unittest exits with a nonzero status if a test fails or encounters an error.
    unittest.main(verbosity=2)
