package com.fluckyeli.ddlparser;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class TableMeta {
    private String tableName;
    private String dbType; // 记录解析时的数据库类型
    private List<ColumnMeta> columns = new ArrayList<>();
    private List<ColumnMeta> partitionColumns = new ArrayList<>();

    @Override
    public String toString() {
        return "TableMeta [" + dbType + "] " + tableName + "\n" +
                "  Columns: " + columns + "\n" +
                "  Partitions: " + partitionColumns;
    }
}
