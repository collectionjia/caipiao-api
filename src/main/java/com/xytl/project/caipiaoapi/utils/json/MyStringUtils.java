package com.xytl.project.caipiaoapi.utils.json;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字符串处理
 */
public class MyStringUtils {

    /**
     * 返回值是否为空
     *
     * @return
     */
    public static boolean valueIsNotEmpty(String valuestr) {
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(valuestr) && !"null".equals(valuestr)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 返回值是否为空
     *
     * @return
     */
    public static boolean valueIsEmpty(String valuestr) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(valuestr) || "null".equals(valuestr)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 判断redis非空并返回数据
     *
     * @return
     */
    public static String stringNotNull(String paramValue, String defaultValue) {
        if (StringUtils.isNotEmpty(paramValue)) {
            return paramValue;
        } else {
            return defaultValue;
        }
    }


    /**
     * 判断redis非空并返回数据
     *
     * @return
     */
    public static String string3double(String string3double) {
        String s = "";
        s = String.format("%.3f", Double.parseDouble(string3double));
        return s;
    }


    public static String quchong(String originalStr) {
        // 1. 按逗号分割成单个条目
        String[] items = originalStr.split(",");

        // 2. 用LinkedHashSet保存去重后的条目（保证顺序），用普通Set记录已出现的key
        Set<String> keySet = new LinkedHashSet<>(); // 存最终结果（保持插入顺序）
        Set<String> existKeys = new java.util.HashSet<>(); // 存已出现的第一个数字

        for (String item : items) {
            // 3. 提取第一个冒号前的数字作为key
            String key = item.split(":")[0];

            // 4. 如果key未出现过，就保留该条目
            if (!existKeys.contains(key)) {
                existKeys.add(key);
                keySet.add(item);
            }
        }

        // 5. 将去重后的集合拼接回字符串
        String result = String.join(",", keySet);

        // 输出结果
        System.out.println("去重后的字符串：");
        System.out.println(result);
        return result;
    }



    /**
     * 核心过滤方法
     * @param str1 原始数据字符串
     * @param str2 过滤条件字符串
     * @return 过滤后的结果字符串
     */
    public static String filterStrings(String str1, String str2) {
        // 1. 分割字符串2，获取各数组的过滤分数列表
        String[] str2Parts = str2.split(";");
        List<List<Double>> filterScores = new ArrayList<>();
        for (String part : str2Parts) {
            List<Double> scores = Arrays.stream(part.split(","))
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
            filterScores.add(scores);
        }

        // 2. 分割字符串1，处理每条数据
        String[] str1Items = str1.split(",");
        List<String> filteredItems = new ArrayList<>();

        for (String item : str1Items) {
            // 分割每条数据：序号:分数:数组标识
            String[] itemParts = item.split(":");
            if (itemParts.length != 3) {
                continue; // 跳过格式错误的数据
            }

            try {
                double score = Double.parseDouble(itemParts[1]); // 分数
                int arrayIndex = Integer.parseInt(itemParts[2]); // 数组标识

                // 检查数组标识是否有效，并且分数是否在过滤列表中
                if (arrayIndex < filterScores.size() && filterScores.get(arrayIndex).contains(score)) {
                    filteredItems.add(item); // 符合条件则保留
                }
            } catch (NumberFormatException e) {
                // 数字格式错误时跳过该条数据
                continue;
            }
        }

        // 3. 将过滤后的结果拼接成字符串返回
        return String.join(",", filteredItems);
    }



    public static String queryByScore(String originalStr, String filterScoreStr) {
        // 步骤1：解析过滤字符串，提取所有需要保留的分数，存入Set方便快速匹配
        Set<Double> targetScores = new HashSet<>();
        // 先按分号分割分组，再按逗号分割单个分数
        String[] filterGroups = filterScoreStr.split(";");
        for (String group : filterGroups) {
            String[] scores = group.split(",");
            for (String scoreStr : scores) {
                try {
                    // 转为Double类型，统一匹配（避免字符串"9.0"和"9"不匹配的问题）
                    double score = Double.parseDouble(scoreStr.trim());
                    targetScores.add(score);
                } catch (NumberFormatException e) {
                    System.out.println("忽略无效的过滤分数：" + scoreStr);
                }
            }
        }
        System.out.println("需要保留的分数列表：" + targetScores);

        // 步骤2：解析原始字符串，筛选出分数在目标列表中的条目
        String[] originalItems = originalStr.split(",");
        List<String> matchedItems = new ArrayList<>();

        for (String item : originalItems) {
            // 按冒号分割，提取第二个元素（分数）
            String[] parts = item.split(":");
            if (parts.length >= 2) { // 确保格式正确
                try {
                    double itemScore = Double.parseDouble(parts[1].trim());
                    // 判断该分数是否在目标列表中，是则保留
                    if (targetScores.contains(itemScore)) {
                        matchedItems.add(item);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("忽略格式错误的条目：" + item);
                }
            }
        }

        // 步骤3：拼接筛选后的结果
        String finalResult = String.join(",", matchedItems);

        // 输出最终结果
        System.out.println("\n过滤后的最终字符串：");
        System.out.println(finalResult);
        return  finalResult;
    }



    /**
     * 核心方法：先过滤分数，再按序号去重
     * @param str1 原始数据字符串
     * @param str2 过滤条件字符串
     * @return 过滤+去重后的结果字符串
     */
    public static String filterAndDeduplicateStrings(String str1, String str2) {
        // 1. 分割字符串2，获取各数组的过滤分数列表
        String[] str2Parts = str2.split(";");
        List<List<Double>> filterScores = new ArrayList<>();
        for (String part : str2Parts) {
            List<Double> scores = Arrays.stream(part.split(","))
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
            filterScores.add(scores);
        }

        // 2. 分割字符串1，先过滤分数，再按序号去重（用Map保证序号唯一，key=序号，value=完整数据项）
        String[] str1Items = str1.split(",");
        Map<String, String> uniqueItems = new HashMap<>(); // key: 冒号第一个数字（序号），value: 完整数据项

        for (String item : str1Items) {
            // 分割每条数据：序号:分数:数组标识
            String[] itemParts = item.split(":");
            if (itemParts.length != 3) {
                continue; // 跳过格式错误的数据
            }

            try {
                String serialNumber = itemParts[0]; // 冒号第一个数字（序号）
                double score = Double.parseDouble(itemParts[1]); // 分数
                int arrayIndex = Integer.parseInt(itemParts[2]); // 数组标识

                // 第一步：过滤分数（符合条件才继续）
                if (arrayIndex < filterScores.size() && filterScores.get(arrayIndex).contains(score)) {
                    // 第二步：按序号去重（Map的key唯一，已存在则不覆盖，保留最先出现的）
                    uniqueItems.putIfAbsent(serialNumber, item);
                }
            } catch (NumberFormatException e) {
                // 数字格式错误时跳过该条数据
                continue;
            }
        }

        // 3. 将去重后的结果拼接成字符串返回（保持原始顺序）
        // 注：Java 8+ HashMap默认保留插入顺序，如需严格按原始序号顺序，可改用LinkedHashMap（效果一致）
        return String.join(",", uniqueItems.values());
    }


    public static void main(String[] args) {


        // 1. 原始字符串（去重后的目标字符串）
        String originalStr = "1:8.0:0,2:25.0:0,3:5.0:0,4:8.0:0,5:12.0:0,6:6.0:0,7:10.0:0,8:7.0:0,9:9.0:0,10:12.0:0,1:7.0:1,2:25.0:1,3:6.0:1,4:5.0:1,5:12.0:1,6:5.0:1,7:8.0:1,8:8.0:1,9:10.0:1,10:12.0:1";
        // 2. 过滤分数的字符串（需要保留的分数列表）
        String filterScoreStr = "9.0;5.0,8.0";

//        String aa = filterStrings(originalStr,filterScoreStr);
//        System.out.println(aa);
        String aa2 = filterAndDeduplicateStrings(originalStr,filterScoreStr);
        System.out.println(aa2);
    }

    /**
     * rich-text 不支持 font 标签，转为 span style
     */
    public static String fontToSpan(String html) {
        if (StringUtils.isEmpty(html)) {
            return "";
        }
        return html
                .replace("<font color='red' >", "<span style=\"color:red;\">")
                .replace("<font color='red'>", "<span style=\"color:red;\">")
                .replace("<font color='blue'>", "<span style=\"color:blue;\">")
                .replace("<font>", "<span>")
                .replace("</font>", "</span>");
    }

    public static String spanRed(String text) {
        if (text == null) {
            text = "";
        }
        return "<span style=\"color:red;\">" + text + "</span>";
    }

    public static String spanBlue(String text) {
        if (text == null) {
            text = "";
        }
        return "<span style=\"color:blue;\">" + text + "</span>";
    }

    private static String formatMoney(String value) {
        return string3double(valueIsEmpty(value) ? "0" : value);
    }

    /**
     * 与后台 log.warn("支付 ...") 一致的展示格式
     */
    public static String formatPayDisplayHtml(String planno, String lushu, String playnum, String playNumScore,
                                             String xianzhicishu, String cishu, String initmoneyBefore,
                                             String payMoney, String balance) {
        return "支付 "
                + spanBlue("期数:" + emptyToBlank(planno)) + " "
                + spanRed("路数:" + emptyToBlank(lushu)) + " "
                + spanRed("号码:" + emptyToBlank(playnum)) + " "
                + spanBlue("分数:" + emptyToBlank(playNumScore)) + " "
                + spanRed("限制次数:" + emptyToBlank(xianzhicishu)) + " "
                + spanRed("投注次数:" + emptyToBlank(cishu)) + " "
                + spanBlue("投注前金额:" + formatMoney(initmoneyBefore)) + " "
                + spanRed("投注金额:" + formatMoney(payMoney)) + " "
                + spanBlue("余额:" + formatMoney(balance));
    }

    /**
     * 与后台 log.error("赢了 ...") 一致的展示格式
     */
    public static String formatWinDisplayHtml(String planno, String playNo, String oldnum, String numberScore,
                                              String cishu, String initmoneyBefore, String winMoney, String balance) {
        return "赢了 "
                + spanBlue("期数:" + emptyToBlank(planno)) + " "
                + spanRed("路数:" + emptyToBlank(playNo)) + " "
                + spanRed("投注号码:" + emptyToBlank(oldnum)) + " "
                + spanBlue("投注分数:" + emptyToBlank(numberScore)) + " "
                + spanRed("投注次数:" + emptyToBlank(cishu)) + " "
                + spanBlue("投注前金额:" + formatMoney(initmoneyBefore)) + " "
                + spanRed("挣到金额:" + formatMoney(winMoney)) + " "
                + spanBlue("余额:" + formatMoney(balance));
    }

    /**
     * 解析投注项 号码:分数[:组]
     */
    public static String[] parseBetItem(String item) {
        if (valueIsEmpty(item)) {
            return null;
        }
        String[] parts = item.split(":");
        if (parts.length < 2 || valueIsEmpty(parts[0]) || valueIsEmpty(parts[1])) {
            return null;
        }
        return parts;
    }

    public static String emptyToBlank(String value) {
        return valueIsEmpty(value) ? "" : value;
    }

}
