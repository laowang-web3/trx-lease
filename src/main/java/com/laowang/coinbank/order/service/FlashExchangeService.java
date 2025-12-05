package com.laowang.coinbank.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.laowang.coinbank.bot.TelegramSendMessage;
import com.laowang.coinbank.common.SymbolTypeEnum;
import com.laowang.coinbank.order.dao.FlashExchangeDao;
import com.laowang.coinbank.order.dao.entity.FlashExchangeDO;
import com.laowang.coinbank.order.dao.entity.ReceiveOrderDO;
import com.laowang.coinbank.service.OkxService;
import com.laowang.coinbank.service.TronService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 向外转出金额记录表
 */
@Service
@Slf4j
public class FlashExchangeService extends ServiceImpl<FlashExchangeDao, FlashExchangeDO> {

    @Autowired
    public ReceiveOrderService receiveOrderService;
    @Autowired
    public TelegramSendMessage telegramSendMessage;
    @Autowired
    public OkxService okxService;
    @Autowired
    public TronService tronService;

    public FlashExchangeDO exchange(ReceiveOrderDO receiveOrderDO,SymbolTypeEnum toCoin) {
        FlashExchangeDO flashExchangeDO = new FlashExchangeDO();
        flashExchangeDO.setToCoin(toCoin.getValue());
        flashExchangeDO.setFromAddress(receiveOrderDO.getFromAddress());
        flashExchangeDO.setFormAmount(receiveOrderDO.getAmount());
        flashExchangeDO.setFormCoin(receiveOrderDO.getCoinType());
        flashExchangeDO.setToAddress(receiveOrderDO.getToAddress());
        flashExchangeDO.setReceiveId(receiveOrderDO.getId());
        if(SymbolTypeEnum.USDT.getCode() == toCoin.getCode()){
            flashExchangeDO = okxService.getUsdtAmount(flashExchangeDO);
        }else {
            flashExchangeDO = okxService.getTrxAmount(flashExchangeDO);

        }

        if (null != flashExchangeDO.getToAmount() && flashExchangeDO.getToAmount().compareTo(BigDecimal.ZERO) > 0) {
            String tronId = null;
            if(SymbolTypeEnum.USDT.getCode() == toCoin.getCode()){
                tronId = tronService.transUsdt(flashExchangeDO.getFromAddress(), flashExchangeDO.getToAmount());
            }else {
                tronId = tronService.transferTRX(flashExchangeDO.getFromAddress(), flashExchangeDO.getToAmount());

                }
            if (StringUtils.isNotEmpty(tronId)) {
                log.info("----"+receiveOrderDO.getCoinType()+"闪兑自动向" + flashExchangeDO.getFromAddress() + "转"+toCoin.getValue() + flashExchangeDO.getToAmount().toPlainString());
                flashExchangeDO.setTronId(tronId);
                flashExchangeDO.setCreateDate(LocalDateTime.now());
                this.save(flashExchangeDO);
                receiveOrderService.dealStatus(receiveOrderDO.getId());
                telegramSendMessage.buildFlashText(flashExchangeDO.getFormCoin(),flashExchangeDO.getToCoin(),flashExchangeDO.getFormAmount().stripTrailingZeros().toPlainString(),
                        flashExchangeDO.getToAmount().stripTrailingZeros().toPlainString(),flashExchangeDO.getFromAddress(),flashExchangeDO.getTronId());
                log.info(flashExchangeDO.getId() + "---闪兑转出记录创建成功---");
                return flashExchangeDO;
            }
        }
        return null;
    }


}
