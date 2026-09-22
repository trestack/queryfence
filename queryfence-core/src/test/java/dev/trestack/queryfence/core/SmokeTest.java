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

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.jupiter.api.Test;

/** Verifies the toolchain works end to end. Delete once real tests exist. */
class SmokeTest {

  @Test
  void jsqlParserIsOnTheClasspath() throws Exception {
    Statement statement =
        CCJSqlParserUtil.parse("SELECT * FROM purchase_order WHERE tenant_id = ?");
    assertThat(statement).isInstanceOf(Select.class);
  }
}
