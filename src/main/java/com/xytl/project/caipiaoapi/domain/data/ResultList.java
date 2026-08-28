package com.xytl.project.caipiaoapi.domain.data;

import lombok.Data;

import java.util.List;

@Data
public class ResultList {
    private Integer code;
    private String msg;
    private String sign;
    private List<DataSourceItem> data;


    public boolean isSuccess() {
        return code != null && code.intValue() == 0;
    }
    public boolean isTokenError() {
        return msg != null && msg.indexOf("invalid")>0;
    }

    // 这里省略了getters和setters方法

    // DataItem类定义在Response类内部
    @Data
    public static class DataSourceItem {
        public String issue;
        public String code;
        public String codeStyle;
        public String saleStartTime;
        // 这里省略了getters和setters方法
    }
}
