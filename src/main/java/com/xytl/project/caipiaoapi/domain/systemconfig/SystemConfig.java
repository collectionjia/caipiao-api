package com.xytl.project.caipiaoapi.domain.systemconfig;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName(value = "system_config")
public class SystemConfig implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId(value = "id",type= IdType.ASSIGN_ID)
	private Long id;

	@TableField(value="name")
	@Schema(title = "名称")
	private String name;

	@TableField(value="code")
	@Schema(title = "编码")
	private String code;

	@TableField(value="value")
	@Schema(title = "值")
	private String value;

	@TableField(value="remark")
	@Schema(title = "备注")
	private String remark;

	@TableField(value="create_time", fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private Date createTime;

	// 以下添加的是扩展字段需要标注 @TableField(exist = false)
}
