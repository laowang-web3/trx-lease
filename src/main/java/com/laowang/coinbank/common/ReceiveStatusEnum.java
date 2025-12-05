package com.laowang.coinbank.common;

public enum ReceiveStatusEnum {
    NON(0, "未处理"),
    DEAL(1, "已处理");


    ReceiveStatusEnum(int code, String value) {
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
