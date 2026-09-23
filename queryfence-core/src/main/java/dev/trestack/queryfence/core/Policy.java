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

import dev.trestack.queryfence.core.internal.RequirePredicateRule;
import dev.trestack.queryfence.core.internal.UnboundedWriteRule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The rules QueryFence checks, with their suppressions. Immutable; create one with {@link
 * #builder()}.
 *
 * <pre>{@code
 * Policy policy = Policy.builder()
 *     .requirePredicate("tenant-isolation", "tenant_id", "purchase_order", "order_item")
 *     .updateWithoutWhere("no-unbounded-update")
 *     .deleteWithoutWhere("no-unbounded-delete")
 *     .build();
 * }</pre>
 */
public final class Policy {

  private final List<Rule> rules;
  private final List<Suppression> suppressions;
  private final Mode mode;
  private final Mode onUnparseable;

  private Policy(Builder builder) {
    this.rules = List.copyOf(builder.rules);
    this.suppressions = List.copyOf(builder.suppressions);
    this.mode = builder.mode;
    this.onUnparseable = builder.onUnparseable;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<Rule> rules() {
    return rules;
  }

  public List<Suppression> suppressions() {
    return suppressions;
  }

  /** Whether violations fail the test ({@link Mode#FAIL}, the default) or are only reported. */
  public Mode mode() {
    return mode;
  }

  /** How unparseable statements are treated; {@link Mode#FAIL} by default (fail closed). */
  public Mode onUnparseable() {
    return onUnparseable;
  }

  /** Builds a {@link Policy}. Invalid input fails fast with {@link IllegalArgumentException}. */
  public static final class Builder {

    private final List<Rule> rules = new ArrayList<>();
    private final List<Suppression> suppressions = new ArrayList<>();
    private Mode mode = Mode.FAIL;
    private Mode onUnparseable = Mode.FAIL;

    private Builder() {}

    /** Every occurrence of {@code tables} must be filtered by {@code column}. */
    public Builder requirePredicate(String id, String column, String... tables) {
      return requirePredicate(id, column, List.of(tables), List.of());
    }

    /**
     * Every occurrence of {@code tables} must be filtered by {@code column}; comparisons with the
     * functions in {@code allowedFunctions} (for example {@code current_setting}) are accepted as
     * values.
     */
    public Builder requirePredicate(
        String id, String column, Collection<String> tables, Collection<String> allowedFunctions) {
      return add(RequirePredicateRule.of(id, column, tables, allowedFunctions));
    }

    /**
     * Same, naming the column rows are looked up by when no tenant is involved ({@code id} by
     * default). A query that only filters by it is reported as {@code PRIMARY_KEY_LOOKUP}.
     */
    public Builder requirePredicate(
        String id,
        String column,
        Collection<String> tables,
        Collection<String> allowedFunctions,
        String primaryKey) {
      return add(RequirePredicateRule.of(id, column, tables, allowedFunctions, primaryKey));
    }

    /** Every {@code UPDATE} must have a WHERE clause that is not always true. */
    public Builder updateWithoutWhere(String id) {
      return add(new UnboundedWriteRule(id, UnboundedWriteRule.Kind.UPDATE));
    }

    /** Every {@code DELETE} must have a WHERE clause that is not always true. */
    public Builder deleteWithoutWhere(String id) {
      return add(new UnboundedWriteRule(id, UnboundedWriteRule.Kind.DELETE));
    }

    public Builder suppress(String ruleId, String origin, String reason) {
      suppressions.add(new Suppression(ruleId, origin, reason));
      return this;
    }

    public Builder mode(Mode mode) {
      this.mode = Objects.requireNonNull(mode, "mode");
      return this;
    }

    public Builder onUnparseable(Mode onUnparseable) {
      this.onUnparseable = Objects.requireNonNull(onUnparseable, "onUnparseable");
      return this;
    }

    public Policy build() {
      Set<String> ids = new HashSet<>();
      for (Rule rule : rules) {
        ids.add(rule.id());
      }
      for (Suppression suppression : suppressions) {
        if (!ids.contains(suppression.ruleId())) {
          throw new IllegalArgumentException(
              "Suppression for "
                  + suppression.origin()
                  + " refers to unknown rule '"
                  + suppression.ruleId()
                  + "'");
        }
      }
      return new Policy(this);
    }

    private Builder add(Rule rule) {
      for (Rule existing : rules) {
        if (existing.id().equals(rule.id())) {
          throw new IllegalArgumentException("Duplicate rule id '" + rule.id() + "'");
        }
      }
      rules.add(rule);
      return this;
    }
  }
}
