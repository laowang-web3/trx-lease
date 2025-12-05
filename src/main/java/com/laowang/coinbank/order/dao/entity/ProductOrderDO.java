package com.laowang.coinbank.order.dao.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.math.BigDecimal;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 商品订单 Entity
 *
 */
@Data
@TableName("product_order")
public class ProductOrderDO {

    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;
    /**
     * 用户账号
     */
    private String account;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 业务类型
     */

    private Integer businessType;
    /**
     * trx金额
     */
    private BigDecimal trxAmount;
    /**
     * usdt金额
     */
    private BigDecimal usdtAmount;
    /**
     * 业务类型描述
     */
    private String business;

    /**
     * 创建日期
     */
    private LocalDateTime createDate;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 商品单价
     */
    private BigDecimal  unitPrice;
    /**
     * 商品数量
     */
    private Integer amount;

    /**
     * usdt汇率
     */
    private BigDecimal usdtRate;
    /**
     * trx汇率
     */
    private BigDecimal trxRate;

    /**
     * 接收地址表id
     */
    private Long receiveAddressId;

    /**
     * 接收地址
     */
    private String receiveAddress;



}
