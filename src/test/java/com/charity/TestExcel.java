package com.charity;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import java.util.*;

public class TestExcel {
    public static void main(String[] args) {
        try {
            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.addHeaderAlias("id", "活动ID");
            writer.addHeaderAlias("title", "标题");
            writer.setOnlyAlias(true);

            List<Map<String, Object>> list = new ArrayList<>();
            writer.write(list, true);
            writer.close();
            System.out.println("Done");
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }
}
