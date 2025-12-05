package com.laowang.coinbank.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.laowang.coinbank.bot.TelegramSendMessage;
import com.laowang.coinbank.common.BusinessEnum;
import com.laowang.coinbank.common.OrderTypeEnum;
import com.laowang.coinbank.common.SymbolTypeEnum;
import com.laowang.coinbank.order.dao.PayOrderDao;
import com.laowang.coinbank.order.dao.entity.PayOrderDO;
import com.laowang.coinbank.order.dao.entity.ProductOrderDO;
import com.laowang.coinbank.order.dao.entity.ReceiveOrderDO;
import com.laowang.coinbank.service.ApiTrxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.*;

/**
 * 业务订单
 */
@Service
@Slf4j
public class PayOrderService extends ServiceImpl<PayOrderDao, PayOrderDO> {

    @Autowired
    private ApiTrxService apiTrxService;
    @Autowired
    private TelegramSendMessage telegramSendMessage;

    public Boolean createPayOrert(String tronId, ReceiveOrderDO receiveOrder, BigDecimal amount, ProductOrderDO productOrderDO,
                                  SymbolTypeEnum coinType, String address){
        PayOrderDO payOrderDO = new PayOrderDO();
        payOrderDO.setStatus(OrderTypeEnum.PAY.getCode());
        payOrderDO.setTronId(tronId);
        payOrderDO.setReceiveOrderId(receiveOrder.getId());
        payOrderDO.setPayDate(LocalDateTime.now());
        payOrderDO.setPayAmount(amount);
        payOrderDO.setProductOrderId(productOrderDO.getId());
        payOrderDO.setCoinType(coinType.getValue());
        payOrderDO.setCoinTypeId(coinType.getCode());
        payOrderDO.setReceiveAddress(address);

        boolean isOk = false;
        if(productOrderDO.getBusinessType()== BusinessEnum.PREMIUM.getCode()){
            //调用开通api 1个月Telegram Premium会员
            String month = productOrderDO.getBusiness().substring(0,productOrderDO.getBusiness().indexOf("个月"));
            payOrderDO = apiTrxService.createPremium(payOrderDO,productOrderDO.getAccount(),month);
            if(null != payOrderDO){
                this.save(payOrderDO);
                telegramSendMessage.buildPremiumText(month,productOrderDO.getUsdtAmount().stripTrailingZeros().toPlainString(),productOrderDO.getReceiveAddress(),payOrderDO.getTronId());
                log.info("-------------支付订单创建成功----------"+payOrderDO.getId());
                return true;
            }else {
                log.info("------------调用api失败--------");
            }
        }else if(productOrderDO.getBusinessType()==BusinessEnum.COUNT.getCode()){
            payOrderDO = apiTrxService.createAuto(payOrderDO,receiveOrder.getFromAddress(),productOrderDO.getAmount());
            if(null != payOrderDO){
                this.save(payOrderDO);
                telegramSendMessage.buildPackageCountText(productOrderDO.getAmount().toString(),payOrderDO.getPayAmount().stripTrailingZeros().toPlainString()+" "+payOrderDO.getCoinType(),productOrderDO.getReceiveAddress(),payOrderDO.getTronId());
                log.info("-------------支付订单创建成功----------"+payOrderDO.getId());
                return true;
            }else {
                log.info("------------调用api失败--------");
            }
        }

        return false;

    }


}
