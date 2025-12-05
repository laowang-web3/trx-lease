package com.laowang.coinbank.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExchangeOrder {

    public ExchangeOrder(String txId1, String symbol1, String fromAddress1, String toAddress1, BigDecimal trxNum1){
        txId=txId1;
        symbol=symbol1;
        fromAddress=fromAddress1;
        toAddress=toAddress1;
        trxNum=trxNum1;
        createDate = LocalDateTime.now();
    }


    public String txId;

    public String symbol;

    public String fromAddress;

    public String toAddress;

    public BigDecimal trxNum;

    public LocalDateTime createDate;

}
