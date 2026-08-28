package com.xytl.project.caipiaoapi.domain.data;

import lombok.Data;

import java.util.List;

@Data
public class InsertNum {

    public int ticketId;
    public String planNo;
    public List<DataItem> datalist;


    // DataItem类定义在Response类内部
    @Data
    public static class DataItem {
        private String playId;
        private String betNum;
        private String betAmount;
        private String betCount;
        private String content;

    }

}
