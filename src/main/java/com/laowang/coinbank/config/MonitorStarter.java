package com.laowang.coinbank.config;

import com.laowang.coinbank.bot.TelegramBot;
import com.laowang.coinbank.service.TronTransactionMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * 区块链监控器启动
 */
@Component
@Slf4j
public class MonitorStarter implements ApplicationRunner {

    @Autowired
    private TelegramBot telegramBot;
    @Autowired
    private TronTransactionMonitor tronTransactionMonitor;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramBot);
        log.info("Bot 已启动，配置参数已加载");

        tronTransactionMonitor.startMonitoring();
    }
}
