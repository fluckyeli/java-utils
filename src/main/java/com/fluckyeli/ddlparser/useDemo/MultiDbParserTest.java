package com.fluckyeli.ddlparser.useDemo;

import com.alibaba.druid.DbType;
import com.fluckyeli.ddlparser.GenericDdlParserUtils;
import com.fluckyeli.ddlparser.TableMeta;

public class MultiDbParserTest {
    public static void main(String[] args) {
        // --- 场景 1: MySQL ---
        String mysqlDDL = "CREATE TABLE `users` (" +
                "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                "  `username` varchar(50) DEFAULT NULL COMMENT '用户名'," +
                "  PRIMARY KEY (`id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';";

        printResult(GenericDdlParserUtils.parseCreateTable(mysqlDDL, DbType.mysql));

        // --- 场景 2: Oracle ---
        // Oracle 通常没有反引号，字段类型也不同
        String oracleDDL = "CREATE TABLE \"EMP_DATA\" (" +
                "  \"EMP_ID\" NUMBER(10) PRIMARY KEY," +
                "  \"EMP_NAME\" VARCHAR2(100)," +
                "  \"JOIN_DATE\" DATE" +
                ");";

        printResult(GenericDdlParserUtils.parseCreateTable(oracleDDL, DbType.oracle));

        // --- 场景 3: ODPS / Hive (支持分区) ---
        String odpsDDL = "CREATE TABLE IF NOT EXISTS sale_log (" +
                "  log_id STRING COMMENT '日志ID'," +
                "  amount DOUBLE" +
                ") " +
                "PARTITIONED BY (ds STRING, city STRING);";

        printResult(GenericDdlParserUtils.parseCreateTable(odpsDDL, DbType.odps));
    }

    private static void printResult(TableMeta meta) {
        System.out.println("------------------------------------------------");
        if (meta != null) {
            System.out.println("解析成功 [" + meta.getDbType() + "]");
            System.out.println("表名: " + meta.getTableName());
            System.out.println("字段数: " + meta.getColumns().size());
            // 简单打印第一个字段看看类型
            if (!meta.getColumns().isEmpty()) {
                System.out.println("首字段: " + meta.getColumns().get(0));
            }
            if (!meta.getPartitionColumns().isEmpty()) {
                System.out.println("分区字段: " + meta.getPartitionColumns());
            }
        } else {
            System.out.println("解析失败");
        }
    }
}