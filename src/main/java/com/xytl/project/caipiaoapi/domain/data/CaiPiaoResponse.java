package com.xytl.project.caipiaoapi.domain.data;
import java.util.List;

import lombok.Data;

@Data
public class CaiPiaoResponse {
    private Integer code;
    private String msg;
    private String sign;
    private List<DataItem> data;
    
    
    public boolean isSuccess() {
        return code != null && code.intValue() == 0;
    }
    public boolean isTokenError() {
        return msg != null && msg.indexOf("invalid")>0;
    }

    // 这里省略了getters和setters方法

    // DataItem类定义在Response类内部
    @Data
    public static class DataItem {
        private String issue;
        private String code;
        private String codeStyle;
        private String saleStartTime;

        // 这里省略了getters和setters方法
    }
}