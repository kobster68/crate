# Requirements and Test Oracles

Project: **CrateDB**. This document contains **6 functional requirements, 3 non-functional requirements, and 9 linked test oracles**, covering the assignment minimum for a three-student team. These are proposed checks, not results from executed tests or a record of individual contributions.

## Extraction Scope

Requirements were extracted from the team repository's README, reference documentation, and Java code comments at commit [`30ba40f4b0a61502c190cde0e3cfac33b3f388c7`](https://github.com/kobster68/crate/tree/30ba40f4b0a61502c190cde0e3cfac33b3f388c7). Source links below are pinned to that revision so reviewers can reproduce the extraction even after the repository changes.

The README identifies supported features and system qualities. Documentation supplies their observable contracts and configuration limits. Code comments provide additional evidence for refresh visibility and durability. The statements below paraphrase those sources into testable requirements; fixture sizes and test procedures are our proposed operational checks.

## Functional Requirements

1. **FR-1 — SQL over HTTP.** The system shall accept SQL statements through `POST /_sql` using a JSON `stmt` and optional `args`, and return query results containing column names, rows, and a row count. **Sources:** [S1], [S2].

2. **FR-2 — Update on a primary-key conflict.** The system shall apply the assignments in `INSERT ... ON CONFLICT (primary_key) DO UPDATE SET ...` to the existing row when the insert conflicts with its primary key, instead of creating a second row. **Source:** [S3].

3. **FR-3 — Dynamic schema expansion.** For a table explicitly configured with `column_policy = 'dynamic'`, the system shall accept a previously undefined column during insertion, infer its type from the supplied value, expose it in `information_schema.columns`, and allow it to be queried after schema propagation. **Sources:** [S1], [S4].

4. **FR-4 — Strict schema enforcement.** For a table explicitly configured with `column_policy = 'strict'`, the system shall reject an insertion that references a column absent from the schema. **Source:** [S4].

5. **FR-5 — Full-text search.** The system shall support `MATCH` searches against a text column indexed using a full-text analyzer, returning rows whose analyzed tokens match the search term. **Sources:** [S1], [S5], [S6], [S7].

6. **FR-6 — Explicit refresh visibility.** After a successful `REFRESH TABLE` on an open table, the system shall make writes completed before the refresh visible to subsequent search queries. **Sources:** [S8] and the `RefreshRequest` class comment [S9]. The comment describes making operations since the previous refresh available for search, supporting the SQL-level contract.

## Non-Functional Requirements

1. **NFR-1 — Availability and fault recovery.** A replicated table shall recover query availability after the loss of a node holding a primary shard by promoting a surviving replica, provided an in-sync copy of every affected shard remains available and the surviving cluster retains a voting quorum. **Sources:** README availability/self-healing claims [S1], documented shard recovery [S10], and quorum requirements [S11]. This is a conditional recovery requirement, not a promise of uninterrupted requests or a fixed failover time.

2. **NFR-2 — Horizontal scalability.** With shard allocation and rebalancing enabled and adequate capacity, the system shall distribute existing table shards onto eligible nodes added to the cluster while preserving the table's logical query results. **Sources:** README horizontal scaling and rebalancing claims [S1], and documented shard distribution when nodes join [S12]. This checks the ability to scale out; it does not imply linear speedup.

3. **NFR-3 — Durability under request synchronization.** With `translog.durability = 'REQUEST'` and an intact persistent data directory, the system shall recover successfully acknowledged writes after an unclean process shutdown and restart. **Sources:** documented translog replay [S13], the durability setting [S14], and comments on `Translog.Durability.REQUEST` and `ASYNC` [S15]. The comments distinguish synchronization per request from synchronization on a timer; the requirement deliberately specifies `REQUEST`.

## Test Oracles

A test oracle is the basis for deciding whether observed behavior is correct. Here, specification oracles use documented contracts and hand-calculated expected values; state-transition oracles check the intended before/after change; metamorphic oracles require data to remain unchanged across a topology change or restart. Each oracle below identifies its requirement, setup/action, and expected result.

### Common Setup and Assumptions

