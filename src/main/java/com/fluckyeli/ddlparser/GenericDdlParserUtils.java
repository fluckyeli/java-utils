package com.fluckyeli.ddlparser;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.alibaba.druid.util.StringUtils;

import java.sql.SQLSyntaxErrorException;
import java.util.List;

/**
 * 通用数据库 DDL 解析工具类
 * 支持 MySQL, Oracle, PostgreSQL, Hive, ODPS 等 Druid 支持的所有类型
 */
public class GenericDdlParserUtils {

    /**
     * 解析 Create Table 语句 (入口方法)
     *
     * @param ddl    DDL SQL 语句
     * @param dbType 数据库类型 (使用 com.alibaba.druid.DbType 枚举，如 DbType.mysql, DbType.oracle, DbType.odps)
     * @return TableMeta
     */
    public static TableMeta parseCreateTable(String ddl, DbType dbType) {
        if (StringUtils.isEmpty(ddl)) return null;

        try {
            // 1. 根据传入的 dbType 解析语句
            List<SQLStatement> statements = SQLUtils.parseStatements(ddl, dbType);
            if (statements.isEmpty()) return null;

            SQLStatement statement = statements.get(0);

            // 2. 确保是建表语句
            if (statement instanceof SQLCreateTableStatement) {
                SQLCreateTableStatement createTableStmt = (SQLCreateTableStatement) statement;
                TableMeta tableMeta = new TableMeta();
                tableMeta.setDbType(dbType.name());

                // 3. 提取表名
                tableMeta.setTableName(cleanName(createTableStmt.getTableName()));

                // 4. 提取普通列
                // Druid 将列定义和约束（如主键约束）都放在 TableElementList 中
                List<SQLTableElement> tableElementList = createTableStmt.getTableElementList();
                for (SQLTableElement element : tableElementList) {
                    if (element instanceof SQLColumnDefinition) {
                        SQLColumnDefinition columnDef = (SQLColumnDefinition) element;
                        tableMeta.getColumns().add(extractColumnInfo(columnDef));
                    }
                }

                // 5. 提取分区列
                // 注意：只有支持类似 Hive/ODPS 语法 (PARTITIONED BY) 的数据库，此列表才会有值
                // MySQL 的 Partition By Range 通常不被解析为 ColumnDefinition，而是 PartitionBy 子句，处理方式不同
                List<SQLColumnDefinition> partitionColumns = createTableStmt.getPartitionColumns();
                if (partitionColumns != null) {
                    for (SQLColumnDefinition partitionCol : partitionColumns) {
                        tableMeta.getPartitionColumns().add(extractColumnInfo(partitionCol));
                    }
                }

                return tableMeta;
            }else if (statement instanceof SQLDropTableStatement){
                throw new SQLSyntaxErrorException("这是一个 Drop Table 语句，而非 Create Table 语句。");
            }else {
                throw new SQLSyntaxErrorException("不支持的 SQL 语句类型: " + statement.toString());
            }
        } catch (Exception e) {
            System.err.println("解析失败 [" + dbType + "]: " + e.getMessage());
        }
        return null;
    }

    /**
     * 辅助：提取列详情
     */
    private static ColumnMeta extractColumnInfo(SQLColumnDefinition columnDef) {
        String colName = cleanName(columnDef.getName().getSimpleName());
        String colType = columnDef.getDataType().toString();

        String colComment = null;
        if (columnDef.getComment() != null) {
            colComment = cleanQuote(columnDef.getComment().toString());
        }

        // 简单判断是否为主键 (仅判断行内定义，未判断表级约束)
        boolean isPk = columnDef.isPrimaryKey();

        return new ColumnMeta(colName, colType, colComment, isPk);
    }

    /**
     * 清理标识符 (去除反引号 ` 和 双引号 ")
     * MySQL 使用 `, Oracle/Postgres 使用 "
     */
    private static String cleanName(String name) {
        if (name == null) return null;
        return name.replaceAll("[`\"]", "");
    }

    /**
     * 清理字符串值 (去除单引号)
     */
    private static String cleanQuote(String val) {
        if (val == null) return null;
        return val.replace("'", "");
    }
}