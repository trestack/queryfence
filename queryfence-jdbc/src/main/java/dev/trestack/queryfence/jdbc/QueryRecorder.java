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
package dev.trestack.queryfence.jdbc;

import dev.trestack.queryfence.core.Policy;
import dev.trestack.queryfence.core.SqlChecker;
import dev.trestack.queryfence.core.Suppression;
import dev.trestack.queryfence.core.Violation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects the statements executed through a wrapped {@code DataSource} and checks them against the
 * policy. Thread-safe: statements executed on other threads during a test are recorded too.
 */
public final class QueryRecorder {

  private final Policy policy;
  private final SqlChecker checker;
  private final List<CapturedStatement> statements = new CopyOnWriteArrayList<>();
  private volatile boolean recording = true;

  QueryRecorder(Policy policy) {
    this.policy = Objects.requireNonNull(policy, "policy");
    this.checker = SqlChecker.of(policy);
  }

  /** The statements recorded so far, oldest first. */
  public List<CapturedStatement> statements() {
    return List.copyOf(statements);
  }

  /** Forgets every recorded statement; the next test starts from an empty list. */
  public void clear() {
    statements.clear();
  }

  /** Stops recording without unwrapping the {@code DataSource}. */
  public void pause() {
    recording = false;
  }

  /** Resumes recording after {@link #pause()}. */
  public void resume() {
    recording = true;
  }

  public boolean isRecording() {
    return recording;
  }

  public Policy policy() {
    return policy;
  }

  /**
   * The violations of the recorded statements, with the origin that produced each one. Suppressions
   * of the policy are applied here, because they are keyed by origin.
   */
  public List<FoundViolation> violations() {
    List<FoundViolation> found = new ArrayList<>();
    for (CapturedStatement statement : statements) {
      for (Violation violation : checker.check(statement.sql())) {
        if (!isSuppressed(violation, statement.origin())) {
          found.add(new FoundViolation(violation, statement));
        }
      }
    }
    return List.copyOf(found);
  }

  private boolean isSuppressed(Violation violation, Origin origin) {
    if (!origin.isKnown()) {
      return false;
    }
    for (Suppression suppression : policy.suppressions()) {
      if (suppression.ruleId().equals(violation.ruleId())
          && suppression.origin().equals(origin.classAndMethod())) {
        return true;
      }
    }
    return false;
  }

  void record(CapturedStatement statement) {
    if (recording) {
      statements.add(statement);
    }
  }

  /** A violation together with the statement and the code that produced it. */
  public record FoundViolation(Violation violation, CapturedStatement statement) {

    public Origin origin() {
      return statement.origin();
    }

    @Override
    public String toString() {
      return "["
          + violation.ruleId()
          + "] "
          + violation.code()
          + "\n    "
          + violation.message()
          + "\n    sql    : "
          + statement.sql()
          + "\n    origin : "
          + statement.origin();
    }
  }
}
