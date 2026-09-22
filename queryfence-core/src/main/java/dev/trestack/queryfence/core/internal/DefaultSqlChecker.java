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

import dev.trestack.queryfence.core.Policy;
import dev.trestack.queryfence.core.Rule;
import dev.trestack.queryfence.core.SqlChecker;
import dev.trestack.queryfence.core.Violation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.sf.jsqlparser.statement.Statement;

/** Default {@link SqlChecker}: parses once per SQL string, then applies every rule. */
public final class DefaultSqlChecker implements SqlChecker {

  /** Shared by all checkers: parse results do not depend on the policy. */
  private static final ParseCache PARSE_CACHE = new ParseCache(10_000);

  private final Policy policy;

  public DefaultSqlChecker(Policy policy) {
    this.policy = Objects.requireNonNull(policy, "policy");
  }

  @Override
  public List<Violation> check(String sql) {
    Objects.requireNonNull(sql, "sql");
    ParseCache.Parsed parsed = PARSE_CACHE.get(sql);
    if (parsed.failed()) {
      return List.of(
          new Violation(
              null, null, Violation.Code.UNPARSEABLE, null, null, Messages.unparseable(), sql));
    }
    List<Statement> statements = parsed.statements();
    List<Violation> violations = new ArrayList<>();
    for (Statement statement : statements) {
      String statementSql = statements.size() == 1 ? sql : statement.toString();
      for (Rule rule : policy.rules()) {
        if (rule instanceof RequirePredicateRule requirePredicate) {
          violations.addAll(
              new RequirePredicateAnalyzer(requirePredicate, statementSql).analyze(statement));
        } else if (rule instanceof UnboundedWriteRule unboundedWrite) {
          violations.addAll(UnboundedWriteCheck.check(unboundedWrite, statement, statementSql));
        }
      }
    }
    return List.copyOf(violations);
  }
}
