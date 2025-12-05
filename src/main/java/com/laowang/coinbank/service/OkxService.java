package com.laowang.coinbank.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laowang.coinbank.config.FlashExchangeProperties;
import com.laowang.coinbank.order.dao.entity.FlashExchangeDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Slf4j
public class OkxService {

    @Autowired
    private FlashExchangeProperties flashExchangeProperties;
    private final BigDecimal TrxPrice = new BigDecimal(0.32885).setScale(5, BigDecimal.ROUND_DOWN);
    public BigDecimal RecentTrxPrice = null;


    /**
     * 查询trx最新报价
     *
     * @return
     */
    public BigDecimal getTrxRecentPrice() {
        try {
            String url = "https://www.okx.com/api/v5/market/index-tickers?instId=TRX-USDT&quoteCcy=USDT";
            String result = HttpRequest.get(url)
                    .timeout(20000)
                    .execute().body();
            log.info("-------从欧意更新最新trx价格--result----------" + result);
            JSONObject jsonObject = JSONUtil.parseObj(result);
            if (null != jsonObject) {
                Integer code = jsonObject.getInt("code");
                if (0 == code) {
                    JSONArray data = jsonObject.getJSONArray("data");
                    if (null != data) {
                        JSONObject object = data.getJSONObject(0);
                        String balance = object.getStr("idxPx");
                        log.info("-------从欧意获取到了最新Trx价格----------" + balance);
                        BigDecimal bigDecimal = new BigDecimal(balance);
                        RecentTrxPrice = bigDecimal;
                        return bigDecimal;
                    }
                }
            }

        } catch (Exception e) {
            log.error("-------从欧意获取最新Trx报错了----------" + e.getMessage());
        } finally {
            RecentTrxPrice = TrxPrice;
            return TrxPrice;
        }


    }

    /**
     * 加上利润后卖价
     *
     * @return
     */
    public BigDecimal getTrxAmount(BigDecimal num) {
        FlashExchangeDO flashExchangeDO = new FlashExchangeDO();
        flashExchangeDO.setFormAmount(num);
        return getTrxAmount(flashExchangeDO).getToAmount();
    }
    /**
     * 加上利润后卖价
     *
     * @return
     */
    public FlashExchangeDO getTrxAmount(FlashExchangeDO flashExchangeDO) {
        if (null == RecentTrxPrice) {
            getTrxRecentPrice();
        }
        //1usdt兑换多少trx
        BigDecimal rate = BigDecimal.ONE.divide(RecentTrxPrice, 3, BigDecimal.ROUND_DOWN);
        flashExchangeDO.setExchangeRate(rate);
        //单价利润
        BigDecimal earnings = rate.multiply(flashExchangeProperties.getProfit());
        flashExchangeDO.setEarnings(earnings.multiply(flashExchangeDO.getFormAmount()));
        //单卖价
        BigDecimal SellPrice = rate.subtract(earnings);
        //总价
        BigDecimal allAmount = flashExchangeDO.getFormAmount().multiply(SellPrice).setScale(3, BigDecimal.ROUND_DOWN);
        flashExchangeDO.setToAmount(allAmount);
        return flashExchangeDO;
    }

    /**
     * 加上利润后卖价
     *
     * @return
     */
    public FlashExchangeDO getUsdtAmount(FlashExchangeDO flashExchangeDO) {
        if (null == RecentTrxPrice) {
            getTrxRecentPrice();
        }
        //单价利润
        BigDecimal earnings = RecentTrxPrice.multiply(flashExchangeProperties.getProfit());
        flashExchangeDO.setExchangeRate(RecentTrxPrice);
        flashExchangeDO.setEarnings(earnings.multiply(flashExchangeDO.getFormAmount()));
        //单卖价
        BigDecimal SellPrice = RecentTrxPrice.subtract(earnings);
        //总价
        BigDecimal allAmount = flashExchangeDO.getFormAmount().multiply(SellPrice).setScale(3, BigDecimal.ROUND_DOWN);
        flashExchangeDO.setToAmount(allAmount);
        return flashExchangeDO;
    }

    public BigDecimal getUsdtAmount(BigDecimal trxNum) {
        FlashExchangeDO flashExchangeDO = new FlashExchangeDO();
        flashExchangeDO.setFormAmount(trxNum);
        return getUsdtAmount(flashExchangeDO).getToAmount();
    }


    /**
     * trx最新汇率
     * @return
     */
    public BigDecimal getTrxRate() {
        if (null == RecentTrxPrice) {
            getTrxRecentPrice();
        }
        return RecentTrxPrice;
    }


}
