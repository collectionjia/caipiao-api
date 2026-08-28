package com.xytl.project.caipiaoapi.utils.json;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.xytl.project.caipiaoapi.domain.data.ResultList;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

public class HistoryTest {

    private static String numstr="1,2,3,4,5,6,7,8,9,10,";


    public static void main(String[] args) throws IOException {
        Map<String, String> header = getHeader("02f399d8a36943e0962f29afb06d56551760087140751");
        prediction(67, header);
    }


    /**
     * 获取请求头
     *
     * @return
     */
    private static Map<String, String> getHeader(String token) {
        Map<String, String> header = new HashMap<>();
        if (StringUtils.isNotEmpty(token)) {
            header.put("token", token);
        } else {
            header.put("token", "0a90ce8649ec4a5aa92137b3afba0e5d1747467014954");
        }
        header.put("User-Agent",
                "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        header.put("device", "4");
        return header;
    }


    public static String prediction(int ticketId, Map<String, String> header) throws IOException {
        //根据100期预测出一组数据
        String numstr = "";
        int countnum=100;
        String queryNumUrl = "https://bwapi-cf.rhgknx.com:2083/coron/api/ticketSourceResult/ticketSourceResultList.json";
        String jsonParams = "{\"ticketId\":" + ticketId + ",\"num\":" + countnum + "}";
        HttpRequest createnum = HttpUtil.createPost(queryNumUrl).addHeaders(header);
        createnum.body(jsonParams);
        // 执行请求
        String response2 = createnum.execute().body();
        ResultList resultList = JSON.parseObject(response2, ResultList.class);
        if (resultList.getData().size() > 0) {
            List<ResultList.DataSourceItem> datalist = resultList.getData();
                    Map<String, Integer> countMap = new HashMap<>(); // 创建用于统计的Map
            StringBuilder sb = new StringBuilder();
            String tempstr="";
            for (int i = datalist.size() - 1; i >= 0; i--) {
                    ResultList.DataSourceItem dsitem = datalist.get(i);
                    sb.append(dsitem.getCode());
                    System.out.println(dsitem.getCode());
                    String codestr=dsitem.getCode();
                    String[] codearry=codestr.split(" ");
                    tempstr=codestr;
                    int num= Integer.parseInt(codearry[0])-1;
                      String targetValue = codearry[num];
                     countMap.put(targetValue, countMap.getOrDefault(targetValue, 0) + 1);
                    // System.out.println(codearry[num]+"第"+ num  +"位");
                    sb.append("\n");
                }

 // 输出统计结果
        System.out.println("统计结果:");
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            System.out.println("数字 " + entry.getKey() + " 出现了 " + entry.getValue() + " 次");
        }

        }

        return "";
    }








    /**
     * 转化成二维数组
     * @param input
     * @return
     */
    public static int[][] convertStringTo2DArray(String input) {
        // 按换行符分割字符串，得到每一行
        String[] rows = input.split("\n");
        int rowCount = rows.length;

        // 检查是否有行，避免空数组
        if (rowCount == 0) {
            return new int[0][0];
        }

        // 按空格分割第一行，得到列数
        String[] firstRowElements = rows[0].split(" ");
        int colCount = firstRowElements.length;

        // 创建二维数组
        int[][] array = new int[rowCount][colCount];

        // 遍历每一行和每一列，将字符串转换为整数并存储到二维数组
        for (int i = 0; i < rowCount; i++) {
            String[] elements = rows[i].split(" ");
            for (int j = 0; j < colCount; j++) {
                array[i][j] = Integer.parseInt(elements[j]);
            }
        }

        return array;
    }


