package com.xytl.project.caipiaoapi.domain.data;

import lombok.Data;

import java.util.List;

@Data
public class BanlanceResult {
    private Integer code;
    private List<DataSourceItem> data;


    public boolean isSuccess() {
        return code != null && code.intValue() == 0;
    }

    // 这里省略了getters和setters方法

    // DataItem类定义在Response类内部
    @Data
    public static class DataSourceItem {
        public String balance;
        // 这里省略了getters和setters方法
    }
}
