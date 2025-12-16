package com.fluckyeli.ddlparser.useDemo;

import com.alibaba.druid.DbType;
import com.fluckyeli.ddlparser.GenericDdlParserUtils;
import com.fluckyeli.ddlparser.TableMeta;

import java.util.List;

public class MultiDdlParseTest {
    public static void main(String[] args) {
        String multiSql =
                "CREATE TABLE table_a (id INT, name STRING); " +
                        "DROP TABLE IF EXISTS old_table; " + // 这条会被忽略
                        "CREATE TABLE table_b (id INT, score DOUBLE) PARTITIONED BY (dt STRING);";

        List<TableMeta> tables =
                GenericDdlParserUtils.parseMultiCreateTable(multiSql, DbType.odps);

        System.out.println("共解析出表数量: " + tables.size());
        tables.forEach(t -> System.out.println("解析到表: " + t.getTableName()));
    }
}