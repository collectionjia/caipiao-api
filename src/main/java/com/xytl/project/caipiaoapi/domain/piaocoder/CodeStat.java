package com.xytl.project.caipiaoapi.domain.piaocoder;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CodeStat {

    @Schema(title = "位置")
    private int index;
    @Schema(title = "统计信息")
    private List<CodeStatItem> stat;
}
