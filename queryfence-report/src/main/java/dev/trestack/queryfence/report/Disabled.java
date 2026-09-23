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
package dev.trestack.queryfence.report;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Says out loud that QueryFence is switched off. Turning the checks off is an emergency exit, and
 * an emergency exit nobody notices is how a project ends up unprotected for months, so the warning
 * is loud and printed once per reason.
 */
public final class Disabled {

  /** The property that switches QueryFence off. */
  public static final String PROPERTY = "queryfence.enabled";

  private static final ConcurrentMap<String, Boolean> announced = new ConcurrentHashMap<>();

  private Disabled() {}

  /** Prints the warning for this context, once. */
  public static void announce(String where) {
    if (announced.putIfAbsent(where, Boolean.TRUE) != null) {
      return;
    }
    String line = "=".repeat(78);
    System.err.println(
        "\n"
            + line
            + "\nQueryFence is disabled ("
            + PROPERTY
            + "=false) for "
            + where
            + "."
            + "\nNo SQL policy is checked in this run: tenant leaks will not fail the build."
            + "\nThis is an emergency exit. To accept a single known query instead, add a"
            + "\nsuppression with a reason to the policy file.\n"
            + line
            + "\n");
  }

  /** For QueryFence's own tests. */
  public static void reset() {
    announced.clear();
  }
}
