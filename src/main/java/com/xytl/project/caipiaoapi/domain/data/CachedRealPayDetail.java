package com.xytl.project.caipiaoapi.domain.data;

import lombok.Data;

@Data
public class CachedRealPayDetail {
    private String playNo;
    private int betCount;
    private boolean success;
    private String response;
}
