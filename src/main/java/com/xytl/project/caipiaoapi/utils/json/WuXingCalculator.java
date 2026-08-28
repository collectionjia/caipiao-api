package com.xytl.project.caipiaoapi.utils.json;

import java.util.Scanner;

public class WuXingCalculator {

    private static final String[] TIANGAN = {"", "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] DIZHI = {"", "子鼠", "丑牛", "寅虎", "卯兔", "辰龙", "巳蛇", "午马", "未羊", "申猴", "酉鸡", "戌狗", "亥猪"};
    private static final String[] WUXING_TIANGAN = {"", "木", "木", "火", "火", "土", "土", "金", "金", "水", "水"};
    private static final String[] WUXING_DIZHI = {"", "水", "土", "木", "木", "土", "火", "火", "土", "金", "金", "土", "水"};

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入一个年份：");
        int year = 1993;
        int index = (year - 4) % 60; // 计算天干地支的索引
        String gzYear = TIANGAN[index + 1] + DIZHI[index + 1];
        String wuxing = WUXING_TIANGAN[index + 1] + WUXING_DIZHI[index + 1];
        String animal = DIZHI[index + 1].substring(1);
        System.out.println(year + "年" + gzYear + "属" + wuxing + "(" + animal + ")");
    }

}
