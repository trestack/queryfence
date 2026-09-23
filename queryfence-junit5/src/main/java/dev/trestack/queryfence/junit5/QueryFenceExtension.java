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

import dev.trestack.queryfence.core.Mode;
import dev.trestack.queryfence.core.Policy;
import dev.trestack.queryfence.jdbc.CaptureSettings;
import dev.trestack.queryfence.jdbc.FencedDataSource;
import dev.trestack.queryfence.jdbc.QueryFence;
import dev.trestack.queryfence.jdbc.QueryRecorder.Finding;
import dev.trestack.queryfence.junit5.internal.Findings;
import dev.trestack.queryfence.junit5.internal.RunReport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Checks the SQL a test executes against a {@link Policy}.
 *
 * <pre>{@code
 * class OrderRepositoryTest {
 *
 *   @RegisterExtension
 *   static final QueryFenceExtension queryFence = QueryFenceExtension.fromClasspath();
 *
 *   DataSource dataSource = queryFence.wrap(TestDatabase.dataSource());
 * }
 * }</pre>
 *
 * <p>Only the statements of the test method itself — and of everything it calls, on any thread —
 * are checked. Fixtures in {@code @BeforeEach}/{@code @AfterEach}, context startup and migrations
 * run outside that window.
 */
public final class QueryFenceExtension
    implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

  private final Policy policy;
  private final CaptureSettings captureSettings;
  private final List<FencedDataSource> dataSources = new CopyOnWriteArrayList<>();

  private QueryFenceExtension(Policy policy, CaptureSettings captureSettings) {
    this.policy = Objects.requireNonNull(policy, "policy");
    this.captureSettings = Objects.requireNonNull(captureSettings, "captureSettings");
    RunReport.instance().registerShutdownHook();
  }

  /** Loads {@value PolicyFile#DEFAULT_RESOURCE} from the classpath. */
  public static QueryFenceExtension fromClasspath() {
    return of(PolicyFile.fromClasspath());
  }

  /** Loads a policy from the classpath. */
  public static QueryFenceExtension fromClasspath(String resource) {
    return of(PolicyFile.fromClasspath(resource));
  }

  /** Uses a policy built in Java. */
  public static QueryFenceExtension of(Policy policy) {
    return of(policy, CaptureSettings.defaults());
  }

  /** Uses a policy built in Java, with explicit origin resolution settings. */
  public static QueryFenceExtension of(Policy policy, CaptureSettings captureSettings) {
    return new QueryFenceExtension(policy, captureSettings);
  }

  /** Wraps a data source so the statements the test executes through it are checked. */
  public DataSource wrap(DataSource dataSource) {
    FencedDataSource fenced = QueryFence.wrap(dataSource, policy, captureSettings);
    dataSources.add(fenced);
    return fenced;
  }

  public Policy policy() {
    return policy;
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    dataSources.forEach(dataSource -> dataSource.recorder().clear());
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    List<Finding> findings = new ArrayList<>();
    int statements = 0;
    for (FencedDataSource dataSource : dataSources) {
      findings.addAll(dataSource.recorder().findings());
      statements += dataSource.recorder().statements().size();
      dataSource.recorder().clear();
    }
    String test = testId(context);
    RunReport.instance().add(test, policy.mode(), findings, statements);

    if (!findings.isEmpty() && policy.mode() == Mode.FAIL) {
      throw new AssertionError(
          Findings.failureMessage(findings, test, RunReport.instance().reportFile().toString()));
    }
  }

  private static String testId(ExtensionContext context) {
    return context
        .getTestClass()
        .map(
            testClass ->
                testClass.getName()
                    + context.getTestMethod().map(method -> "#" + method.getName()).orElse(""))
        .orElseGet(context::getDisplayName);
  }
}
