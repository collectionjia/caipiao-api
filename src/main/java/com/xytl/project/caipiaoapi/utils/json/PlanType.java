package com.xytl.project.caipiaoapi.utils.json;

public class PlanType {

    /**
     * 通过次数控制
     * @param countNotWinCountstrflag
     * @return
     */
    public static int  suanfa1Count(String countNotWinCountstrflag,String numberflag,String beforeNumber){
        int countNotWinCountTotal=0;
        if(MyStringUtils.valueIsNotEmpty(beforeNumber) && MyStringUtils.valueIsNotEmpty(numberflag)){
            String nowscore=numberflag.split(":")[1];
            String beforerscore=beforeNumber.split(":")[1];
            if(Double.parseDouble(nowscore)<Double.parseDouble(beforerscore)){
                countNotWinCountstrflag="0";
            }
        }
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstrflag)+1;
        return countNotWinCountTotal;
    }


    /**
     * 倍率为1的
     * @param
     * @return
     */
    public static int[]  suanfa1(String countNotWinCountstr){
        int paymoneyTotal=0,countNotWinCountTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        if(countNotWinCountTotal>=1 && countNotWinCountTotal<=3){
            paymoneyTotal=1;
        }else if(countNotWinCountTotal>=4 && countNotWinCountTotal<=6){
            paymoneyTotal=2;
        }else if(countNotWinCountTotal>=7 && countNotWinCountTotal<=9){
            paymoneyTotal=3;
        }else if(countNotWinCountTotal>=10 && countNotWinCountTotal<=12){
            paymoneyTotal=5;
        }else if(countNotWinCountTotal>=13 && countNotWinCountTotal<=15){
            paymoneyTotal=8;
        }else if(countNotWinCountTotal>=16 && countNotWinCountTotal<=18){
            paymoneyTotal=10;
        }else if(countNotWinCountTotal>=19 && countNotWinCountTotal<=21){
            paymoneyTotal=15;
        }else if(countNotWinCountTotal>=22 && countNotWinCountTotal<=24){
            paymoneyTotal=25;
        }else if(countNotWinCountTotal>=25 && countNotWinCountTotal<=27){
            paymoneyTotal=40;
        }else if(countNotWinCountTotal>=28 && countNotWinCountTotal<=30){
            paymoneyTotal=55;
        }else if(countNotWinCountTotal>=31 && countNotWinCountTotal<=33){
            paymoneyTotal=75;
        }else if(countNotWinCountTotal>=34 && countNotWinCountTotal<=36){
            paymoneyTotal=115;
        }else if(countNotWinCountTotal>=37 && countNotWinCountTotal<=39){
            paymoneyTotal=175;
        }else if(countNotWinCountTotal>=40 && countNotWinCountTotal<=42){
            paymoneyTotal=255;
        }else if(countNotWinCountTotal>=43 && countNotWinCountTotal<=45){
            paymoneyTotal=355;
        }else if(countNotWinCountTotal>=46 && countNotWinCountTotal<=48){
            paymoneyTotal=500;
        }else if(countNotWinCountTotal>=49 && countNotWinCountTotal<=51){
            paymoneyTotal=750;
        }else if(countNotWinCountTotal>=52 && countNotWinCountTotal<=54){
            paymoneyTotal=1100;
        }else if(countNotWinCountTotal>=55 && countNotWinCountTotal<=57){
            paymoneyTotal=1600;
        }else if(countNotWinCountTotal>=58 && countNotWinCountTotal<=60){
            paymoneyTotal=2300;
        }else if(countNotWinCountTotal>=61 && countNotWinCountTotal<=63){
            paymoneyTotal=3300;
        }else if(countNotWinCountTotal>=64 && countNotWinCountTotal<=66){
            paymoneyTotal=4700;
        }else if(countNotWinCountTotal>=67 && countNotWinCountTotal<=69){
            paymoneyTotal=6700;
        }else if(countNotWinCountTotal>=70 && countNotWinCountTotal<=72){
            paymoneyTotal=9600;
        }else if(countNotWinCountTotal>=73 && countNotWinCountTotal<=75){
            paymoneyTotal=14000;
        }else if(countNotWinCountTotal>=76 && countNotWinCountTotal<=78){
            paymoneyTotal=20000;
        }else if(countNotWinCountTotal>=79 && countNotWinCountTotal<=81){
            paymoneyTotal=29000;
        }else{
            paymoneyTotal=1;
            countNotWinCountTotal=1;
        }
        int[] moneypay=new int[2];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        return moneypay;
    }



    /**
     * 倍率为3的
     * @param
     * @return
     */
    public static  int[]  suanfa3(String countNotWinCountstr,String countZuStr){
        int paymoneyTotal=0,countNotWinCountTotal=0,countZuStrTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        countZuStrTotal=Integer.parseInt(countZuStr)+1;
        if(countNotWinCountTotal>=1 && countNotWinCountTotal<=10){
            paymoneyTotal=1;
        }else if(countNotWinCountTotal>=11 && countNotWinCountTotal<=20){
            paymoneyTotal=2;
        }else{
            paymoneyTotal=1;
        }
        int[] moneypay=new int[3];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        moneypay[2]=countZuStrTotal;
        return moneypay;
    }

    /**
     * 倍率为1的
     * @param
     * @return
     */
    public static int[]  suanfa1(String countNotWinCountstr,String countZuStr){
        int paymoneyTotal=0,countNotWinCountTotal=0,countZuStrTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        countZuStrTotal=Integer.parseInt(countZuStr)+1;
        if(countNotWinCountTotal>=1 && countNotWinCountTotal<=3){
            paymoneyTotal=1;
        }else if(countNotWinCountTotal>=4 && countNotWinCountTotal<=6){
            paymoneyTotal=2;
        }else if(countNotWinCountTotal>=7 && countNotWinCountTotal<=9){
            paymoneyTotal=3;
        }else if(countNotWinCountTotal>=10 && countNotWinCountTotal<=12){
            paymoneyTotal=5;
        }else if(countNotWinCountTotal>=13 && countNotWinCountTotal<=15){
            paymoneyTotal=8;
        }else if(countNotWinCountTotal>=16 && countNotWinCountTotal<=18){
            paymoneyTotal=10;
        }else if(countNotWinCountTotal>=19 && countNotWinCountTotal<=21){
            paymoneyTotal=15;
        }else if(countNotWinCountTotal>=22 && countNotWinCountTotal<=24){
            paymoneyTotal=25;
        }else if(countNotWinCountTotal>=25 && countNotWinCountTotal<=27){
            paymoneyTotal=40;
        }else if(countNotWinCountTotal>=28 && countNotWinCountTotal<=30){
            paymoneyTotal=55;
        }else if(countNotWinCountTotal>=31 && countNotWinCountTotal<=33){
            paymoneyTotal=75;
        }else if(countNotWinCountTotal>=34 && countNotWinCountTotal<=36){
            paymoneyTotal=115;
        }else if(countNotWinCountTotal>=37 && countNotWinCountTotal<=39){
            paymoneyTotal=175;
        }else if(countNotWinCountTotal>=40 && countNotWinCountTotal<=42){
            paymoneyTotal=255;
        }else if(countNotWinCountTotal>=43 && countNotWinCountTotal<=45){
            paymoneyTotal=355;
        }else if(countNotWinCountTotal>=46 && countNotWinCountTotal<=48){
            paymoneyTotal=500;
        }else if(countNotWinCountTotal>=49 && countNotWinCountTotal<=51){
            paymoneyTotal=750;
        }else if(countNotWinCountTotal>=52 && countNotWinCountTotal<=54){
            paymoneyTotal=1100;
        }else if(countNotWinCountTotal>=55 && countNotWinCountTotal<=57){
            paymoneyTotal=1600;
        }else if(countNotWinCountTotal>=58 && countNotWinCountTotal<=60){
            paymoneyTotal=2300;
        }else if(countNotWinCountTotal>=61 && countNotWinCountTotal<=63){
            paymoneyTotal=3300;
        }else if(countNotWinCountTotal>=64 && countNotWinCountTotal<=66){
            paymoneyTotal=4700;
        }else if(countNotWinCountTotal>=67 && countNotWinCountTotal<=69){
            paymoneyTotal=6700;
        }else if(countNotWinCountTotal>=70 && countNotWinCountTotal<=72){
            paymoneyTotal=9600;
        }else if(countNotWinCountTotal>=73 && countNotWinCountTotal<=75){
            paymoneyTotal=14000;
        }else if(countNotWinCountTotal>=76 && countNotWinCountTotal<=78){
            paymoneyTotal=20000;
        }else if(countNotWinCountTotal>=79 && countNotWinCountTotal<=81){
            paymoneyTotal=29000;
        }else{
            paymoneyTotal=1;
            countNotWinCountTotal=1;
        }
        int[] moneypay=new int[3];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        moneypay[2]=countZuStrTotal;
        return moneypay;
    }


    /**
     * 倍率为5的
     * @param
     * @return
     */
    public static  int[]  suanfa5(String countNotWinCountstr){
        int paymoneyTotal=0;
        int countNotWinCountTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        if(countNotWinCountTotal>=1 && countNotWinCountTotal<=3){
            paymoneyTotal=10;
        }else if(countNotWinCountTotal>=4 && countNotWinCountTotal<=6){
            paymoneyTotal=15;
        }else if(countNotWinCountTotal>=7 && countNotWinCountTotal<=9){
            paymoneyTotal=25;
        }else if(countNotWinCountTotal>=10 && countNotWinCountTotal<=12){
            paymoneyTotal=40;
        }else if(countNotWinCountTotal>=13 && countNotWinCountTotal<=15){
            paymoneyTotal=60;
        }else if(countNotWinCountTotal>=16 && countNotWinCountTotal<=18){
            paymoneyTotal=85;
        }else if(countNotWinCountTotal>=19 && countNotWinCountTotal<=21){
            paymoneyTotal=115;
        }else if(countNotWinCountTotal>=22 && countNotWinCountTotal<=24){
            paymoneyTotal=155;
        }else if(countNotWinCountTotal>=25 && countNotWinCountTotal<=27){
            paymoneyTotal=220;
        }else if(countNotWinCountTotal>=28 && countNotWinCountTotal<=30){
            paymoneyTotal=320;
        }else if(countNotWinCountTotal>=31 && countNotWinCountTotal<=33){
            paymoneyTotal=460;
        }else if(countNotWinCountTotal>=34 && countNotWinCountTotal<=36){
            paymoneyTotal=660;
        }else if(countNotWinCountTotal>=37 && countNotWinCountTotal<=39){
            paymoneyTotal=940;
        }else if(countNotWinCountTotal>=40 && countNotWinCountTotal<=42){
            paymoneyTotal=1340;
        }else if(countNotWinCountTotal>=43 && countNotWinCountTotal<=45){
            paymoneyTotal=1940;
        }else if(countNotWinCountTotal>=46 && countNotWinCountTotal<=48){
            paymoneyTotal=2740;
        }else{
            paymoneyTotal=1;
        }
        int[] moneypay=new int[2];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        return moneypay;
    }




    /**
     * 第三方案改写
     * @param
     * @return
     */
    public static  int[]  suanfa6(String countNotWinCountstr,String countZuStr){
        int paymoneyTotal=0,countNotWinCountTotal=0,countZuStrTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        countZuStrTotal=Integer.parseInt(countZuStr)+1;
        if(countNotWinCountTotal>=1 && countNotWinCountTotal<10){
            paymoneyTotal=1;
        }else if(countNotWinCountTotal>=10 && countNotWinCountTotal<=15){
            paymoneyTotal=3;
        }else{
            paymoneyTotal=1;
            countZuStrTotal=0;
            countNotWinCountTotal=0;
        }
        int[] moneypay=new int[3];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        moneypay[2]=countZuStrTotal;
        return moneypay;
    }

    /**
     * 倍率为1的
     * @param
     * @return
     */
    public static int[]  suanfa1plus(String countNotWinCountstr,String countZuStr){
        int paymoneyTotal=0,countNotWinCountTotal=0,countZuStrTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        countZuStrTotal=Integer.parseInt(countZuStr)+1;
        if(countNotWinCountTotal>=1 && countNotWinCountTotal<=3){
            paymoneyTotal=5;
        }else if(countNotWinCountTotal>=4 && countNotWinCountTotal<=6){
            paymoneyTotal=1;
        }else if(countNotWinCountTotal>=7 && countNotWinCountTotal<=9){
            paymoneyTotal=2;
        }else if(countNotWinCountTotal>=10 && countNotWinCountTotal<=12){
            paymoneyTotal=3;
        }else if(countNotWinCountTotal>=13 && countNotWinCountTotal<=15){
            paymoneyTotal=5;
        }else if(countNotWinCountTotal>=16 && countNotWinCountTotal<=18){
            paymoneyTotal=8;
        }else if(countNotWinCountTotal>=19 && countNotWinCountTotal<=21){
            paymoneyTotal=10;
        }else if(countNotWinCountTotal>=22 && countNotWinCountTotal<=24){
            paymoneyTotal=15;
        }else if(countNotWinCountTotal>=25 && countNotWinCountTotal<=27){
            paymoneyTotal=25;
        }else if(countNotWinCountTotal>=28 && countNotWinCountTotal<=30){
            paymoneyTotal=40;
        }else if(countNotWinCountTotal>=31 && countNotWinCountTotal<=33){
            paymoneyTotal=55;
        }else if(countNotWinCountTotal>=34 && countNotWinCountTotal<=36){
            paymoneyTotal=75;
        }else if(countNotWinCountTotal>=37 && countNotWinCountTotal<=39){
            paymoneyTotal=115;
        }else if(countNotWinCountTotal>=40 && countNotWinCountTotal<=42){
            paymoneyTotal=175;
        }else if(countNotWinCountTotal>=43 && countNotWinCountTotal<=45){
            paymoneyTotal=255;
        }else if(countNotWinCountTotal>=46 && countNotWinCountTotal<=48){
            paymoneyTotal=355;
        }else if(countNotWinCountTotal>=49 && countNotWinCountTotal<=51){
            paymoneyTotal=500;
        }else if(countNotWinCountTotal>=52){
            paymoneyTotal=100;
        }else{
            paymoneyTotal=1;
            countZuStrTotal=1;
            countNotWinCountTotal=1;
        }
        int[] moneypay=new int[3];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        moneypay[2]=countZuStrTotal;
        return moneypay;
    }

    /**
     * 倍率为1的
     * @param
     * @return
     */
    public static int[]  suanfa2plus(String countNotWinCountstr,String countZuStr){
        int paymoneyTotal=0,countNotWinCountTotal=0,countZuStrTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        countZuStrTotal=Integer.parseInt(countZuStr)+1;
        if(countNotWinCountTotal>=1 && countNotWinCountTotal<=3){
            paymoneyTotal=1;
        }else if(countNotWinCountTotal>=4 && countNotWinCountTotal<=6){
            paymoneyTotal=2;
        }else if(countNotWinCountTotal>=7 && countNotWinCountTotal<=9){
            paymoneyTotal=3;
        }else if(countNotWinCountTotal>=10 && countNotWinCountTotal<=12){
            paymoneyTotal=5;
        }else if(countNotWinCountTotal>=13 && countNotWinCountTotal<=15){
            paymoneyTotal=8;
        }else if(countNotWinCountTotal>=16 && countNotWinCountTotal<=18){
            paymoneyTotal=10;
        }else if(countNotWinCountTotal>=19 && countNotWinCountTotal<=21){
            paymoneyTotal=15;
        }else if(countNotWinCountTotal>=22 && countNotWinCountTotal<=24){
            paymoneyTotal=25;
        }
//        else if(countNotWinCountTotal>=25 && countNotWinCountTotal<=27){
//            paymoneyTotal=115;
//        }else if(countNotWinCountTotal>=28 && countNotWinCountTotal<=30){
//            paymoneyTotal=175;
//        }
        else{
            paymoneyTotal=0;
//            countZuStrTotal=1;
//            countNotWinCountTotal=1;
        }
        int[] moneypay=new int[3];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        moneypay[2]=countZuStrTotal;
        return moneypay;
    }


    /**
     * 倍率为1的
     * @param
     * @return
     */
    public static int[]  suanfa3plus(String countNotWinCountstr,String countZuStr){
        int paymoneyTotal=0,countNotWinCountTotal=0,countZuStrTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        countZuStrTotal=Integer.parseInt(countZuStr)+1;
        if(countNotWinCountTotal>=1 && countNotWinCountTotal<=3){
            paymoneyTotal=1;
        }else if(countNotWinCountTotal>=4 && countNotWinCountTotal<=6){
            paymoneyTotal=2;
        }else if(countNotWinCountTotal>=7 && countNotWinCountTotal<=9){
            paymoneyTotal=3;
        }else if(countNotWinCountTotal>=10 && countNotWinCountTotal<=12){
            paymoneyTotal=5;
        }else if(countNotWinCountTotal>=13 && countNotWinCountTotal<=15){
            paymoneyTotal=8;
        }else{
            paymoneyTotal=0;
//            countZuStrTotal=1;
//            countNotWinCountTotal=1;
        }
        int[] moneypay=new int[3];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        moneypay[2]=countZuStrTotal;
        return moneypay;
    }


    /**
     * 倍率为1的
     * @param
     * @return
     */
    public static int[]  suanfa4plus(String countNotWinCountstr,String countZuStr){
        int paymoneyTotal=0,countNotWinCountTotal=0,countZuStrTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        countZuStrTotal=Integer.parseInt(countZuStr)+1;
        paymoneyTotal=1;
//        if(countNotWinCountTotal<=20){
//            paymoneyTotal=1;
//        }else{
//            paymoneyTotal=0;
//        }
//        else if(countNotWinCountTotal>9 && countNotWinCountTotal<=19){
//            paymoneyTotal=2;
//        }else{
//            paymoneyTotal=1;
//            countZuStrTotal=1;
//            countNotWinCountTotal=1;
//        }
        int[] moneypay=new int[3];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        moneypay[2]=countZuStrTotal;
        return moneypay;
    }




    /**
     * 倍率为1的
     * @param
     * @return
     */
    public static int[]  suanfa3plus2(String countNotWinCountstr,String countZuStr){
        int paymoneyTotal=0,countNotWinCountTotal=0,countZuStrTotal=0;
        countNotWinCountTotal=Integer.parseInt(countNotWinCountstr)+1;
        countZuStrTotal=Integer.parseInt(countZuStr)+1;
        if(countNotWinCountTotal>=1 && countNotWinCountTotal<=9){
            paymoneyTotal=2;
        }else if(countNotWinCountTotal>=10 && countNotWinCountTotal<=13){
            paymoneyTotal=4;
        }else if(countNotWinCountTotal>=14 && countNotWinCountTotal<=49){
            paymoneyTotal=0;
        }else if(countNotWinCountTotal>=50 && countNotWinCountTotal<=52){
            paymoneyTotal=10;
        }else if(countNotWinCountTotal>=53 && countNotWinCountTotal<=55){
            paymoneyTotal=20;
        }else if(countNotWinCountTotal>=56 && countNotWinCountTotal<=58){
            paymoneyTotal=30;
        }else if(countNotWinCountTotal>=59 && countNotWinCountTotal<=61){
            paymoneyTotal=50;
        }else if(countNotWinCountTotal>=62 && countNotWinCountTotal<=64){
            paymoneyTotal=80;
        }else if(countNotWinCountTotal>=65 && countNotWinCountTotal<=67){
            paymoneyTotal=100;
        }else if(countNotWinCountTotal>=68 && countNotWinCountTotal<=70){
            paymoneyTotal=150;
        }else if(countNotWinCountTotal>=70 && countNotWinCountTotal<=100){
            paymoneyTotal=100;
        }else{
            paymoneyTotal=1;
            countZuStrTotal=0;
            countNotWinCountTotal=0;
        }
        int[] moneypay=new int[3];
        moneypay[0]=paymoneyTotal;
        moneypay[1]=countNotWinCountTotal;
        moneypay[2]=countZuStrTotal;
        return moneypay;
    }



}
