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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import dev.trestack.queryfence.core.Corpus.Case;
import dev.trestack.queryfence.core.TenantPredicates.Mutation;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Metamorphic relation: a statement that passes must stop passing as soon as any one of its tenant
 * predicates (or its INSERT tenant column) is removed. A passing case that survives the removal is
 * either a hole in the engine or a case that never protected anything.
 */
class MetamorphicTest {

  private static final String COLUMN = "tenant_id";

  static Stream<Arguments> passingCases() {
    return Corpus.cases().stream()
        .filter(c -> c.expect().isEmpty())
        .map(c -> Arguments.of(Named.of(c.id(), c)));
  }

  static Stream<Arguments> allCases() {
    return Corpus.cases().stream().map(c -> Arguments.of(Named.of(c.id(), c)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("passingCases")
  void removingAnyTenantPredicateProducesAViolation(Case c) {
    List<Mutation> mutations = TenantPredicates.removeOneByOne(c.sql(), COLUMN);
    assumeFalse(mutations.isEmpty(), "case has no tenant predicate to remove");

    SqlChecker checker = GoldenCorpusTest.checker(c);
    for (Mutation mutation : mutations) {
      assertThat(checker.check(mutation.sql()))
          .as(
              "%s: removing %s must be caught%n  before: %s%n  after : %s",
              c.id(), mutation.description(), c.sql(), mutation.sql())
          .isNotEmpty();
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("allCases")
  void mutatedStatementsNeverMakeTheEngineThrow(Case c) {
    SqlChecker checker = GoldenCorpusTest.checker(c);
    for (Mutation mutation : TenantPredicates.removeOneByOne(c.sql(), COLUMN)) {
      assertThatCode(() -> checker.check(mutation.sql()))
          .as("%s: %s", c.id(), mutation.sql())
          .doesNotThrowAnyException();
    }
  }

  @Test
  void mostPassingCasesActuallyCarryATenantPredicate() {
    List<Case> passing = Corpus.cases().stream().filter(c -> c.expect().isEmpty()).toList();
    List<String> withoutPredicate =
        passing.stream()
            .filter(c -> TenantPredicates.removeOneByOne(c.sql(), COLUMN).isEmpty())
            .map(Case::id)
            .toList();

    // Statements that pass without any tenant predicate exist on purpose (unprotected tables,
    // ignored statements, CTE shadowing) but they must stay a small minority of the corpus.
    assertThat(withoutPredicate)
        .as("passing cases with no tenant predicate at all")
        .hasSizeLessThan(passing.size() / 4);
  }
}
