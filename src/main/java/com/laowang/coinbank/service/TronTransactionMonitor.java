package com.laowang.coinbank.service;
import com.laowang.coinbank.config.MonitorProperties;
import com.laowang.coinbank.config.TelegramBotProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tron.trident.api.GrpcAPI;
import org.tron.trident.api.WalletGrpc;
import org.tron.trident.proto.Chain;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TronTransactionMonitor {
    // 注入配置类
    @Autowired
    private MonitorProperties monitorProperties;
    @Autowired
    private TelegramBotProperties telegramBotProperties;

    private  WalletGrpc.WalletBlockingStub blockingStub;
    private long lastBlockHeight = 0;
    @Autowired()
    @Lazy
    private MonitorService monitorService;


    public void startMonitoring() throws InterruptedException {
        if(null == blockingStub){
            ManagedChannel channel = ManagedChannelBuilder.forAddress(monitorProperties.getGrpcHost(), monitorProperties.getGrpcHostPort())
                    .usePlaintext()
                    .build();
            this.blockingStub = WalletGrpc.newBlockingStub(channel);
        }
        log.info("开始监控TRON转账交易...");
        log.info("监控地址: " + telegramBotProperties.getCollectMoneyAddress());

        // 获取初始区块高度
        lastBlockHeight = getCurrentBlockHeight() - 10; // 从稍早的区块开始
        log.info("开始监控的区块高度: " + lastBlockHeight);

        while (true) {
            long currentHeight = getCurrentBlockHeight();
            log.info("最新区块高度"+currentHeight);
            log.info("---------lastBlockHeight---"+lastBlockHeight);
            if (currentHeight > lastBlockHeight) {
                // 处理新产生的区块
                for (long i = lastBlockHeight + 1; i <= currentHeight; i++) {
                    long finalI = i;
                    // 获取当前线程对象
                    monitorService.processBlock(blockingStub,finalI);

                }
                lastBlockHeight = currentHeight -3;
            }

            // 等待下一次检查
            TimeUnit.MILLISECONDS.sleep(monitorProperties.getMonitorInterval());
        }
    }

    private long getCurrentBlockHeight() {
        Chain.Block nowBlock = blockingStub.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        return nowBlock.getBlockHeader().getRawData().getNumber();
    }
}
