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
package dev.trestack.queryfence.junit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import com.acme.orders.OrderRepository;
import dev.trestack.queryfence.report.Disabled;
import dev.trestack.queryfence.report.RunReport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

/**
 * Runs test classes that use the extension inside JUnit's test kit, so the failures they produce
 * can be asserted instead of failing this build.
 */
class ExtensionBehaviourTest {

  @TempDir Path reportDirectory;

  @BeforeEach
  void resetReport() {
    RunReport.instance().reset();
    RunReport.instance().reportFile(reportDirectory.resolve("report.json"));
  }

  @Test
  void failsTheTestAndPointsAtTheLineOfTheLeakingQuery() {
    Events tests = run(LeakingCase.class);

    tests.assertStatistics(stats -> stats.started(1).failed(1));
    assertThat(failureMessage(tests))
        .contains("QueryFence: 1 violation in " + LeakingCase.class.getName() + "#listsOrders")
        .contains("MISSING_PREDICATE")
        .contains("purchase_order")
        .contains("Add \"purchase_order.tenant_id = ?\"")
        .contains("SELECT id FROM purchase_order WHERE status = ?")
        .contains(
            "com.acme.orders.OrderRepository#findByStatus (OrderRepository.java:"
                + OrderRepository.lastLine
                + ")");
  }

  @Test
  void passesWhenEveryStatementIsFenced() {
    run(FencedCase.class).assertStatistics(stats -> stats.started(1).succeeded(1));
  }

  @Test
  void ignoresStatementsExecutedOutsideTheTestMethod() {
    run(LeakInBeforeEachCase.class).assertStatistics(stats -> stats.started(1).succeeded(1));
  }

  @Test
  void reportsWithoutFailingInReportMode() {
    run(ReportModeCase.class).assertStatistics(stats -> stats.started(1).succeeded(1));

    assertThat(RunReport.instance().findings()).hasSize(1);
  }

  @Test
  void groupsTheReportByPolicy() {
    run(LeakingCase.class);
    run(ReportModeCase.class);

    assertThat(RunReport.instance().results())
        .extracting(results -> results.policy() + "/" + results.mode())
        .containsExactly("queryfence.yml/FAIL", "queryfence-report-mode.yml/REPORT");
    assertThat(RunReport.instance().summary())
        .contains("QueryFence [queryfence.yml, mode FAIL]: 1 violation")
        .contains("QueryFence [queryfence-report-mode.yml, mode REPORT]: 1 violation");
  }

  @Test
  void canBeSwitchedOffWithALoudWarning() {
    System.setProperty("queryfence.enabled", "false");
    Disabled.reset();
    try {
      run(LeakingCase.class).assertStatistics(stats -> stats.started(1).succeeded(1));
      assertThat(RunReport.instance().findings()).isEmpty();
    } finally {
      System.clearProperty("queryfence.enabled");
      Disabled.reset();
    }
  }

  @Test
  void appliesSuppressionsFromThePolicyFile() {
    run(SuppressedCase.class).assertStatistics(stats -> stats.started(1).succeeded(1));

    assertThat(RunReport.instance().findings()).isEmpty();
  }

  @Test
  void writesTheJsonReportWithOneEntryPerFinding() throws Exception {
    run(LeakingCase.class);
    RunReport.instance().flush();

    String json = Files.readString(reportDirectory.resolve("report.json"));
    assertThat(json)
        .contains("\"policy\":\"queryfence.yml\"")
        .contains("\"mode\":\"FAIL\"")
        .contains("\"tests\":1")
        .contains("\"findings\":1")
        .contains("\"code\":\"MISSING_PREDICATE\"")
        .contains("\"table\":\"purchase_order\"")
        .contains("\"method\":\"findByStatus\"")
        .contains("\"line\":" + OrderRepository.lastLine)
        .contains("\"test\":\"" + LeakingCase.class.getName() + "#listsOrders\"");
  }

  @Test
  void countsEveryCheckedStatementInTheSummary() {
    run(FencedCase.class);

    assertThat(RunReport.instance().summary())
        .contains("QueryFence [queryfence.yml, mode FAIL]: 0 violations in 1 test")
        .contains("1 statements checked");
  }

  private static Events run(Class<?> testClass) {
    return EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(testClass))
        .execute()
        .testEvents();
  }

  private static String failureMessage(Events tests) {
    Optional<Throwable> failure =
        tests.failed().stream()
            .findFirst()
            .flatMap(event -> event.getPayload(org.junit.platform.engine.TestExecutionResult.class))
            .flatMap(org.junit.platform.engine.TestExecutionResult::getThrowable);
    assertThat(failure).isPresent();
    return failure.orElseThrow().getMessage();
  }

  // ------------------------------------------------------------------ cases run by the test kit

  /** Executes a query that forgets the tenant filter. */
  static class LeakingCase {

    @RegisterExtension static final QueryFenceExtension fence = QueryFenceExtension.fromClasspath();

    @Test
    void listsOrders() {
      repository(fence).findByStatus(1L, "OPEN");
    }
  }

  /** Executes only fenced queries. */
  static class FencedCase {

    @RegisterExtension static final QueryFenceExtension fence = QueryFenceExtension.fromClasspath();

    @Test
    void listsOrders() {
      List<?> orders = repository(fence).findByTenantAndStatus(1L, "OPEN");
      assertThat(orders).isEmpty();
    }
  }

  /** Leaks in a fixture, which is outside the capture window. */
  static class LeakInBeforeEachCase {

    @RegisterExtension static final QueryFenceExtension fence = QueryFenceExtension.fromClasspath();

    @BeforeEach
    void seed() {
      repository(fence).findByStatus(1L, "OPEN");
    }

    @Test
    void listsOrders() {
      repository(fence).findByTenantAndStatus(1L, "OPEN");
    }
  }

  /** Same leak, but the policy only reports. */
  static class ReportModeCase {

    @RegisterExtension
    static final QueryFenceExtension fence =
        QueryFenceExtension.fromClasspath("queryfence-report-mode.yml");

    @Test
    void listsOrders() {
      repository(fence).findByStatus(1L, "OPEN");
    }
  }

  /** Same leak, but the policy suppresses that method with a reason. */
  static class SuppressedCase {

    @RegisterExtension
    static final QueryFenceExtension fence =
        QueryFenceExtension.fromClasspath("queryfence-suppressed.yml");

    @Test
    void listsOrders() {
      repository(fence).findByStatus(1L, "OPEN");
    }
  }

  private static OrderRepository repository(QueryFenceExtension fence) {
    DataSource dataSource = fence.wrap(TestDatabase.create());
    return new OrderRepository(dataSource);
  }
}
