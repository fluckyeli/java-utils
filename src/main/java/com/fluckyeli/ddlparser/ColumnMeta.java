package com.fluckyeli.ddlparser;

public class ColumnMeta {
    final private String name;
    final private String type;
    final private String comment;
    final private boolean isPrimaryKey; // 新增：是否为主键标识

    public ColumnMeta(String name, String type, String comment, boolean isPrimaryKey) {
        this.name = name;
        this.type = type;
        this.comment = comment;
        this.isPrimaryKey = isPrimaryKey;
    }

    @Override
    public String toString() {
        String pkMark = isPrimaryKey ? " [PK]" : "";
        return String.format("{%s, %s%s, comment='%s'}", name, type, pkMark, comment);
    }
}
