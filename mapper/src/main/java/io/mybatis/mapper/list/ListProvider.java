/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mybatis.mapper.list;

import io.mybatis.provider.EntityColumn;
import io.mybatis.provider.EntityTable;
import io.mybatis.provider.SqlScript;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 批量操作方法
 *
 * @author liuzh
 */
public class ListProvider {

  /**
   * @return 批量插入
   */
  public static String insertList(ProviderContext providerContext, @Param("entityList") List<?> entityList) {
    if (entityList == null || entityList.size() == 0) {
      throw new NullPointerException("Parameter cannot be empty");
    }
    return SqlScript.caching(providerContext, new SqlScript() {
      @Override
      public String getSql(EntityTable entity) {
        return "INSERT INTO " + entity.tableName()
            + "(" + entity.insertColumnList() + ")"
            + " VALUES "
            + foreach("entityList", "entity", ",", () ->
            trimSuffixOverrides("(", ")", ",", () ->
                entity.insertColumns().stream().map(column -> column.variables("entity.")).collect(Collectors.joining(","))));
      }
    });
  }

  /**
   * 单主键批量更新
   */

  public static String updateList(ProviderContext providerContext, @Param("entityList") List<?> entityList) {
    if (entityList == null || entityList.size() == 0) {
      throw new NullPointerException("Parameter cannot be empty");
    }
    return SqlScript.caching(providerContext, new SqlScript() {
      @Override
      public String getSql(EntityTable entity) {
        List<EntityColumn> idColumns = entity.idColumns();
//        EntityColumn entityColumn = idColumns.get(0);
        String sql = "UPDATE "
            + entity.tableName()
            + trimSuffixOverrides("SET", " ", ",", () -> entity.normalColumns().stream().map(column ->
                trimSuffixOverrides(column.column() + " = CASE ", "end, ", "", () ->
                    foreach("entityList", "entity", " ", () ->
                        "WHEN ( " +
                            idColumns.stream().map(id -> id.columnEqualsProperty("entity.")).collect(Collectors.joining(" AND "))
                            + ") THEN " + column.variables("entity.")

                    )
                ))
            .collect(Collectors.joining("")))
            + where(() ->
            idColumns.stream().map(id ->
                id.column() + " in " + foreach("entityList", "entity", ",", "(", ")", () -> id.variables("entity."))
            ).collect(Collectors.joining(" AND "))

        );
        return sql;

      }
    });
  }

  /**
   * 单主键，非空更新
   *
   * @param providerContext
   * @param entityList
   * @return String
   */
  public static String updateListSelective(ProviderContext providerContext, @Param("entityList") List<?> entityList) {
    if (entityList == null || entityList.size() == 0) {
      throw new NullPointerException("Parameter cannot be empty");
    }
    return SqlScript.caching(providerContext, new SqlScript() {
      @Override
      public String getSql(EntityTable entity) {
        List<EntityColumn> idColumns = entity.idColumns();
        String sql = "UPDATE "
            + entity.tableName()
            + trimSuffixOverrides("SET", " ", ",", () -> entity.normalColumns().stream().map(column ->
                trimSuffixOverrides(column.column() + " = CASE ", "end, ", "", () ->
                    foreach("entityList", "entity", " ", () ->
                        choose(() -> whenTest(column.notNullTest("entity."),
                            () -> "WHEN ( " +
                                idColumns.stream().map(id -> id.columnEqualsProperty("entity.")).collect(Collectors.joining(" AND "))
                                + ") THEN " + column.variables("entity.")
                        ) +
                            otherwise(() ->
                                "WHEN ( " +
                                    idColumns.stream().map(id -> id.columnEqualsProperty("entity.")).collect(Collectors.joining(" AND "))
                                    +
                                    " ) THEN " + column.column()))
                    )))
            .collect(Collectors.joining("")))

            + where(() ->
            idColumns.stream().map(id ->
                id.column() + " in " + foreach("entityList", "entity", ",", "(", ")", () -> id.variables("entity."))
            ).collect(Collectors.joining(" AND "))

        );
        return sql;
      }
    });
  }

}
