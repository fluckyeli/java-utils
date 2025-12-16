package com.fluckyeli.ddlparser.useDemo;

import com.alibaba.druid.DbType;
import com.fluckyeli.ddlparser.ColumnMeta;
import com.fluckyeli.ddlparser.GenericDdlParserUtils;
import com.fluckyeli.ddlparser.TableMeta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全面测试类：验证 GenericDdlParserUtils 对不同数据库的支持
 */
public class UniversalDdlParserTest {

    public static void main(String[] args) {
        // 使用 Map 存储 测试名称 -> 测试用例(DDL, DbType)
        Map<String, TestCase> testCases = new LinkedHashMap<>();

        // 1. MySQL (主要测试：反引号, AUTO_INCREMENT, 行级注释)
        testCases.put("MySQL", new TestCase(
                DbType.mysql,
                "CREATE TABLE `t_order` (\n" +
                        "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',\n" +
                        "  `order_no` varchar(64) DEFAULT NULL COMMENT '订单号',\n" +
                        "  `amount` decimal(10,2) DEFAULT '0.00',\n" +
                        "  PRIMARY KEY (`id`)\n" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';"
        ));

        // 2. Oracle (主要测试：双引号, NUMBER/VARCHAR2 类型, 无反引号)
        testCases.put("Oracle", new TestCase(
                DbType.oracle,
                "CREATE TABLE \"HR\".\"EMPLOYEES\" (\n" +
                        "   \"EMPLOYEE_ID\" NUMBER(6,0) PRIMARY KEY,\n" +
                        "   \"FIRST_NAME\" VARCHAR2(20),\n" +
                        "   \"HIRE_DATE\" DATE DEFAULT sysdate\n" +
                        ");"
        ));

        // 3. PostgreSQL (主要测试：Schema前缀, SERIAL, JSONB 复杂类型)
        testCases.put("PostgreSQL", new TestCase(
                DbType.postgresql,
                "CREATE TABLE public.products (\n" +
                        "    product_id SERIAL PRIMARY KEY,\n" +
                        "    name text NOT NULL,\n" +
                        "    attributes jsonb,\n" +
                        "    price numeric(10,2)\n" +
                        ");"
        ));

        // 4. SQL Server (主要测试：方括号 [], IDENTITY)
        testCases.put("SQL Server", new TestCase(
                DbType.sqlserver,
                "CREATE TABLE [dbo].[Users] (\n" +
                        "    [UserId] INT IDENTITY(1,1) PRIMARY KEY,\n" +
                        "    [UserName] NVARCHAR(50) NOT NULL,\n" +
                        "    [CreatedAt] DATETIME DEFAULT GETDATE()\n" +
                        ");"
        ));

        // 5. ODPS / MaxCompute (主要测试：LIFECYCLE, PARTITIONED BY)
        testCases.put("ODPS (MaxCompute)", new TestCase(
                DbType.odps,
                "CREATE TABLE IF NOT EXISTS data_works_log (\n" +
                        "    log_code STRING COMMENT '日志编码',\n" +
                        "    content STRING COMMENT '内容'\n" +
                        ") \n" +
                        "COMMENT '日志表'\n" +
                        "PARTITIONED BY (dt STRING, region STRING)\n" +
                        "LIFECYCLE 30;"
        ));

        // 6. Hive (主要测试：复杂的 ARRAY/STRUCT 类型, 分区)
        testCases.put("Hive", new TestCase(
                DbType.hive,
                "CREATE TABLE student_scores (\n" +
                        "  student_id INT,\n" +
                        "  name STRING,\n" +
                        "  scores MAP<STRING, INT>,\n" +
                        "  address STRUCT<city:STRING, street:STRING>\n" +
                        ")\n" +
                        "PARTITIONED BY (semester STRING)\n" +
                        "STORED AS ORC;"
        ));

        // 7. ClickHouse (主要测试：ClickHouse 特有类型 Int32, MergeTree)
        testCases.put("ClickHouse", new TestCase(
                DbType.clickhouse,
                "CREATE TABLE hit_log (\n" +
                        "    WatchID UInt64,\n" +
                        "    JavaEnable UInt8,\n" +
                        "    Title String\n" +
                        ") ENGINE = MergeTree() ORDER BY WatchID;"
        ));

        // --- 执行测试 ---
        System.out.println("=========================================");
        System.out.println("   Druid DDL 解析器 - 多数据库兼容性测试");
        System.out.println("=========================================");

        for (Map.Entry<String, TestCase> entry : testCases.entrySet()) {
            runTest(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 执行单个测试并打印结果
     */
    private static void runTest(String dbName, TestCase testCase) {
        System.out.println("\n>>> 测试场景: " + dbName);
        try {
            TableMeta meta = GenericDdlParserUtils.parseCreateTable(testCase.ddl, testCase.dbType);

            if (meta == null) {
                System.err.println("❌ 解析结果为空 (可能不是 Create Table 语句或语法错误)");
                return;
            }

            System.out.println("✅ 解析成功 | 数据库类型: " + meta.getDbType());
            System.out.println("   表名: " + meta.getTableName());
            System.out.println("   字段概览 (" + meta.getColumns().size() + " 列):");

            // 打印前3个字段作为示例
            int limit = 3;
            for (int i = 0; i < Math.min(meta.getColumns().size(), limit); i++) {
                ColumnMeta col = meta.getColumns().get(i);
                System.out.printf("     - %-15s | 类型: %-15s | 注释: %s\n",
                        col.toString().split(",")[0].substring(1), // 简单截取名字用于展示
                        getColumnType(col),
                        getComment(col)
                );
            }
            if (meta.getColumns().size() > limit) System.out.println("     ... (更多列省略)");

            // 如果有分区字段，打印出来
            if (!meta.getPartitionColumns().isEmpty()) {
                System.out.println("   🚩 分区字段: " + meta.getPartitionColumns());
            }

        } catch (Exception e) {
            System.err.println("❌ 发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 辅助类：简单的测试用例封装
    static class TestCase {
        DbType dbType;
        String ddl;

        public TestCase(DbType dbType, String ddl) {
            this.dbType = dbType;
            this.ddl = ddl;
        }
    }

    // 辅助提取显示信息 (因为 ColumnMeta 字段是 private，实际使用建议加 Getter)
    private static String getColumnType(ColumnMeta col) {
        // 这里只是为了演示，强行 toString 解析，实际应该在 ColumnMeta 加 getter
        String s = col.toString();
        int start = s.indexOf(", ") + 2;
        int end = s.indexOf(", comment=");
        if (start > 0 && end > start) return s.substring(start, end);
        return "Unknown";
    }

    private static String getComment(ColumnMeta col) {
        String s = col.toString();
        int start = s.indexOf("comment='") + 9;
        return s.substring(start, s.length() - 2);
    }
}