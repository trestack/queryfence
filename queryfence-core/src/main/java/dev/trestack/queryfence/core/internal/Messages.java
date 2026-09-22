/*
 * Copyright 2026 the QueryFence authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.trestack.queryfence.core.internal;

/**
 * Violation messages. Each states the problem and the fix; the templates are specified in DESIGN.md
 * "Violation model" and asserted verbatim by the golden corpus.
 */
final class Messages {

  private Messages() {}

  static String missingPredicate(String table, String alias, String column) {
    return display(table, alias)
        + " has no tenant filter. Add \""
        + ref(table, alias)
        + "."
        + column
        + " = ?\" to the WHERE clause of the query block that uses it.";
  }

  static String ambiguousColumn(String table, String alias, String column) {
    return "Column \""
        + column
        + "\" is not qualified in a query block that uses several tables, so it protects none of"
        + " them. Qualify it with the table alias, for example \""
        + ref(table, alias)
        + "."
        + column
        + " = ?\".";
  }

  static String missingInsertColumn(String table, String column) {
    return "INSERT into "
        + table
        + " does not set "
        + column
        + ". Add "
        + column
        + " to the column list and bind the current tenant.";
  }

  static String noWhere(UnboundedWriteRule.Kind kind, String table, String alias) {
    return prefix(kind, table, alias)
        + " has no WHERE clause and "
        + verb(kind)
        + " every row. Add a WHERE clause that selects only the intended rows.";
  }

  static String tautologicalWhere(UnboundedWriteRule.Kind kind, String table, String alias) {
    return prefix(kind, table, alias)
        + " has a WHERE clause that is always true and "
        + verb(kind)
        + " every row. Replace it with a condition that selects only the intended rows.";
  }

  static String unsupportedStatement(String statement, String table, String alias) {
    return statement
        + " statements on "
        + display(table, alias)
        + " are not analysed yet. Rewrite the statement as INSERT or UPDATE, or suppress its origin"
        + " with a reason.";
  }

  static String unparseable() {
    return "QueryFence could not parse this statement, so it cannot prove it safe. Report the SQL"
        + " to QueryFence, or set onUnparseable: REPORT to only report it.";
  }

  private static String prefix(UnboundedWriteRule.Kind kind, String table, String alias) {
    return (kind == UnboundedWriteRule.Kind.UPDATE ? "UPDATE of " : "DELETE from ")
        + display(table, alias);
  }

  private static String verb(UnboundedWriteRule.Kind kind) {
    return kind == UnboundedWriteRule.Kind.UPDATE ? "changes" : "removes";
  }

  private static String display(String table, String alias) {
    return alias == null ? table : table + " (" + alias + ")";
  }

  private static String ref(String table, String alias) {
    return alias == null ? table : alias;
  }
}
