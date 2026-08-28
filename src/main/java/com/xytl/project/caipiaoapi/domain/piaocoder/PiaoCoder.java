package com.xytl.project.caipiaoapi.domain.piaocoder;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("piao_coder")
public class PiaoCoder {
    @TableId(value="id", type = IdType.AUTO)
    @Schema(title = "ID")
    private Long id;
    @TableField("type")
    @Schema(title = "类型")
    private int type;
    @Schema(title = "期数")
    @TableField("issue")
    private String issue;
    @Schema(title = "开奖号码")
    @TableField("code")
    private String code;
    @Schema(title = "开奖日期")
    @TableField("sale_start_time")
    private String saleStartTime;
    @Schema(title = "创建时间")
    @TableField("create_time")
    private Date createTime;
    
}