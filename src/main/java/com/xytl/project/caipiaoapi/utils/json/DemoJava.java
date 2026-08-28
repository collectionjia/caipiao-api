package com.xytl.project.caipiaoapi.utils.json;

import java.util.HashMap;
import java.util.Map;

public class DemoJava {

    public static void main(String[] args) {
            String input="5:22,3:77";
            String aa=getStringValue(input);
            System.out.println(aa);
    }




    private static String getStringValue(String input){
        String numberstr="";
        Map<Integer, Double> numberScoreMap = parseInput(input);
        // 找出最高分数
        double maxScore = numberScoreMap.values().stream().min(Double::compare).orElse(0.0);

        // 找出所有具有最高分数的号码
        Map<Integer, Double> maxScoreNumbers = new HashMap<>();
        numberScoreMap.forEach((num, score) -> {
            if (score == maxScore) {
                maxScoreNumbers.put(num, score);
            }
        });

        // 如果只有一个最高分数，直接选择
        if (maxScoreNumbers.size() == 1) {
            Map.Entry<Integer, Double> entry = maxScoreNumbers.entrySet().iterator().next();
            System.out.println("投注选择: 号码 " + entry.getKey() + ", 分数 " + entry.getValue());
            numberstr=entry.getKey()+":"+entry.getValue();
        }
        // 如果有多个相同最高分数，选择号码最大的
        else {
            int maxNumber = maxScoreNumbers.keySet().stream().min(Integer::compare).orElse(0);
            System.out.println("投注选择: 号码 " + maxNumber + ", 分数 " + maxScoreNumbers.get(maxNumber));
            numberstr=maxNumber+":"+maxScoreNumbers.get(maxNumber);
        }
        return numberstr;
    }


    private static Map<Integer, Double> parseInput(String input) {
        Map<Integer, Double> map = new HashMap<>();
        String[] pairs = input.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            int number = Integer.parseInt(parts[0]);
            double score = Double.parseDouble(parts[1]);
            map.put(number, score);
        }
        return map;
    }


}