- Use a disposable test database built from the cited revision, an authorized test account, and fresh `doc.qa_*` tables. Run scenarios independently with no other writers. Unless a scenario says otherwise, explicitly set `number_of_replicas = 0` for a single-node test.
- Wait for setup statements to succeed. Before asserting search results, explicitly run `REFRESH TABLE` on the relevant table. O6 specifies exactly where its refresh occurs. Use `ORDER BY` for ordered comparisons; compare JSON values rather than whitespace or object-key order.
- Dynamic schema propagation, node discovery, shard relocation, failover, and restart are asynchronous. Poll the relevant metadata/readiness condition rather than using a fixed sleep. [S4] explicitly warns that a newly added column can temporarily be unknown on another node.
- For a bounded classroom run, use a **proposed 120-second observation limit** for asynchronous readiness/recovery, recording elapsed time and final state. This is a test-harness assumption, not an extracted CrateDB performance guarantee. A timeout means the expected state was not observed within the test window and needs investigation; it does not establish a violated vendor time limit. Missing prerequisites make the scenario blocked, not passed.
- NFR-1 and NFR-2 require a controlled cluster with node discovery configured, adequate disk space, and no allocation filters excluding the intended nodes. NFR-1 starts with three master-eligible nodes in the voting configuration and healthy replicas. NFR-3 requires a persistent volume and storage that honors synchronization requests.

| Oracle ID | Requirement ID | Requirement Description | Test Oracle (Expected Behavior) |
| --- | --- | --- | --- |
| O1 | FR-1 | Submit parameterized SQL over HTTP. | **Specification oracle.** POST `{"stmt":"SELECT CAST(? AS INTEGER) + 1 AS answer", "args":[41]}` to `/_sql` with JSON content type. The response is successful and contains `cols: ["answer"]`, `rows: [[42]]`, and `rowcount: 1`, with no SQL error. Additional documented response fields such as `duration` are allowed. The expected value 42 is calculated independently of CrateDB. |
| O2 | FR-2 | Update the existing row on a primary-key conflict. | **State-transition oracle.** Create `doc.qa_upsert (id INTEGER PRIMARY KEY, visits INTEGER)` and insert `(1, 1)`. Execute `INSERT INTO doc.qa_upsert (id, visits) VALUES (1, 1) ON CONFLICT (id) DO UPDATE SET visits = visits + 1`. After refresh, `SELECT id, visits FROM doc.qa_upsert ORDER BY id` returns exactly `[(1, 2)]`; there is one row, not two. |
| O3 | FR-3 | Add and query a dynamically inferred column. | **Specification and metadata oracle.** Create `doc.qa_dynamic (id INTEGER PRIMARY KEY)` with `column_policy = 'dynamic'`. Insert `(id, new_col) VALUES (1, 7)`. Once schema propagation completes, `information_schema.columns`, filtered to schema `doc`, table `qa_dynamic`, and column `new_col`, contains exactly one entry with `data_type = 'bigint'`. After refresh, selecting `id, new_col` returns exactly `[(1, 7)]`. The integer-literal-to-`BIGINT` expectation follows the documented example in [S4]. |
| O4 | FR-4 | Reject an unknown column in a strict table. | **Negative specification oracle.** Create empty `doc.qa_strict (id INTEGER PRIMARY KEY)` with `column_policy = 'strict'`. Attempt `INSERT INTO doc.qa_strict (id, new_col) VALUES (1, 7)`. Expect an unknown-column error identifying `new_col` (the documented example is `ColumnUnknownException`). After refresh, the table still contains zero rows, and `information_schema.columns` contains no `new_col` entry for this table. Do not require identical error formatting across clients. |
| O5 | FR-5 | Find the expected full-text matches. | **Specification oracle with a fixed dataset.** Create `doc.qa_search (id INTEGER PRIMARY KEY, body TEXT INDEX USING FULLTEXT WITH (analyzer = 'standard'))`. Insert `(1, 'red apple')`, `(2, 'green pear')`, and `(3, 'apple pie')`, then refresh. `SELECT id FROM doc.qa_search WHERE MATCH(body, 'apple') ORDER BY id` returns exactly `[(1), (3)]`; searching for `'banana'` returns no rows. Explicit analyzer choice and ordering make the result reproducible. Do not assert exact relevance scores. |
| O6 | FR-6 | Make completed writes searchable after refresh. | **Specification and code-comment oracle.** Create `doc.qa_refresh (id INTEGER PRIMARY KEY, label TEXT)` with `refresh_interval = 0`, then insert `(1, 'visible')`. Run `REFRESH TABLE doc.qa_refresh` and then `SELECT id, label FROM doc.qa_refresh ORDER BY id`. Expect exactly `[(1, 'visible')]`. The query scans the table rather than using a primary-key lookup. Do **not** require the row to be absent before refresh: disabling the periodic refresh does not prevent every other refresh trigger [S16]. |
| O7 | NFR-1 | Recover availability after losing a primary node. | **Recovery/state-transition oracle.** On the healthy three-node cluster, create `doc.qa_failover (id INTEGER PRIMARY KEY)` with one primary shard and one replica. Insert IDs 1, 2, and 3; refresh and record the ordered result. Confirm table health is `GREEN` and the primary and replica occupy different nodes. Stop only the node hosting the primary. With two voting nodes surviving, wait for recovery, then refresh via a survivor. The ordered IDs remain exactly `[1, 2, 3]`; `sys.shards` shows the affected shard has a `STARTED` primary on a surviving node. A temporary `YELLOW` state while replicas rebuild is not data loss. [S17], [S18] define the metadata observations. |
| O8 | NFR-2 | Redistribute shards without changing query results. | **Metamorphic oracle.** On a two-node cluster, create `doc.qa_scale (id INTEGER PRIMARY KEY)` with six primary shards and zero replicas; insert IDs 1 through 12 and refresh. Record ordered IDs, `COUNT(*) = 12`, `SUM(id) = 78`, and shard placement. Add a third eligible data node. After rebalancing settles, `sys.shards` still shows six primary shards for this table, all `STARTED`, with at least one on the new node. Queries through each node return the same ordered IDs and aggregates. Do not require a particular shard-to-node assignment or a measured speedup. |
| O9 | NFR-3 | Preserve acknowledged writes across an unclean restart. | **Durability/metamorphic oracle.** In a disposable single-node instance, create `doc.qa_durable (id INTEGER PRIMARY KEY, value INTEGER)` with zero replicas and `translog.durability = 'REQUEST'`. Insert `(1, 10)`, `(2, 20)`, and `(3, 30)` and wait for successful acknowledgments. Force-stop the process without a graceful shutdown; keep its data directory. Restart the same instance from that directory, wait for recovery, and refresh. `SELECT id, value FROM doc.qa_durable ORDER BY id` must return exactly `[(1, 10), (2, 20), (3, 30)]`. Judge only acknowledged writes. This checks recovery behavior, not whether recovery happened to use the translog or already-persisted segments. |

