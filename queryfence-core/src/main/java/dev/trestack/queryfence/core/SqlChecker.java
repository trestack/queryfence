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

import dev.trestack.queryfence.core.internal.DefaultSqlChecker;
import java.util.List;

/**
 * Checks SQL statements against a {@link Policy}. Implementations are thread-safe and cache parse
 * results by SQL string.
 */
public interface SqlChecker {

  /** Creates a checker for the given policy. */
  static SqlChecker of(Policy policy) {
    return new DefaultSqlChecker(policy);
  }

  /**
   * Checks one SQL string, which may hold several statements separated by {@code ;}. Suppressions
   * are not applied here, because they depend on where the statement came from.
   *
   * @return the violations, empty when the SQL satisfies every rule
   */
  List<Violation> check(String sql);
}
