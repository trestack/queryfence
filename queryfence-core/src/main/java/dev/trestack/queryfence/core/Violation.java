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
package dev.trestack.queryfence.core;

/**
 * One statement that breaks one rule.
 *
 * @param ruleId the id of the broken rule, or {@code null} for {@link Code#UNPARSEABLE}
 * @param ruleType the type of the broken rule, or {@code null} for {@link Code#UNPARSEABLE}
 * @param code what kind of problem was found
 * @param table the normalized table name (lower case, unquoted, without schema), or {@code null}
 * @param alias the table alias as written in the SQL, or {@code null} when there is none
 * @param message what is wrong and how to fix it
 * @param sql the statement as it was checked
 */
public record Violation(
    String ruleId,
    String ruleType,
    Code code,
    String table,
    String alias,
    String message,
    String sql) {

  /** Violation codes, see {@code docs/DESIGN.md} "Violation model". */
  public enum Code {
    MISSING_PREDICATE,
    AMBIGUOUS_COLUMN,
    MISSING_INSERT_COLUMN,
    NO_WHERE,
    TAUTOLOGICAL_WHERE,
    UNSUPPORTED_STATEMENT,
    UNPARSEABLE
  }
}
