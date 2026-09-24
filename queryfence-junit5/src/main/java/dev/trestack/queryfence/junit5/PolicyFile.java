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

import dev.trestack.queryfence.core.Policy;
import dev.trestack.queryfence.junit5.internal.PolicyYaml;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/** Loads a {@code queryfence.yml} policy from the classpath. */
public final class PolicyFile {

  /** Where the policy is read from unless another resource is named. */
  public static final String DEFAULT_RESOURCE = "queryfence.yml";

  private PolicyFile() {}

  /**
   * Reads {@value #DEFAULT_RESOURCE} from the classpath.
   *
   * @return the policy it declares
   * @throws IllegalStateException when the resource is missing or the policy is invalid
   */
  public static Policy fromClasspath() {
    return fromClasspath(DEFAULT_RESOURCE);
  }

  /**
   * Reads a policy from the classpath.
   *
   * @param resource the resource path, with or without a leading slash
   * @return the policy it declares
   * @throws IllegalStateException when the resource is missing or the policy is invalid
   */
  public static Policy fromClasspath(String resource) {
    Objects.requireNonNull(resource, "resource");
    String path = resource.startsWith("/") ? resource : "/" + resource;
    try (InputStream in = PolicyFile.class.getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalStateException(
            "QueryFence policy " + resource + " was not found on the classpath");
      }
      return new PolicyYaml(resource).read(in);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read QueryFence policy " + resource, e);
    }
  }
}