### Interpretation Limits

- The README's qualitative speed claims and approximate ingestion claim do not specify hardware, workload, percentile, or measurement procedure. They are insufficient to extract a universal latency or throughput pass threshold. NFR-2 therefore checks observable scale-out behavior; any performance benchmark needs a separately agreed workload and target.
- Search visibility is distinct from durability. A refresh makes data searchable; it is not the durability setting. Primary-key lookups can see writes before refresh, so O6 uses a table scan [S8].
- NFR-1 does not cover simultaneous loss of all copies, loss of quorum, or every network-partition scenario. NFR-3 does not promise the same durability for `ASYNC`, lost volumes, or storage corruption [S11], [S14].
- Comments support requirements but can become stale. The code comments cited here agree with the documented contracts; if a future revision contradicts them, record the conflict for review instead of treating existing implementation output as automatically correct.

## Source Register

All links refer to the extraction commit. Line ranges identify the evidence used, not just a documentation landing page.

| Source | Source Type | Location and Evidence |
| --- | --- | --- |
| S1 | README | [README.rst, lines 23–57](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/README.rst#L23-L57): SQL interfaces, dynamic schemas, full-text search, scaling, availability, and rebalancing. |
| S2 | Documentation | [HTTP endpoint, lines 9–65](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/interfaces/http.rst#L9-L65): JSON request/response contract and parameter substitution. |
| S3 | Documentation | [INSERT conflict handling, lines 144–205](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/sql/statements/insert.rst#L144-L205): updating the existing primary-key row, including a counter example. |
| S4 | Documentation | [Column policy, lines 7–109](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/column-policy.rst#L7-L109): strict rejection, dynamic inference, metadata, and asynchronous propagation. |
| S5 | Documentation | [Fulltext search, lines 29–64](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/dql/fulltext.rst#L29-L64): `MATCH` and analyzed-token matching. |
| S6 | Documentation | [Fulltext index definition, lines 83–105](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/fulltext-indices.rst#L83-L105): creating an indexed column and selecting an analyzer. |
| S7 | Documentation | [Standard analyzer, lines 53–66](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/analyzers.rst#L53-L66): tokenization and lowercasing without stopwords. |
| S8 | Documentation | [REFRESH, lines 27–98](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/sql/statements/refresh.rst#L27-L98): visibility after refresh and primary-key lookup exceptions. |
| S9 | Code comment | [RefreshRequest.java, lines 30–38](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/server/src/main/java/org/elasticsearch/action/admin/indices/refresh/RefreshRequest.java#L30-L38): operations become available for search after refresh. |
| S10 | Documentation | [Replication, lines 95–170](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/replication.rst#L95-L170): replica promotion, recovery, placement on different nodes, and underreplication. |
| S11 | Documentation | [Master election, lines 129–162](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/concepts/clustering.rst#L129-L162): voting configuration and majority requirements. |
| S12 | Documentation | [Sharding, lines 13–36](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/sharding.rst#L13-L36): distribution when nodes join and configurable shard counts. |
| S13 | Documentation | [Storage durability, lines 82–99](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/concepts/storage-consistency.rst#L82-L99): persistent storage and translog replay on startup. |
| S14 | Documentation | [Translog settings, lines 747–772](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/sql/statements/create-table.rst#L747-L772): `REQUEST`, `ASYNC`, and the unsynchronized-data loss window. |
| S15 | Code comments | [Translog.java, lines 1451–1462](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/server/src/main/java/org/elasticsearch/index/translog/Translog.java#L1451-L1462): request-based versus interval-based synchronization. |
| S16 | Documentation | [Refresh interval, lines 421–452](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/sql/statements/create-table.rst#L421-L452): disabling periodic refresh does not guarantee stale reads. |
| S17 | Documentation | [Shard metadata](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/admin/system-information.rst#L927-L1070): table/shard identity, primary status, node identity, and shard state. |
| S18 | Documentation | [Table health](https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/admin/system-information.rst#L1936-L2015): `GREEN`, `YELLOW`, and `RED` definitions. |

## Peer Review and Submission

Submit this file as a pull request to the team repository. Before merging, obtain a human peer review that checks source accuracy, requirement/oracle traceability, and the assumptions needed to reproduce each scenario. Confirm each student's required contributions during team review; the totals above do not establish authorship.

The assignment overview asks for team-member review, while its detailed workflow asks for review from another team. Request teammate review and obtain the cross-team review as well unless the instructor clarifies that teammate review satisfies the requirement. Keep the PR unmerged until the required review is complete.

AI assistance was used to draft and check this document. Team members should verify the sources and revise the proposed requirements and oracles before accepting the submission.

[S1]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/README.rst#L23-L57
[S2]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/interfaces/http.rst#L9-L65
[S3]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/sql/statements/insert.rst#L144-L205
[S4]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/column-policy.rst#L7-L109
[S5]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/dql/fulltext.rst#L29-L64
[S6]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/fulltext-indices.rst#L83-L105
[S7]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/analyzers.rst#L53-L66
[S8]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/sql/statements/refresh.rst#L27-L98
[S9]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/server/src/main/java/org/elasticsearch/action/admin/indices/refresh/RefreshRequest.java#L30-L38
[S10]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/replication.rst#L95-L170
[S11]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/concepts/clustering.rst#L129-L162
[S12]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/general/ddl/sharding.rst#L13-L36
[S13]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/concepts/storage-consistency.rst#L82-L99
[S14]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/sql/statements/create-table.rst#L747-L772
[S15]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/server/src/main/java/org/elasticsearch/index/translog/Translog.java#L1451-L1462
[S16]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/sql/statements/create-table.rst#L421-L452
[S17]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/admin/system-information.rst#L927-L1070
[S18]: https://github.com/kobster68/crate/blob/30ba40f4b0a61502c190cde0e3cfac33b3f388c7/docs/admin/system-information.rst#L1936-L2015
