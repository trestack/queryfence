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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Runs every case of the golden corpus ({@code src/test/resources/corpus}) through the rule engine.
 * See the corpus README for the file format.
 */
class GoldenCorpusTest {

  private static final String POLICIES = "policies.yml";

  record Expected(String rule, String code, String table, String alias, String message) {}

  record Case(
      String id,
      String file,
      String sql,
      String policy,
      List<Expected> expect,
      String reason,
      List<String> clause) {}

  static Stream<Arguments> cases() throws IOException, URISyntaxException {
    Path dir = Path.of(GoldenCorpusTest.class.getResource("/corpus").toURI());
    List<Case> cases = new ArrayList<>();
    try (Stream<Path> files = Files.list(dir)) {
      for (Path file :
          files
              .filter(p -> p.getFileName().toString().endsWith(".yml"))
              .filter(p -> !p.getFileName().toString().equals(POLICIES))
              .sorted()
              .toList()) {
        cases.addAll(readCases(file));
      }
    }
    return cases.stream().map(c -> Arguments.of(Named.of(c.id(), c)));
  }

  private static final Map<String, Policy> POLICY_CACHE = new HashMap<>();

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  void corpusCase(Case c) throws IOException {
    SqlChecker checker = SqlChecker.of(policy(c.policy()));

    List<Expected> actual =
        checker.check(c.sql()).stream()
            .map(v -> new Expected(v.ruleId(), v.code().name(), v.table(), v.alias(), v.message()))
            .toList();

    assertThat(actual)
        .as("%s (%s, %s): %s", c.id(), c.file(), c.clause(), c.reason())
        .containsExactlyInAnyOrderElementsOf(c.expect());
  }

  private static synchronized Policy policy(String name) throws IOException {
    if (POLICY_CACHE.isEmpty()) {
      Map<String, Object> all = load("/corpus/" + POLICIES);
      for (Map.Entry<String, Object> entry : all.entrySet()) {
        POLICY_CACHE.put(entry.getKey(), toPolicy(asMap(entry.getValue())));
      }
    }
    Policy policy = POLICY_CACHE.get(name);
    if (policy == null) {
      throw new IllegalArgumentException("Unknown corpus policy '" + name + "'");
    }
    return policy;
  }

  /** Minimal mapping of the policy schema; the real YAML loader lives in queryfence-junit5. */
  private static Policy toPolicy(Map<String, Object> map) {
    Policy.Builder builder = Policy.builder();
    builder.mode(Mode.valueOf((String) map.getOrDefault("mode", "FAIL")));
    builder.onUnparseable(Mode.valueOf((String) map.getOrDefault("onUnparseable", "FAIL")));
    for (Object o : (List<?>) map.get("rules")) {
      Map<String, Object> rule = asMap(o);
      String id = (String) rule.get("id");
      switch ((String) rule.get("type")) {
        case "require-predicate" ->
            builder.requirePredicate(
                id,
                (String) rule.get("column"),
                strings(rule.get("tables")),
                strings(rule.getOrDefault("allowedFunctions", List.of())));
        case "update-without-where" -> builder.updateWithoutWhere(id);
        case "delete-without-where" -> builder.deleteWithoutWhere(id);
        default -> throw new IllegalArgumentException("Unknown rule type in " + rule);
      }
    }
    return builder.build();
  }

  private static List<Case> readCases(Path file) throws IOException {
    String name = file.getFileName().toString();
    Map<String, Object> root = load("/corpus/" + name);
    List<Case> cases = new ArrayList<>();
    for (Object o : (List<?>) root.get("cases")) {
      Map<String, Object> c = asMap(o);
      List<Expected> expect = new ArrayList<>();
      for (Object e : (List<?>) c.get("expect")) {
        Map<String, Object> m = asMap(e);
        expect.add(
            new Expected(
                (String) m.get("rule"),
                (String) m.get("code"),
                (String) m.get("table"),
                (String) m.get("alias"),
                (String) m.get("message")));
      }
      cases.add(
          new Case(
              (String) c.get("id"),
              name,
              (String) c.get("sql"),
              (String) c.get("policy"),
              expect,
              (String) c.get("reason"),
              strings(c.get("clause"))));
    }
    return cases;
  }

  private static Map<String, Object> load(String resource) throws IOException {
    try (InputStream in = GoldenCorpusTest.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Missing corpus resource " + resource);
      }
      return new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object o) {
    return (Map<String, Object>) o;
  }

  private static List<String> strings(Object o) {
    return ((List<?>) o).stream().map(String::valueOf).toList();
  }
}
