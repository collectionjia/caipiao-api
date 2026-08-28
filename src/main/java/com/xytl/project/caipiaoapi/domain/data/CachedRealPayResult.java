package com.xytl.project.caipiaoapi.domain.data;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CachedRealPayResult {
    private boolean success;
    private String message;
    private String planNo;
    private String cachedPlanNo;
    private int realBetCount;
    private int failCount;
    private String balance;
    private List<CachedRealPayDetail> details = new ArrayList<>();
}
