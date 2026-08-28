package com.xytl.project.caipiaoapi.domain.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OrderList {
    private Integer code;
    private String msg;
    private List<OrderItem> data;

    // DataItem类定义在Response类内部
    @Data
    public static class OrderItem {


        public String ticketPlanNo;

        public String ticketResult;
        // 这里省略了getters和setters方法
    }
}
