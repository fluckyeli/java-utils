package com.fluckyeli.excel;

import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;


/**
 * 纯 Java 实现的 Excel 工具类，不依赖 Spring
 */
public class ExcelUtils {

    /**
     * 解析 Excel 流
     *
     * @param inputStream 输入流 (调用者负责提供流，本方法会在 try-with-resources 中关闭 Workbook，流也会随之关闭)
     * @param clazz       映射的 Bean 类
     * @param startRow    数据起始行（0-based，例如表头在第0行，数据从第1行开始，则填1）
     * @param endRow      结束行（null 表示读到最后一行）
     * @param <T>         泛型
     * @return 解析后的对象列表
     */
    public static <T> List<T> parse(InputStream inputStream, Class<T> clazz, int startRow, Integer endRow) {
        List<T> resultList = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) { // 自动关闭资源

            Sheet sheet = workbook.getSheetAt(0); // 默认读取第一个 Sheet
            if (sheet == null) {
                return resultList;
            }

            int totalRows = sheet.getPhysicalNumberOfRows();
            int actualEndRow = (endRow == null || endRow > totalRows) ? totalRows : endRow;

            // 1. 解析表头 (假设第0行总是表头，用于建立映射关系)
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headerMap = new HashMap<>();
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    headerMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
                }
            }

            // 2. 建立 字段 -> 列索引 的映射
            Map<Field, Integer> fieldColumnMap = new HashMap<>();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(ExcelColumn.class)) {
                    ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                    String headerName = annotation.value();
                    if (headerMap.containsKey(headerName)) {
                        field.setAccessible(true); // 允许访问私有字段
                        fieldColumnMap.put(field, headerMap.get(headerName));
                    }
                }
            }

            // 3. 遍历数据行
            for (int i = startRow; i < actualEndRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                T instance = clazz.getDeclaredConstructor().newInstance();
                boolean hasData = false;

                for (Map.Entry<Field, Integer> entry : fieldColumnMap.entrySet()) {
                    Field field = entry.getKey();
                    Integer colIndex = entry.getValue();
                    Cell cell = row.getCell(colIndex);

                    if (cell != null) {
                        Object cellValue = convertCellValue(cell, field.getType());
                        if (cellValue != null) {
                            field.set(instance, cellValue);
                            hasData = true;
                        }
                    }
                }

                if (hasData) {
                    resultList.add(instance);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Excel 解析失败", e);
        }
        
        return resultList;
    }

    /**
     * 单元格类型转换逻辑
     */
    private static Object convertCellValue(Cell cell, Class<?> fieldType) {
        DataFormatter formatter = new DataFormatter(); // POI 提供的格式化工具

        // 1. String
        if (fieldType == String.class) {
            return formatter.formatCellValue(cell);
        }
        
        // 2. Integer
        if (fieldType == Integer.class || fieldType == int.class) {
            String val = formatter.formatCellValue(cell);
            return (val == null || val.isEmpty()) ? null : Integer.parseInt(val);
        }
        
        // 3. Double
        if (fieldType == Double.class || fieldType == double.class) {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }
            String val = formatter.formatCellValue(cell);
            return (val == null || val.isEmpty()) ? null : Double.parseDouble(val);
        }
        
        // 4. BigDecimal
        if (fieldType == BigDecimal.class) {
            String val = formatter.formatCellValue(cell);
            return (val == null || val.isEmpty()) ? null : new BigDecimal(val);
        }
        
        // 5. Date
        if (fieldType == Date.class) {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            }
        }

        return null;
    }
}