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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.statement.upsert.Upsert;

/**
 * Removes, one at a time, the predicates and INSERT columns that protect a tenant column. Works on
 * the parsed statement, never on the SQL text, so it cannot be fooled by formatting.
 *
 * <p>A predicate is neutralized by turning it into a constant-true comparison, which keeps the
 * statement valid and its shape (AND/OR nesting, join conditions) intact.
 */
final class TenantPredicates {

  record Mutation(String description, String sql) {}

  private TenantPredicates() {}

  /**
   * One mutated statement per tenant predicate or INSERT column found in {@code sql}. SQL that does
   * not parse has nothing to remove, so it yields no mutation.
   */
  static List<Mutation> removeOneByOne(String sql, String column) {
    Statements parsed = parseOrNull(sql);
    if (parsed == null) {
      return List.of();
    }
    int count = new Walker(column, -1).walk(parsed);
    List<Mutation> mutations = new ArrayList<>();
    for (int target = 0; target < count; target++) {
      Walker walker = new Walker(column, target);
      Statements statements = parse(sql);
      walker.walk(statements);
      mutations.add(new Mutation(walker.description, join(statements)));
    }
    return mutations;
  }

  private static Statements parse(String sql) {
    Statements parsed = parseOrNull(sql);
    if (parsed == null) {
      throw new IllegalStateException("SQL stopped parsing between passes: " + sql);
    }
    return parsed;
  }

  private static Statements parseOrNull(String sql) {
    try {
      return CCJSqlParserUtil.parseStatements(sql);
    } catch (Exception e) {
      return null;
    }
  }

  private static String join(Statements statements) {
    StringBuilder sb = new StringBuilder();
    for (Statement statement : statements) {
      if (sb.length() > 0) {
        sb.append("; ");
      }
      sb.append(statement);
    }
    return sb.toString();
  }

  /** Counts tenant predicates, and neutralizes the one at index {@code target}. */
  private static final class Walker {

    private final String column;
    private final int target;
    private int index;
    private String description = "nothing";

    Walker(String column, int target) {
      this.column = column.toLowerCase(Locale.ROOT);
      this.target = target;
    }

    int walk(Statements statements) {
      for (Statement statement : statements) {
        statement(statement);
      }
      return index;
    }

    private void statement(Statement statement) {
      if (statement instanceof Select select) {
        select(select);
      } else if (statement instanceof Update update) {
        withItems(update.getWithItemsList());
        expression(update.getWhere());
        joins(update.getStartJoins());
        joins(update.getJoins());
        if (update.getUpdateSets() != null) {
          for (UpdateSet set : update.getUpdateSets()) {
            expressions(set.getValues());
          }
        }
      } else if (statement instanceof Delete delete) {
        withItems(delete.getWithItemsList());
        expression(delete.getWhere());
        joins(delete.getJoins());
      } else if (statement instanceof Insert insert) {
        withItems(insert.getWithItemsList());
        insertColumns(
            insert.getColumns(), valuesOf(insert.getSelect()), () -> insert.setColumns(null));
        updateSets(insert.getSetUpdateSets());
        if (insert.getSelect() != null) {
          select(insert.getSelect());
        }
      } else if (statement instanceof Upsert upsert) {
        insertColumns(
            upsert.getColumns(), valuesOf(upsert.getSelect()), () -> upsert.setColumns(null));
        updateSets(upsert.getUpdateSets());
        if (upsert.getSelect() != null) {
          select(upsert.getSelect());
        }
      }
    }

    /** The VALUES rows of an INSERT, or null when it is fed by a SELECT. */
    private static Values valuesOf(Select select) {
      return select instanceof Values values ? values : null;
    }

    private void select(Select select) {
      withItems(select.getWithItemsList());
      if (select instanceof PlainSelect plain) {
        if (plain.getSelectItems() != null) {
          for (SelectItem<?> item : plain.getSelectItems()) {
            expression(item.getExpression());
          }
        }
        fromItem(plain.getFromItem());
        joins(plain.getJoins());
        expression(plain.getWhere());
        expression(plain.getHaving());
      } else if (select instanceof SetOperationList list) {
        list.getSelects().forEach(this::select);
      } else if (select instanceof Values values) {
        expressions(values.getExpressions());
      } else if (select.getSelectBody() != null && select.getSelectBody() != select) {
        select(select.getSelectBody());
      }
    }

