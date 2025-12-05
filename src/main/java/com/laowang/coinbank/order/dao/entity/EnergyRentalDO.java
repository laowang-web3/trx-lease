package com.laowang.coinbank.order.dao.entity;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 能量租赁记录表 Entity
 *
 */
@Data
@TableName("energy_rental")
public class EnergyRentalDO {

    /**
     * 
     */
    private Long id;

    /**
     * 转入地址
     */
    private String address;

    /**
     * 转入数量
     */
    private BigDecimal amount;
    /**
     * 能量值
     */
    private Integer energyValue;

    /**
     * 能量时间（小时）
     */
    private Integer time;

    /**
     * 笔数
     */
    private Integer count;
    /**
     * 收钱入账表id
     */
    private Long receiveId;

    /**
     * 创建时间
     */
    private LocalDateTime createDate;

    /**
     * 交易id
     */
    private String txid;

    /**
     * 成本价
     */
    private BigDecimal cost;
    /**
     * 卖价
     */
    private BigDecimal sellingPrice;
    /**
     * 利润
     */
    private BigDecimal earnings;
}
