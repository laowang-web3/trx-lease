package com.laowang.coinbank.order.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.laowang.coinbank.common.ReceiveStatusEnum;
import com.laowang.coinbank.common.SymbolTypeEnum;
import com.laowang.coinbank.order.dao.ReceiveOrderDao;
import com.laowang.coinbank.order.dao.entity.ReceiveOrderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收款地址接收记录
 */
@Service
@Slf4j
public class ReceiveOrderService extends ServiceImpl<ReceiveOrderDao, ReceiveOrderDO> {


    public ReceiveOrderDO saveReceive(String tronId, BigDecimal amount
            , SymbolTypeEnum coinType, String fromAddress, String toAddress) {
        ReceiveOrderDO receiveOrderDO = new ReceiveOrderDO();
        receiveOrderDO.setStatus(ReceiveStatusEnum.NON.getCode());
        receiveOrderDO.setAmount(amount);
        receiveOrderDO.setCoinType(coinType.getValue());
        receiveOrderDO.setCoinTypeId(coinType.getCode());
        receiveOrderDO.setTronId(tronId);
        receiveOrderDO.setFromAddress(fromAddress);
        receiveOrderDO.setToAddress(toAddress);
        receiveOrderDO.setCreateDate(LocalDateTime.now());
        this.save(receiveOrderDO);
        log.info(receiveOrderDO.getId() + "----接收记录创建成功---" + amount.toPlainString());
        return receiveOrderDO;
    }

    public Boolean dealStatus(Long id) {
        LambdaUpdateWrapper<ReceiveOrderDO> updateWrapper = Wrappers.lambdaUpdate(ReceiveOrderDO.class);
        updateWrapper.eq(ReceiveOrderDO::getId, id);
        updateWrapper.set(ReceiveOrderDO::getStatus, ReceiveStatusEnum.DEAL.getCode());
       return this.update(updateWrapper);
    }

}
