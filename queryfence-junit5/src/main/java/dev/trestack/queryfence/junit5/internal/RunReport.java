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
package dev.trestack.queryfence.junit5.internal;

import dev.trestack.queryfence.core.Mode;
import dev.trestack.queryfence.jdbc.QueryRecorder.Finding;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects what every test found and, once the JVM running the tests ends, prints a summary and
 * writes {@code target/queryfence/report.json}.
 *
 * <p>The flush is a shutdown hook rather than a JUnit callback on purpose: it behaves the same on
 * every JUnit 5 and 6 version, and it also runs when the build stops after a failure.
 */
public final class RunReport {

  /** One finding, with the test that executed the statement. */
  public record ReportedFinding(String test, Finding finding) {}

  private static final RunReport INSTANCE = new RunReport();

  private final List<ReportedFinding> findings = new ArrayList<>();
  private final Set<String> tests = new LinkedHashSet<>();

  /** Every mode used in this JVM; more than one when a run mixes policies. */
  private final Set<Mode> modes = new LinkedHashSet<>();

  private final AtomicInteger statements = new AtomicInteger();
  private final AtomicBoolean hookRegistered = new AtomicBoolean();
  private volatile Path reportFile = Path.of("target", "queryfence", "report.json");

  private RunReport() {}

  public static RunReport instance() {
    return INSTANCE;
  }

  /** Makes sure the report is flushed when the test JVM ends. */
  public void registerShutdownHook() {
    if (hookRegistered.compareAndSet(false, true)) {
      Runtime.getRuntime().addShutdownHook(new Thread(this::flush, "queryfence-report"));
    }
  }

  /** Where the report is written; {@code target/queryfence/report.json} by default. */
  public Path reportFile() {
    return reportFile;
  }

  /** For tests: writes the report somewhere else. */
  public void reportFile(Path reportFile) {
    this.reportFile = reportFile;
  }

  public synchronized void add(
      String test, Mode mode, List<Finding> testFindings, int executedStatements) {
    tests.add(test);
    modes.add(mode);
    statements.addAndGet(executedStatements);
    for (Finding finding : testFindings) {
      findings.add(new ReportedFinding(test, finding));
    }
  }

  public synchronized List<ReportedFinding> findings() {
    return List.copyOf(findings);
  }

  /** Prints the suite summary and writes the JSON report. */
  public synchronized void flush() {
    if (tests.isEmpty()) {
      return;
    }
    System.out.println(summary());
    write();
  }

  public synchronized String summary() {
    StringBuilder sb = new StringBuilder();
    sb.append("\nQueryFence: ")
        .append(findings.size())
        .append(findings.size() == 1 ? " violation in " : " violations in ")
        .append(tests.size())
        .append(tests.size() == 1 ? " test" : " tests")
        .append(" (")
        .append(statements.get())
        .append(" statements checked, ")
        .append(modes())
        .append(")");
    if (!findings.isEmpty()) {
      for (ReportedFinding reported : findings) {
        sb.append("\n\n").append(Findings.format(reported.finding(), reported.test()));
      }
      sb.append("\n\nFull report: ").append(reportFile);
    }
    return sb.toString();
  }

  private void write() {
    try {
      Path parent = reportFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(reportFile, json(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write the QueryFence report to " + reportFile, e);
    }
  }

  /** {@code mode FAIL}, or {@code modes FAIL, REPORT} when a run mixes policies. */
  private String modes() {
    List<String> names = modes.stream().map(Mode::name).toList();
    return (names.size() == 1 ? "mode " : "modes ") + String.join(", ", names);
  }

  public synchronized String json() {
    Json json = new Json();
    json.object();
    json.key("modes").array();
    modes.forEach(m -> json.value(m.name()));
    json.end();
    json.field("generatedAt", Instant.now().toString());
    json.key("summary").object();
    json.field("tests", tests.size());
    json.field("statements", statements.get());
    json.field("findings", findings.size());
    json.end();
    json.key("findings").array();
    for (ReportedFinding reported : findings) {
      var violation = reported.finding().violation();
      var statement = reported.finding().statement();
      var origin = statement.origin();
      json.object();
      json.field("test", reported.test());
      json.field("rule", violation.ruleId());
      json.field("type", violation.ruleType());
      json.field("code", violation.code().name());
      json.field("table", violation.table());
      json.field("alias", violation.alias());
      json.field("message", violation.message());
      json.field("sql", statement.sql());
      json.field("batch", statement.batch());
      json.key("origin").object();
      json.field("class", origin.className());
      json.field("method", origin.methodName());
      json.field("file", origin.fileName());
      json.field("line", origin.lineNumber());
      json.end();
      json.end();
    }
    json.end();
    json.end();
    return json.toString();
  }

  /** For tests: forget everything recorded so far. */
  public synchronized void reset() {
    findings.clear();
    tests.clear();
    modes.clear();
    statements.set(0);
  }
}
