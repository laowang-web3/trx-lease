package com.laowang.coinbank.job;


import com.laowang.coinbank.order.service.ProductOrderService;
import com.laowang.coinbank.service.OkxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CheckOrderJob {

    @Autowired
    private OkxService okxService;

    @Autowired
    private ProductOrderService productOrderService;

    /**
     * 每分钟检查一下订单
     */
    @Scheduled(cron = "10 * * * * ?")
    public void updateTrxPrice() {
        productOrderService.clearOutTimeOrder();
    }
}
