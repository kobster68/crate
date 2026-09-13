# Requirements and Test Oracles

## Scope

create/docs/general/builtins/selects.rst
create/docs/general/builtins/aggregation.rst

## Functional Requirements
| Requirement | Source |
|---|---|
| Selecting * shall return all the columns | selects.rst, line 28 |
| A column alias shall change the column name for output | selects.rst, line 40 |
| DISTINCT shall remove duplicate output rows | selects.rst, line 117 |
| An equality condition in WHERE shall restrict results to matching rows | selects.rst, line 140 |
| COUNT(column) shall count only non-null values in that column | aggregation.rst, line 258 |
| COUNT(DISTINCT column) shall count distinct, non-null values | aggregation.rst, line 281 |
| COUNT(*) shall return the number of matching rows | aggregation.rst, line 321 |

## Non-Functional Requirements
todo

## Test Oracles
todo