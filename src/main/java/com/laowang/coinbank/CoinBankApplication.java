package com.laowang.coinbank;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
@EnableAsync
@EnableScheduling
@MapperScan("com.laowang.coinbank.order.dao")
@SpringBootApplication
public class CoinBankApplication {

    public static void main(String[] args) throws TelegramApiException {
        SpringApplication.run(CoinBankApplication.class, args);
    }

}