    private void withItems(List<WithItem<?>> withItems) {
      if (withItems != null) {
        for (WithItem<?> item : withItems) {
          if (item.getSelect() != null) {
            select(item.getSelect());
          }
        }
      }
    }

    private void joins(List<Join> joins) {
      if (joins == null) {
        return;
      }
      for (Join join : joins) {
        fromItem(join.getFromItem());
        if (join.getOnExpressions() != null) {
          join.getOnExpressions().forEach(this::expression);
        }
      }
    }

    private void fromItem(FromItem item) {
      if (item instanceof Select nested) {
        select(nested);
      }
    }

    private void expressions(ExpressionList<?> list) {
      if (list != null) {
        list.forEach(this::expression);
      }
    }

    private void updateSets(List<UpdateSet> sets) {
      if (sets == null) {
        return;
      }
      for (UpdateSet set : sets) {
        for (int i = 0; i < set.getColumns().size(); i++) {
          if (isTenantColumn(set.getColumn(i)) && hit("INSERT ... SET " + column)) {
            set.getColumns().remove(i);
            set.getValues().remove(i);
            return;
          }
        }
      }
    }

    private void insertColumns(
        ExpressionList<Column> columns, Values values, Runnable clearColumns) {
      if (columns == null) {
        return;
      }
      for (int i = 0; i < columns.size(); i++) {
        if (isTenantColumn(columns.get(i)) && hit("INSERT column " + column)) {
          columns.remove(i);
          if (columns.isEmpty()) {
            clearColumns.run();
          }
          removeValueAt(values, i);
          return;
        }
      }
    }

    private void removeValueAt(Values values, int position) {
      if (values == null || values.getExpressions() == null) {
        return;
      }
      ExpressionList<?> rows = values.getExpressions();
      if (rows instanceof ParenthesedExpressionList<?> singleRow) {
        singleRow.remove(position);
        return;
      }
      for (Expression row : rows) {
        if (row instanceof ParenthesedExpressionList<?> list) {
          list.remove(position);
        }
      }
    }

    private void expression(Expression expression) {
      if (expression == null) {
        return;
      }
      if (expression instanceof AndExpression and) {
        expression(and.getLeftExpression());
        expression(and.getRightExpression());
      } else if (expression instanceof OrExpression or) {
        expression(or.getLeftExpression());
        expression(or.getRightExpression());
      } else if (expression instanceof NotExpression not) {
        expression(not.getExpression());
      } else if (expression instanceof ExistsExpression exists) {
        expression(exists.getRightExpression());
      } else if (expression instanceof Select nested) {
        select(nested);
      } else if (expression instanceof InExpression in) {
        if (isTenantColumn(in.getLeftExpression()) && !in.isNot() && hit("IN predicate")) {
          in.setLeftExpression(new LongValue(1));
          in.setRightExpression(new ParenthesedExpressionList<>(new LongValue(1)));
          return;
        }
        expression(in.getLeftExpression());
        expression(in.getRightExpression());
      } else if (expression instanceof EqualsTo equals) {
        if (mentionsTenantColumn(equals) && hit("equality on " + column)) {
          equals.setLeftExpression(new LongValue(1));
          equals.setRightExpression(new LongValue(1));
          return;
        }
        comparison(equals);
      } else if (expression instanceof ComparisonOperator comparison) {
        comparison(comparison);
      } else if (expression instanceof ParenthesedExpressionList<?> list) {
        list.forEach(this::expression);
      }
    }

    private void comparison(ComparisonOperator comparison) {
      expression(comparison.getLeftExpression());
      expression(comparison.getRightExpression());
    }

    private boolean mentionsTenantColumn(ComparisonOperator comparison) {
      return isTenantColumn(comparison.getLeftExpression())
          || isTenantColumn(comparison.getRightExpression());
    }

    private boolean isTenantColumn(Expression expression) {
      return expression instanceof Column c
          && column.equals(unquote(c.getColumnName()).toLowerCase(Locale.ROOT));
    }

    private static String unquote(String identifier) {
      if (identifier.length() >= 2) {
        char first = identifier.charAt(0);
        char last = identifier.charAt(identifier.length() - 1);
        if ((first == '"' && last == '"')
            || (first == '`' && last == '`')
            || (first == '[' && last == ']')) {
          return identifier.substring(1, identifier.length() - 1);
        }
      }
      return identifier;
    }

    /** True when this occurrence is the one to neutralize. */
    private boolean hit(String what) {
      boolean selected = index == target;
      if (selected) {
        description = what + " #" + index;
      }
      index++;
      return selected;
    }
  }
}
