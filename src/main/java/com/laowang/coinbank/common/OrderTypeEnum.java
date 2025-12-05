package com.laowang.coinbank.common;

public enum OrderTypeEnum {
    OUT(-2, "已过期"),
    CANCEL(-1, "已取消"),
    NON(0, "未支付"),
    PAY(1, "已支付");


    OrderTypeEnum(int code, String value) {
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
