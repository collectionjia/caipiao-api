package com.xytl.project.caipiaoapi.domain.data;
import java.util.List;

import lombok.Data;

@Data
public class NowCaiPiaoResponse {
    private Integer code;
    private String msg;
    private Object sign;
    private List<DataItem> data;

    public boolean isSuccess() {
        return code != null && code == 0;
    }
    @Data
    public static class DataItem {
        private int ticketId;
        private int sale;
        private long startTime;
        private long endTime;
        private String planId;
        private int isLow;
        private String ticketName;
        private boolean defaultOpen;
        private int advanceStopBetTime;

    }
}

