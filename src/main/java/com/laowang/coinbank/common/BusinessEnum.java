package com.laowang.coinbank.common;

public enum BusinessEnum {
    COUNT(0, "笔数套餐"),
    PREMIUM (1, "飞机会员");

    BusinessEnum(int code, String value) {
        this.code = code;
        this.value = value;
    }
    private int code;
    private String value;
    public int getCode() {
        return code;
    }
    public String getValue() {
        return value;
    }
}
