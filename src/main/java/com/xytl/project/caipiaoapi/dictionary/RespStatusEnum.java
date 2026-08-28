package com.xytl.project.caipiaoapi.dictionary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
/**
 * @author hp
 */

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RespStatusEnum {
    /**
     * 返回状态码
     */
    Success(200, "成功"),
    UnLogin(401, "未登录"),
    OtherLogin(403, "其他设备登录"),
    Fail(101, "业务异常"),
    IllegalArgumentNull(102, "参数为空"),
    IllegalArgument(103, "参数异常"),
    SystemError(104, "系统异常"),
    TimeError(105, "连接已失效，请检查设备时间"),
    SignatureError(106, "参数签名不正确"),
    InvalidDevice(107, "设备异常"),
    PermissionDenied(108, "没有权限访问！"),
    NoAllowFolders(109, "非法访问"),
    RepeatSubmit(110, "重复请求");

    private final int value;
    private final String label;
    private final static Map<Integer, RespStatusEnum> CACHE_ITEMS;

    static {
        CACHE_ITEMS = new HashMap<>();
        List<RespStatusEnum> list = getEnumValues();
        for (RespStatusEnum en : list) {
            CACHE_ITEMS.put(en.getValue(), en);
        }
    }

    RespStatusEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }


    public int getValue() {
        return value;
    }


    public String getLabel() {
        return label;
    }

    public static RespStatusEnum parse(int value) {
        return CACHE_ITEMS.get(value);
    }

    public static List<RespStatusEnum> getEnumValues() {
        return Arrays.asList(values());
    }
}
