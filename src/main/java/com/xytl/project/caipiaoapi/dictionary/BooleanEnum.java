package com.xytl.project.caipiaoapi.dictionary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum BooleanEnum {
    True(1, "是"), False(2, "否");

    private int value;
    private String label;
    private static Map<Integer, BooleanEnum> cacheItems;

    static {
        cacheItems = new HashMap<>();
        List<BooleanEnum> list = getEnumValues();
        for (BooleanEnum en : list) {
            cacheItems.put(en.getValue(), en);
        }
    }

    BooleanEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }


    public int getValue() {
        return value;
    }


    public String getLabel() {
        return label;
    }

    public static BooleanEnum parse(int value) {
        return cacheItems.get(value);
    }

    public static List<BooleanEnum> getEnumValues() {
        return Arrays.asList(values());
    }
}
