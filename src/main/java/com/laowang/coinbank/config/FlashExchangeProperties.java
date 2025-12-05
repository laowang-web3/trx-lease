package com.laowang.coinbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "flashexchange")
public class FlashExchangeProperties {
    private BigDecimal profit;

    public BigDecimal getProfit() {
        return profit.setScale(2,BigDecimal.ROUND_DOWN);
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }
}
