package com.laowang.coinbank.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.laowang.coinbank.bot.TelegramSendMessage;
import com.laowang.coinbank.config.TelegramBotProperties;
import com.laowang.coinbank.order.dao.EnergyRentalDao;
import com.laowang.coinbank.order.dao.entity.EnergyRentalDO;
import com.laowang.coinbank.order.dao.entity.ReceiveOrderDO;
import com.laowang.coinbank.service.ApiTrxService;
import com.laowang.coinbank.utils.TronUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 能量租赁记
 */
@Service
@Slf4j
public class EnergyRentalService extends ServiceImpl<EnergyRentalDao, EnergyRentalDO> {

    @Autowired
    private TelegramBotProperties telegramBotProperties;
    @Autowired
    private ApiTrxService apiTrxService;
    @Autowired
    private TelegramSendMessage telegramSendMessage;
    @Autowired
    private ReceiveOrderService receiveOrderService;

    public Boolean createOrder(ReceiveOrderDO receiveOrderDO) {
        EnergyRentalDO energyRentalDO = new EnergyRentalDO();
        energyRentalDO.setAddress(receiveOrderDO.getFromAddress());
        energyRentalDO.setAmount(receiveOrderDO.getAmount());
        energyRentalDO.setSellingPrice(telegramBotProperties.getEnergyPrice());
        energyRentalDO.setTime(1);
        energyRentalDO.setReceiveId(receiveOrderDO.getId());
        energyRentalDO.setCreateDate(LocalDateTime.now());

        if (receiveOrderDO.getAmount().compareTo(telegramBotProperties.getEnergyPrice()) >= 0) {
            //下单数量
            if (TronUtils.isModuloZero(receiveOrderDO.getAmount(), telegramBotProperties.getEnergyPrice())) {
                BigDecimal num = receiveOrderDO.getAmount().divide(telegramBotProperties.getEnergyPrice(), 0, BigDecimal.ROUND_DOWN);
                log.info("------购买能量笔量：" + num + "-----------");
                //单次20笔封顶
                if (num.intValue() <= 20) {
                    Integer energyValue = 65000 * num.intValue();
                    if(num.intValue()==2){
                        //对方地址无U
                        energyValue= 131000;
                        energyRentalDO =   apiTrxService.getEnergy(energyRentalDO,receiveOrderDO.getFromAddress(), energyValue, 1);
                    }else {
                        //对方地址有U
                        energyRentalDO =   apiTrxService.getEnergy(energyRentalDO,receiveOrderDO.getFromAddress(), energyValue, 1);
                    }
                    if(null != energyRentalDO){
                        energyRentalDO.setEarnings(receiveOrderDO.getAmount().subtract(energyRentalDO.getCost()));
                        energyRentalDO.setCount(num.intValue());
                        energyRentalDO.setEnergyValue(energyValue);
                        this.save(energyRentalDO);
                        telegramSendMessage.buildEnergyText(energyRentalDO.getCount().toString(),energyRentalDO.getEnergyValue().toString(),energyRentalDO.getAddress(),energyRentalDO.getTxid());
                        receiveOrderService.dealStatus(receiveOrderDO.getId());
                    }else {
                        log.info("-----调用能量api接口报错：" + receiveOrderDO.getId());
                    }


                } else {
                    log.info("-----超过最大笔数：" + num);
                }

        } else {
            log.info("------转入价格" + receiveOrderDO.getAmount().toPlainString() + "不是售价的倍数：" + telegramBotProperties.getEnergyPrice().toPlainString() + "-----------");
        }

    }else{
        //转入价格低于最低卖价
        log.info("------转入价格" + receiveOrderDO.getAmount().toPlainString() + "低于最低卖价：" + telegramBotProperties.getEnergyPrice().toPlainString() + "-----------");
    }
        return true;
    }
}
