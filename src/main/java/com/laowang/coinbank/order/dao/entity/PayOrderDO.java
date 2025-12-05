package com.laowang.coinbank.order.dao.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.math.BigDecimal;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户支付商品订单 Entity
 *
 */
@Data
@TableName("pay_order")
public class PayOrderDO {

    /**
     * id
     */
    private Long id;

    /**
     * 交易ID
     */
    private String tronId;

    /**
     * 支付时间
     */
    private LocalDateTime payDate;

    /**
     * 支付状态
     */
    private Integer status;
    /**
     * 支付金额
     */
    private BigDecimal payAmount;
    /**
     * 支付币种
     */
    private String coinType;

    /**
     * 支付币种id
     */
    private Integer coinTypeId;

    /**
     * 订单表id
     */
    private Long productOrderId;

    /**
     * 收钱表id
     */
    private Long receiveOrderId;

    /**
     * 接收地址
     */
    private String receiveAddress;

    /**
     * 成本
     */
    private BigDecimal cost;
}
