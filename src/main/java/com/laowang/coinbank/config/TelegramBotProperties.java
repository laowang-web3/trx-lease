package com.laowang.coinbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramBotProperties {
    private String token;
    private BigDecimal energyPrice;
    private String collectMoneyAddress;
    private String customerService;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public BigDecimal getEnergyPrice() {
        return energyPrice.setScale(1,BigDecimal.ROUND_UP);
    }

    public void setEnergyPrice(BigDecimal energyPrice) {
        this.energyPrice = energyPrice;
    }

    public String getCollectMoneyAddress() {
        return collectMoneyAddress;
    }

    public void setCollectMoneyAddress(String collectMoneyAddress) {
        this.collectMoneyAddress = collectMoneyAddress;
    }

    public String getCustomerService() {
        return customerService;
    }

    public void setCustomerService(String customerService) {
        this.customerService = customerService;
    }
}
