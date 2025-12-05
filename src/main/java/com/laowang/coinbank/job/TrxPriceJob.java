package com.laowang.coinbank.job;


import com.laowang.coinbank.service.OkxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrxPriceJob {

    @Autowired
    private OkxService okxService;
    /**
     * 每小时更新一下最新的trx价格
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void updateTrxPrice() {
        okxService.getTrxRecentPrice();
    }
}
