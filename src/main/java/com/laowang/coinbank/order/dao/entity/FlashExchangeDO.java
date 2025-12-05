package com.laowang.coinbank.order.dao.entity;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 闪兑记录表 Entity
 *
 */
@Data
@TableName("flash_exchange")
public class FlashExchangeDO {

    /**
     * 
     */
    private Long id;

    /**
     * 兑换汇率
     */
    private BigDecimal exchangeRate;

    /**
     * 转入地址
     */
    private String fromAddress;

    /**
     * 转入数量
     */
    private BigDecimal formAmount;
    /**
     * 转入币种
     */
    private String formCoin;

    /**
     * 转出地址
     */
    private String toAddress;

    /**
     * 转出数量
     */
    private BigDecimal toAmount;
    /**
     * 转出币种
     */
    private String toCoin;

    /**
     * 创建时间
     */
    private LocalDateTime createDate;

    /**
     * 收钱入账表id
     */
    private Long receiveId;

    /**
     * 利润
     */
    private BigDecimal earnings;

    /**
     * 交易id
     */
    private String tronId;
}
