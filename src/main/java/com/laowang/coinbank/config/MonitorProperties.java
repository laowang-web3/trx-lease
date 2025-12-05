package com.laowang.coinbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {
    private Boolean online;
    private String trc20UsdtAddress;
    private String grpcHost;
    private Integer grpcHostPort;
    private Integer grpcHostSolidityPort;
    private Integer monitorInterval;

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public String getTrc20UsdtAddress() {
        return trc20UsdtAddress;
    }

    public void setTrc20UsdtAddress(String trc20UsdtAddress) {
        this.trc20UsdtAddress = trc20UsdtAddress;
    }

    public String getGrpcHost() {
        return grpcHost;
    }

    public void setGrpcHost(String grpcHost) {
        this.grpcHost = grpcHost;
    }

    public Integer getGrpcHostPort() {
        return grpcHostPort;
    }

    public void setGrpcHostPort(Integer grpcHostPort) {
        this.grpcHostPort = grpcHostPort;
    }

    public Integer getGrpcHostSolidityPort() {
        return grpcHostSolidityPort;
    }

    public void setGrpcHostSolidityPort(Integer grpcHostSolidityPort) {
        this.grpcHostSolidityPort = grpcHostSolidityPort;
    }

    public Integer getMonitorInterval() {
        return monitorInterval;
    }

    public void setMonitorInterval(Integer monitorInterval) {
        this.monitorInterval = monitorInterval;
    }
}
