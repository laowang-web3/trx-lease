package com.laowang.coinbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "tron")
public class TronProperties {
    private String walletAddress;

    private String hexPrivateKey;

    private String apiKey;

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getHexPrivateKey() {
        return hexPrivateKey;
    }

    public void setHexPrivateKey(String hexPrivateKey) {
        this.hexPrivateKey = hexPrivateKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
