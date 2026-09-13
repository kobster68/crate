# Requirements and Test Oracles

## Scope

crate/docs/general/dql/selects.rst
crate/docs/general/builtins/aggregation.rst
crate/docs/general/ddl/replication.rst

## Functional Requirements
| Requirement | Source |
|---|---|
| Selecting * shall return all the columns | selects.rst, line 28 |
| A column alias shall change the column name for output | selects.rst, line 40 |
| DISTINCT shall remove duplicate rows | selects.rst, line 117 |
| An equality condition in WHERE shall only allow matching rows | selects.rst, line 140 |
| COUNT(column) shall count only non-null values| aggregation.rst, line 258 |
| COUNT(DISTINCT column) shall count distinct, non-null values | aggregation.rst, line 281 |
| COUNT(*) shall return the number of matching rows | aggregation.rst, line 321 |

## Non-Functional Requirements
| Requirement | Source |
|---|---|
| Fault tolerance: When a primary shard is lost, CrateDB shall promote a replica shard to primary shard. | replication.rst, line 18 |
| Recoverability: Following node loss, CrateDB shall attempt to restore the required replica count for affected shards. | replication.rst, line 102 |
| Fault isolation: CrateDB shall not make multiple of the same shard in a single node. | replication.rst, line 161 |

## Test Oracles
todo