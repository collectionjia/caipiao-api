package com.xytl.project.caipiaoapi.utils.json;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {


    public static void main(String[] args) {
        // 创建日期格式化对象，设置为秒
        SimpleDateFormat sdf = new SimpleDateFormat("ss");
        // 获取当前时间的秒数
        String seconds = sdf.format(new Date());
        // 打印结果
        System.out.println("当前时间的秒数：" + seconds);
    }


    public static void main3(String[] args) {

        // 定义长整型时间戳（假设单位为毫秒）
        long start_timestamp = 1728813650000L;
        long end_timestamp = 1728813710000L;
        String startstr=getDate(start_timestamp);
        String endstr=getDate(end_timestamp);
        System.out.println(startstr+"   获取到的时间   "+endstr);




    }

    public static String getDate(){
        // 获取当前日期
        LocalDate date = LocalDate.now();

        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // 按照指定格式输出日期
        String formattedDate = date.format(formatter);
        return formattedDate;
    }


    public static   String  getDate(long timestemp){
        // 将长整型时间戳转换为Date对象
        Date date = new Date(timestemp);

        // 创建SimpleDateFormat对象，并设置日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 设置时区为上海时区
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // 格式化日期
        String formattedDate = sdf.format(date);

        System.out.println("Formatted Date in Shanghai Timezone: " + formattedDate);
        return formattedDate;
    }

    /**
     * 获取当前时间的秒
     * @return
     */
    public static  int getNowSecond(){
        // 创建日期格式化对象，设置为秒
        SimpleDateFormat sdf = new SimpleDateFormat("ss");
        // 获取当前时间的秒数
        String seconds = sdf.format(new Date());
        // 打印结果
//        System.out.println("当前时间的秒数：" + seconds);
        return Integer.parseInt(seconds);
    }



}
