package com.xytl.project.caipiaoapi.domain.piaocoder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CodeStatItem {
    @Schema(title = "数字")
    private int number;
    @Schema(title = "近100期出现次数")
    private int times;
    @Schema(title = "已出现概率%")
    private int rate;
    @Schema(title = "下期可能出现概率%")
    private int willRate;
    
}
