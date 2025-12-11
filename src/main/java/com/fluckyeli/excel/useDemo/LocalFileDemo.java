package com.fluckyeli.excel.useDemo;

import com.fluckyeli.excel.ExcelUtils;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

public class LocalFileDemo {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileDemo.class);

    public static void main(String[] args) {
        String filePath = "assets/products.xlsx"; // 假设文件在项目根目录下

        File file = new File(filePath);

        // 检查文件是否存在
        if (!file.exists()) {
            System.err.println("错误：找不到文件 -> " + file.getAbsolutePath());
            System.out.println("请在项目根目录下创建一个名为 products.xlsx 的文件再试。");
            return;
        }

        System.out.println("开始解析文件: " + file.getAbsolutePath());

        // 2. 使用 try-with-resources 自动关闭流
        try (FileInputStream fis = new FileInputStream(file)) {

            // 3. 调用工具类
            // 假设第1行是表头(index=0)，数据从第2行开始(index=1)
            List<Product> productList = ExcelUtils.parse(fis, Product.class, 1, null);

            // 4. 输出结果
            System.out.println("解析成功，共获取 " + productList.size() + " 条数据：");
            for (Product p : productList) {
                System.out.println(p);
            }

        } catch (FileNotFoundException e) {
            System.err.println("文件未找到");
        } catch (Exception e) {
            logger.error(String.format("解析 %s 时出现异常",filePath), e);
        }
    }
}