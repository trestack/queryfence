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
import dev.trestack.queryfence.jdbc.internal.CapturingListener;
import dev.trestack.queryfence.jdbc.internal.DelegatingFencedDataSource;
import dev.trestack.queryfence.jdbc.internal.OriginResolver;
import java.util.Objects;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

/**
 * Wraps a {@code DataSource} so that every statement executed through it is recorded and checked
 * against a {@link Policy}.
 *
 * <pre>{@code
 * FencedDataSource fenced = QueryFence.wrap(dataSource, policy);
 * // ... run the code under test against fenced ...
 * List<FoundViolation> violations = fenced.recorder().violations();
 * }</pre>
 *
 * <p>The SQL reaches the driver unchanged; QueryFence never rewrites or blocks a statement.
 */
public final class QueryFence {

  private QueryFence() {}

  /** Wraps {@code dataSource}, resolving origins by skipping infrastructure frames. */
  public static FencedDataSource wrap(DataSource dataSource, Policy policy) {
    return wrap(dataSource, policy, CaptureSettings.defaults());
  }

  /** Wraps {@code dataSource} with explicit origin resolution settings. */
  public static FencedDataSource wrap(
      DataSource dataSource, Policy policy, CaptureSettings settings) {
    Objects.requireNonNull(dataSource, "dataSource");
    Objects.requireNonNull(policy, "policy");
    Objects.requireNonNull(settings, "settings");

    QueryRecorder recorder = new QueryRecorder(policy);
    DataSource proxy =
        ProxyDataSourceBuilder.create(dataSource)
            .name("queryfence")
            .listener(new CapturingListener(recorder::record, new OriginResolver(settings)))
            .build();
    return new DelegatingFencedDataSource(proxy, dataSource, recorder);
  }
}