    /**
     * 每列每个数字出现的次数
     * @return
     */
    public static String queryCountNum(String datastr){
        // 将字符串转换为二维数组
        int[][] matrix = convertStringTo2DArray(datastr);
        // 获取矩阵的行数和列数
        int rows = matrix.length;
        int cols = matrix[0].length;

        // 用于存储每列每个数字的出现次数
        int[][] frequency = new int[cols][11]; // 假设数字范围是1到10
        // 遍历矩阵，统计每列每个数字的出现次数
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                int number = matrix[row][col];
                frequency[col][number]++;
            }
        }
        // 计算每列每个数字的得分
        System.out.println("列\t数字\t出现次数\t得分");
        for (int col = 0; col < cols; col++) {
            for (int num = 1; num <= 10; num++) {
                int count = frequency[col][num];
                double score = 50.0 / (count + 1);
                if(score<10){
                    System.out.printf("%d\t%d\t%d\t%.2f%n", col + 1, num, count, score);
                }
            }
            System.out.println("");
            System.out.println("");
            System.out.println("");
        }
        return "";
    }


    public static void columnStatistics(String datastr,int countnum){
        // 将字符串转换为二维数组
        int[][] data = convertStringTo2DArray(datastr);
        int rows = data.length; // 行数
        int cols = data[0].length; // 列数


        // 遍历每一列
        for (int col = 0; col < cols; col++) {
            System.out.println("第 " + (col + 1) + " 赛道数据统计：");
            Map<Integer, Double> percentageMap = new HashMap<>(); // 用于存储每个数字的百分比


            // 遍历每一行，统计当前列的数字出现次数
            for (int row = 0; row < rows; row++) {
                int num = data[row][col];
//                System.out.println(num+"   ---   "+percentageMap.getOrDefault(num, 0.0) + 1);
                percentageMap.put(num, percentageMap.getOrDefault(num, 0.0) + 1);
            }

            String[] numarry=numstr.split(",");


            for(int i=0;i<numarry.length;i++){
                int num= Integer.parseInt( numarry[i]);
                if(percentageMap.get(num)!=null){
                    double count = percentageMap.get(num);
                    double percentage = ((countnum-count) / rows) * 100;
                    percentageMap.put(num, percentage);
                    System.out.printf("数字 %d 出现了 %.2f%%\n", num, percentage);
                }else{
                    double percentage = ((countnum-0) / rows) * 100;
                    percentageMap.put(num, percentage);
                    System.out.printf("数字 %d 出现了 %.2f%%\n", num, percentage);
                }
            }

//            // 计算每个数字的百分比
//            for (Map.Entry<Integer, Double> entry : percentageMap.entrySet()) {
//                int num = entry.getKey();
//                double count = entry.getValue();
//                double percentage = ((countnum-count) / rows) * 100;
//                entry.setValue(percentage);
//                System.out.printf("数字 %d 出现了 %.2f%%\n", num, percentage);
//            }

            // 分类统计剩余百分比范围
            Map<String, List<Integer>> rangeStats = classifyByRemainingPercentage(percentageMap);

            // 输出分类统计结果
            System.out.println("剩余百分比范围统计：");
            for (Map.Entry<String, List<Integer>> entry : rangeStats.entrySet()) {
                List<Integer> sortedList = entry.getValue();
                Collections.sort(sortedList); // 对每个范围内的数字进行排序
                System.out.printf("%s: %s%n", entry.getKey(), sortedList);
            }
            System.out.println(); // 换行分隔每列的统计结果
        }
    }


    // 根据剩余百分比范围对数字进行分类统计
    public static Map<String, List<Integer>> classifyByRemainingPercentage(Map<Integer, Double> percentageMap) {
        Map<String, List<Integer>> rangeStats = new HashMap<>();
        String[] ranges = {"100%", "91%-99%", "81%-90%", "71%-80%", "50%-70%"};

        // 初始化范围列表
        for (String range : ranges) {
            rangeStats.put(range, new ArrayList<>());
        }

        // 遍历每个数字，根据剩余百分比分类
        for (Map.Entry<Integer, Double> entry : percentageMap.entrySet()) {
            int num = entry.getKey();
            double percentage = entry.getValue();
            double remainingPercentage = percentage;

            if (remainingPercentage ==100) {
                rangeStats.get("100%").add(num);
            } else if (remainingPercentage <= 99 && remainingPercentage >= 91) {
                rangeStats.get("91%-99%").add(num);
            } else if (remainingPercentage <=90 && remainingPercentage >= 81) {
                rangeStats.get("81%-90%").add(num);
            }else if (remainingPercentage <=70 && remainingPercentage >= 50) {
                rangeStats.get("50%-70%").add(num);
            }else if (remainingPercentage <=80 && remainingPercentage >= 71) {
                rangeStats.get("71%-80%").add(num);
            }
        }

        return rangeStats;
    }

}
