package com.laowang.coinbank.common;

public enum SymbolTypeEnum {
    TRX(0, "TRX"),
    USDT(1, "USDT");

    SymbolTypeEnum(int code, String value) {
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
