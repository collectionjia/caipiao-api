package com.xytl.project.caipiaoapi.domain.questions;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "Questions")
@TableName(value = "questions")
public class Questions implements Serializable{
	private static final long serialVersionUID = 1L;

	@TableId(value = "id",type= IdType.AUTO)
	@Schema(description = "主建")
	private Integer id;

	@TableField(value="question")
	@Schema(description = "问题")
	private String question;

	@TableField(value="answer")
	@Schema(description = "答案")
	private String answer;


	@TableField(value="ip")
	@Schema(description = "ip")
	private String ip;


	@TableField(value="dcinfo")
	@Schema(description = "设备信息")
	private String dcinfo;

	@TableField(value="typestr")
	@Schema(description = "用户来源")
	private String typestr;


}
