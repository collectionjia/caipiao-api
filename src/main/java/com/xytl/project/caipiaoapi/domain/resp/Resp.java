package com.xytl.project.caipiaoapi.domain.resp;

import org.apache.commons.lang3.StringUtils;

import com.xytl.project.caipiaoapi.dictionary.RespStatusEnum;
import com.xytl.project.caipiaoapi.utils.json.JSON;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 所有的api接口返回对象基类
 * 
 * @author hp
 *
 * @param <T>
 */
@Data
@Schema(title="返回值")
public class Resp<T> {

    @Schema(title = "消息编码")
    private String code;
    @Schema(title = "执行状态")
    private String message;
    @Schema(title = "状态码")
    private int status = 200;
    @Schema(title = "数据")
    private T data;
    
    @Schema(title = "失败数据")
    private ErrorData errorData;
    
    public int getCode() {
        return status;
    }
    
    /**
     * 初始化一个新创建的 Resp 对象，使其表示一个空消息。
     */
    public Resp() {
    }

    /**
     * 初始化一个新创建的 Resp 对象
     * 
     * @param status 状态码
     * @param msg    返回内容
     */
    public Resp(int status, String msg) {
        this.status = status;
        this.message = msg;
    }

    public Resp(int status, String msg, ErrorData error) {
        this.status = status;
        this.message = msg;
        this.errorData = error;
    }

    public Resp(int status, String msg, T result) {
        this.status = status;
        this.message = msg;
        this.data = result;
    }
    
    public Resp(int status, String msg, T result, Object error) {
        this.status = status;
        this.message = msg;
        this.data = result;
        this.errorData =ErrorData.from(error);
    }

    public Resp(int status, ErrorData error) {
        this.status = status;
        this.message = StringUtils.isNotEmpty(error.getMessage()) ? error.getMessage() : null;
        this.errorData = error;
    }

    /**
     * 初始化一个新创建的 Resp 对象
     * 
     * @param status 状态码
     * @param msg    返回内容
     */
    public Resp(T result) {
        this.status = RespStatusEnum.Success.getValue();
        this.data = result;
    }
    
    public Resp(RespStatusEnum en,  T result) {
        this.status = en.getValue();
        this.message = en.getLabel();
        this.data = result;
    }
    
    public Resp(RespStatusEnum en,  T result, ErrorData error) {
        this.status = en.getValue();
        this.message = en.getLabel();
        this.data = result;
        this.errorData =error;
    }

    public boolean isSuccess() {
        return RespStatusEnum.Success.getValue() == status;
    }
    /**
     * 返回成功消息
     * 
     * @return 成功消息
     */
    public static <T> Resp<T> success() {
        return Resp.successByMsg("操作成功");
    }

    /**
     * 返回成功数据
     * 
     * @return 成功消息
     */
    public static <T> Resp<T> successByData(T data) {
        return Resp.success("操作成功", data);
    }
    public static <T> Resp<T> okData(T data) {
    	return Resp.success("操作成功", data);
    }

    /**
     * 返回成功消息
     * 
     * @param msg 返回内容
     * @return 成功消息
     */
    public static <T> Resp<T> successByMsg(String msg) {
        return Resp.success(msg, null);
    }
    
    public static <T> Resp<T> okMsg(String msg) {
    	return Resp.success(msg, null);
    }

    /**
     * 返回成功消息
     * 
     * @param msg  返回内容
     * @param data 数据对象
     * @return 成功消息
     */
    public static <T> Resp<T> success(String msg, T data) {
        return new Resp<T>(RespStatusEnum.Success.getValue(), msg, data);
    }

    /**
     * 返回错误消息
     * 
     * @return
     */
    public static <T> Resp<T> error() {
        return Resp.errorByMsg("操作失败");
    }

    public static <T> Resp<T> error(RespStatusEnum en) {
        return new Resp<T>(en, null);
    }
    
    public static <T> Resp<T> error(RespStatusEnum en, Object error) {
        return new Resp<T>(en, null, ErrorData.from(error));
    }

    /**
     * 返回错误消息
     * 
     * @param msg 返回内容
     * @return 警告消息
     */
    public static <T> Resp<T> errorByMsg(String msg) {
        return new Resp<T>(RespStatusEnum.SystemError.getValue(), msg);
    }
    public static <T> Resp<T> failMsg(String msg) {
    	return new Resp<T>(RespStatusEnum.SystemError.getValue(), msg);
    }

    public static <T> Resp<T> errorByData(T error) {
        return new Resp<T>(RespStatusEnum.SystemError.getValue(), RespStatusEnum.SystemError.getLabel(), null, error);
    }

    public static <T> Resp<T> error(ErrorData error) {
        return new Resp<T>(RespStatusEnum.SystemError.getValue(), error);
    }

    /**
     * 返回错误消息
     * 
     * @param msg  返回内容
     * @param data 数据对象
     * @return 警告消息
     */
    public static <T> Resp<T> error(String msg, T data) {
        return new Resp<T>(RespStatusEnum.SystemError.getValue(), msg, data);
    }

    /**
     * 返回错误消息
     * 
     * @param status 状态码
     * @param msg    返回内容
     * @return 警告消息
     */
    public static <T> Resp<T> errorByMsg(int status, String msg) {
        return new Resp<T>(status, msg);
    }
    
    public String toJson() {
        return JSON.toJSONString(this, false);
    }
}
