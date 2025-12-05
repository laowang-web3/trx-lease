package com.laowang.coinbank.order.dao.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.math.BigDecimal;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 收款记录表 Entity
 *
 */
@Data
@TableName("receive_order")
public class ReceiveOrderDO {

    /**
     * 
     */
    private Long id;

    /**
     * tron交易id
     */
    private String tronId;

    /**
     * 转入地址
     */
    private String fromAddress;

    /**
     * 接收地址
     */
    private String toAddress;

    /**
     * 转入金额
     */
    private BigDecimal amount;
    /**
     * 转入币种
     */
    private String coinType;

    /**
     * 支付币种id
     */
    private Integer coinTypeId;

    /**
     * 创建时间
     */
    private LocalDateTime createDate;

    /**
     * 状态(0 未处理，1已处理)
     */
    private Integer status;

    /**
     * 区块id
     */
    private Integer blockId;
}
