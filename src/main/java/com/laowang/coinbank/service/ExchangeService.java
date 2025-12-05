package com.laowang.coinbank.service;

import com.laowang.coinbank.bo.ExchangeOrder;
import com.laowang.coinbank.common.SymbolTypeEnum;
import com.laowang.coinbank.config.TelegramBotProperties;
import com.laowang.coinbank.order.dao.entity.ReceiveOrderDO;
import com.laowang.coinbank.order.service.EnergyRentalService;
import com.laowang.coinbank.order.service.ProductOrderService;
import com.laowang.coinbank.order.service.ReceiveOrderService;
import com.laowang.coinbank.order.service.FlashExchangeService;
import com.laowang.coinbank.utils.TronUtils;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ExchangeService {
    @Autowired
    private EnergyRentalService energyRentalService;
    @Autowired
    private ProductOrderService productOrderService;
    @Autowired
    private FlashExchangeService transferOrderService;
    @Autowired
    private ReceiveOrderService receiveOrderService;

    private Map<String, ExchangeOrder> UsdtOrderMap = new ConcurrentHashMap();
    private Map<String, ExchangeOrder> TrxOrderMap = new ConcurrentHashMap();


    public void distribute(Long blockId,String txId, SymbolTypeEnum symbolType, String fromAddress, String toAddress, BigDecimal num) {
        if (SymbolTypeEnum.USDT.getValue().equals(symbolType.getValue())) {
            exchangeTrx(txId, fromAddress, toAddress, num);
        }
        if (SymbolTypeEnum.TRX.getValue().equals(symbolType.getValue())) {
            exchangeUsdt(txId, fromAddress, toAddress, num);
        }
    }

    public void exchangeTrx(String txId, String fromAddress, String toAddress, BigDecimal usdtNum) {
        if (!TrxOrderMap.containsKey(txId)) {
            ExchangeOrder order = new ExchangeOrder(txId, SymbolTypeEnum.USDT.getValue(), fromAddress, toAddress, usdtNum);
            TrxOrderMap.put(txId, order);
            ReceiveOrderDO receiveOrderDO = receiveOrderService.saveReceive(txId, usdtNum, SymbolTypeEnum.USDT, fromAddress, toAddress);
            //区分订单和闪兑(订单金额)分别处理
            //1、闪兑大于1u才处理
            if (null != usdtNum && usdtNum.compareTo(BigDecimal.ONE) >= 0) {
                if (!productOrderService.preOrder(txId, SymbolTypeEnum.USDT, receiveOrderDO, toAddress, usdtNum)) {
                    log.info("-----usdt闪兑处理逻辑-----------");
                    transferOrderService.exchange(receiveOrderDO,SymbolTypeEnum.TRX);
                } else {
                    //订单金额处理逻辑
                    log.info("-----订单金额处理逻辑-----------" + usdtNum.toPlainString());
                }

            } else {
                log.info("-----闪兑小于1u-----------" + usdtNum.toPlainString());
            }

        }
        //map中超过1小时的订单删除掉
        if (TrxOrderMap.size() >= 100) {
            Set<String> set = TrxOrderMap.keySet();
            for (String id : set) {
                if (TrxOrderMap.get(id).getCreateDate().plusHours(1).isBefore(LocalDateTime.now())) {
                    TrxOrderMap.remove(id);
                }
            }
        }
    }

    public void exchangeUsdt(String txId, String fromAddress, String toAddress, BigDecimal trxNum) {
        if (!UsdtOrderMap.containsKey(txId)) {
            ExchangeOrder order = new ExchangeOrder(txId, SymbolTypeEnum.TRX.getValue(), fromAddress, toAddress, trxNum);
            UsdtOrderMap.put(txId, order);
            ReceiveOrderDO receiveOrderDO = receiveOrderService.saveReceive(txId, trxNum, SymbolTypeEnum.TRX, fromAddress, toAddress);
            //区分订单和闪兑(订单金额)分别处理
            //1循环订单价格不包括当前价格就是闪兑
            if (!productOrderService.preOrder(txId, SymbolTypeEnum.TRX, receiveOrderDO, toAddress, trxNum)) {
                //2、闪兑大于100trx才处理
                if (null != trxNum && trxNum.compareTo(new BigDecimal(100)) >= 0) {
                    transferOrderService.exchange(receiveOrderDO,SymbolTypeEnum.USDT);
                } else {
                        energyRentalService.createOrder(receiveOrderDO);
                }

            } else {
                //订单金额处理逻辑
                log.info("------订单金额处理逻辑-----------" + trxNum.toPlainString());
            }

        }
        //map中超过1小时的订单删除掉
        if (UsdtOrderMap.size() >= 100) {
            Set<String> set = UsdtOrderMap.keySet();
            for (String id : set) {
                if (UsdtOrderMap.get(id).getCreateDate().plusHours(1).isBefore(LocalDateTime.now())) {
                    UsdtOrderMap.remove(id);
                }
            }
        }

    }

}
