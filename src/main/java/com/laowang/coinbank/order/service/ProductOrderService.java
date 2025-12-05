package com.laowang.coinbank.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.laowang.coinbank.common.BusinessEnum;
import com.laowang.coinbank.common.OrderTypeEnum;
import com.laowang.coinbank.common.ReceiveStatusEnum;
import com.laowang.coinbank.common.SymbolTypeEnum;
import com.laowang.coinbank.config.TelegramBotProperties;
import com.laowang.coinbank.order.dao.PayOrderDao;
import com.laowang.coinbank.order.dao.ProductOrderDao;
import com.laowang.coinbank.order.dao.entity.PayOrderDO;
import com.laowang.coinbank.order.dao.entity.ProductOrderDO;
import com.laowang.coinbank.order.dao.entity.ReceiveOrderDO;
import com.laowang.coinbank.service.OkxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务订单
 */
@Service
@Slf4j
public class ProductOrderService extends ServiceImpl<ProductOrderDao, ProductOrderDO> {

    @Autowired
    @Lazy
    private OkxService okxService;
    @Autowired
    @Lazy
    private PayOrderService payOrderService;
    @Autowired
    @Lazy
    private ReceiveOrderService receiveOrderService;

    private   List<BigDecimal> decimalList = new ArrayList<>();

    public ProductOrderDO createOrert(Long userId, String userName,String account,BusinessEnum businessEnum,String business,BigDecimal unitPrice,
                            Integer amount,String receiveAddress){
        ProductOrderDO orderDO = new ProductOrderDO();
        orderDO.setUserId(userId);
        orderDO.setUserName(userName);
        if(!account.contains("@")){
            account="@"+account;
        }
        orderDO.setAccount(account);
        orderDO.setBusinessType(businessEnum.getCode());
        orderDO.setCreateDate(LocalDateTime.now());
        orderDO.setBusiness(businessEnum.getValue());
        //
        if(BusinessEnum.PREMIUM.equals(businessEnum)){
            orderDO.setUsdtAmount(unitPrice);
        }else {
            BigDecimal trxAmount = unitPrice.multiply(new BigDecimal(amount));
            orderDO.setTrxAmount(trxAmount);
            exchangeUsdtAmount(orderDO);
        }

        orderDO.setBusiness(business);
        orderDO.setReceiveAddress(receiveAddress);
        orderDO.setUnitPrice(unitPrice);
        orderDO.setAmount(amount);
        orderDO.setStatus(OrderTypeEnum.NON.getCode());

        generateDecimals(orderDO);
         if(this.save(orderDO)){
             return orderDO;
         }
         return null;
    }

    /**
     *  计算唯一小数位数
     * 每个金额生成不同的小数位以区分具体订单
     * 1、数据量小直接查数据库校验
     * 2、数据量大预先生成一队，直接取用。
     * @return
     */
    public ProductOrderDO generateDecimals(ProductOrderDO productOrderDO) {
                 BigDecimal decimals = BigDecimal.ZERO;
                 if(CollectionUtils.isEmpty(decimalList)){
                     for(int i=0;i <= 99;i++){
                         decimals = decimals.add(new BigDecimal("0.01"));
                         decimalList.add(decimals);
                     }
                 }
                 decimals = new BigDecimal("0.01");
//                 if(null != productOrderDO.getTrxAmount() ){
//                     productOrderDO.setTrxAmount(productOrderDO.getTrxAmount().add(decimals));
//                 }
//                if(null != productOrderDO.getUsdtAmount() ){
//                    productOrderDO.setUsdtAmount(productOrderDO.getUsdtAmount().add(decimals));
//                }
                LambdaQueryWrapper<ProductOrderDO> wrapper = Wrappers.lambdaQuery(ProductOrderDO.class);
                wrapper.eq(ProductOrderDO::getStatus,OrderTypeEnum.NON.getCode());
                 if(null != productOrderDO.getTrxAmount()){
                      wrapper.apply("  FLOOR(trx_amount)="+productOrderDO.getTrxAmount());
                 }
                 if(null != productOrderDO.getUsdtAmount()){
                    if(null != productOrderDO.getTrxAmount()){
                        wrapper.or();
                    }
                     wrapper.apply("  FLOOR(usdt_amount)="+productOrderDO.getUsdtAmount());
                }
                List<ProductOrderDO> list = this.list(wrapper);
                if(CollectionUtils.isNotEmpty(list)){
                    boolean trx =false;
                    boolean usdt =false;
                    for(BigDecimal item : decimalList){
                        if(null != productOrderDO.getTrxAmount() && !trx){
                            log.info("---------------trx循环-----------"+item);
                            boolean isExist = list.stream().anyMatch(e -> e.getTrxAmount().compareTo(productOrderDO.getTrxAmount().add(item)) == 0);
                            if(!isExist){
                                productOrderDO.setTrxAmount(productOrderDO.getTrxAmount().add(item));
                                trx=true;
                            }
                        }
                        if(trx && null == productOrderDO.getUsdtAmount()){
                            break;
                        }
                        if(null != productOrderDO.getUsdtAmount()&& !usdt){
                            log.info("---------------usdt循环-----------"+item);
                            boolean isExist = list.stream().anyMatch(e -> e.getUsdtAmount().compareTo(productOrderDO.getUsdtAmount().add(item)) == 0);
                            if(!isExist){
                                productOrderDO.setUsdtAmount(productOrderDO.getUsdtAmount().add(item));
                                usdt=true;
                            }
                        }
                        if(usdt && null == productOrderDO.getTrxAmount()){
                            break;
                        }
                        if(trx && usdt){
                            break;
                        }
                    }
                    //防止单用户恶意创建订单
//                    if(list.size()>=10) {
                        List<ProductOrderDO> collect = list.stream().filter(e -> e.getUserId().compareTo(productOrderDO.getUserId()) == 0).collect(Collectors.toList());
                        if(CollectionUtils.isNotEmpty(collect)&&collect.size()>5){
                                List<ProductOrderDO> list1 =  collect.stream()
                                        .sorted(Comparator.comparing(ProductOrderDO::getId))
                                        .collect(Collectors.toList());
                                List<ProductOrderDO> list2 = list1.subList(0, 3);
                                List<Long> ids = list2.stream().map(a -> a.getId()).collect(Collectors.toList());
                                clearOutIds(ids);
                        }
//                    }
                    //单价格超过50条，主动清理一下，保证小数位在较小值。
                    if(list.size()>=50){
                        List<ProductOrderDO> list1 =  list.stream()
                                .sorted(Comparator.comparing(ProductOrderDO::getId))
                                .collect(Collectors.toList());
                        List<ProductOrderDO> list2 = list1.subList(0, 10);
                        List<Long> ids = list2.stream().map(a -> a.getId()).collect(Collectors.toList());
                        clearOutIds(ids);
                    }
                }else {
                     if(null != productOrderDO.getTrxAmount() ){
                         productOrderDO.setTrxAmount(productOrderDO.getTrxAmount().add(decimals));
                     }
                    if(null != productOrderDO.getUsdtAmount() ){
                        productOrderDO.setUsdtAmount(productOrderDO.getUsdtAmount().add(decimals));
                    }
                }


        return productOrderDO;
    }

    public void clearOutTimeOrder(){
        LambdaQueryWrapper<ProductOrderDO> wrapper = Wrappers.lambdaQuery(ProductOrderDO.class);
        wrapper.eq(ProductOrderDO::getStatus,OrderTypeEnum.NON.getCode());
        wrapper.orderByAsc(ProductOrderDO::getCreateDate);
        List<ProductOrderDO> list = this.list(wrapper);
        if(CollectionUtils.isNotEmpty(list)){
            List<Long> ids = new ArrayList<>();
            //超过99条未支付订单(2种方案：1 按时间最早那条循环取消。2 设置1小时超时时间把超时的都取消)
            for(ProductOrderDO productOrderDO1 : list){
                if(productOrderDO1.getCreateDate().plusHours(1).isBefore(LocalDateTime.now())){
                    ids.add(productOrderDO1.getId());
                }
            }
        if(CollectionUtils.isNotEmpty(ids)){
            clearOutIds(ids);
         }
       }
    }
    public void clearOutIds(List<Long> ids){
        LambdaUpdateWrapper<ProductOrderDO> updateWrapper = Wrappers.lambdaUpdate(ProductOrderDO.class);
        updateWrapper.in(ProductOrderDO::getId,ids);
        updateWrapper.set(ProductOrderDO::getStatus,OrderTypeEnum.OUT.getCode());
        this.update(updateWrapper);
    }

    /**
     * userid取消最新的一笔订单
     * @param userId
     */
    public void cancelOrder(Long userId){
        LambdaQueryWrapper<ProductOrderDO> wrapper = Wrappers.lambdaQuery(ProductOrderDO.class);
        wrapper.eq(ProductOrderDO::getStatus,OrderTypeEnum.NON.getCode());
        wrapper.eq(ProductOrderDO::getUserId,userId);
        wrapper.orderByDesc(ProductOrderDO::getCreateDate);
        wrapper.last("LIMIT 1");
        ProductOrderDO productOrderDO = this.getOne(wrapper);
        if(null != productOrderDO){
            LambdaUpdateWrapper<ProductOrderDO> updateWrapper = Wrappers.lambdaUpdate(ProductOrderDO.class);
            updateWrapper.eq(ProductOrderDO::getId,productOrderDO.getId());
            updateWrapper.set(ProductOrderDO::getStatus,OrderTypeEnum.CANCEL.getCode());
            this.update(updateWrapper);
        }
    }
    /**
     * trx商品转usdt价格
     *
     * @return
     */
    public ProductOrderDO exchangeUsdtAmount(ProductOrderDO productOrderDO) {
        BigDecimal trxRate = getTrxRate();
        BigDecimal normal = getTrxRate().multiply(productOrderDO.getTrxAmount());
        productOrderDO.setTrxRate(trxRate);
        productOrderDO.setUsdtRate(BigDecimal.ONE.divide(trxRate,3,BigDecimal.ROUND_DOWN));
        productOrderDO.setUsdtAmount(normal.setScale(2,BigDecimal.ROUND_DOWN));
        return productOrderDO;
    }
    /**
     * 检查入账的金额是不是和未支付订单订单金额匹配
     * 根据地址和金额双验证
     * @return
     */
    public Boolean preOrder(String tronId,SymbolTypeEnum symbolTypeEnum,ReceiveOrderDO receiveOrder,String address,BigDecimal num) {
        LambdaQueryWrapper<ProductOrderDO> wrapper = Wrappers.lambdaQuery(ProductOrderDO.class);
        wrapper.eq(ProductOrderDO::getStatus,OrderTypeEnum.NON.getCode());
        wrapper.eq(ProductOrderDO::getReceiveAddress,address);
        if(symbolTypeEnum.getCode()== SymbolTypeEnum.USDT.getCode()){
            wrapper.eq(ProductOrderDO::getUsdtAmount,num);
        }
        if(symbolTypeEnum.getCode()== SymbolTypeEnum.TRX.getCode()){
            wrapper.eq(ProductOrderDO::getTrxAmount,num);
        }
        List<ProductOrderDO> list = this.list(wrapper);
        if(CollectionUtils.isNotEmpty(list)){
            if(list.size()==1){
                ProductOrderDO productOrderDO = list.get(0);
                boolean isOk = payOrderService.createPayOrert(tronId, receiveOrder, num, productOrderDO,
                        symbolTypeEnum, address);
                if(isOk){
                    receiveOrderService.dealStatus(receiveOrder.getId());
                    payStatus(productOrderDO.getId());
                    return true;
                }
            }else {
                log.info("-------------相同订单超过2个----------");
            }

        }
        return false;
    }
    /**
     * 能量订单保存
     * @return
     */
    public Boolean energy(String tronId,SymbolTypeEnum symbolTypeEnum,ReceiveOrderDO receiveOrder,String address,BigDecimal num) {
        LambdaQueryWrapper<ProductOrderDO> wrapper = Wrappers.lambdaQuery(ProductOrderDO.class);
        wrapper.eq(ProductOrderDO::getStatus,OrderTypeEnum.NON.getCode());
        wrapper.eq(ProductOrderDO::getReceiveAddress,address);
        if(symbolTypeEnum.getCode()== SymbolTypeEnum.USDT.getCode()){
            wrapper.eq(ProductOrderDO::getUsdtAmount,num);
        }
        if(symbolTypeEnum.getCode()== SymbolTypeEnum.TRX.getCode()){
            wrapper.eq(ProductOrderDO::getTrxAmount,num);
        }
        List<ProductOrderDO> list = this.list(wrapper);
        if(CollectionUtils.isNotEmpty(list)){
            if(list.size()==1){
                ProductOrderDO productOrderDO = list.get(0);
                boolean isOk = payOrderService.createPayOrert(tronId, receiveOrder, num, productOrderDO,
                        symbolTypeEnum, address);
                if(isOk){
                    receiveOrderService.dealStatus(receiveOrder.getId());
                    payStatus(productOrderDO.getId());
                    return true;
                }
            }else {
                log.info("-------------相同订单超过2个----------");
            }

        }
        return false;
    }
    public Boolean payStatus(Long id) {
        LambdaUpdateWrapper<ProductOrderDO> updateWrapper = Wrappers.lambdaUpdate(ProductOrderDO.class);
        updateWrapper.eq(ProductOrderDO::getId, id);
        updateWrapper.set(ProductOrderDO::getStatus, OrderTypeEnum.PAY.getCode());
        return this.update(updateWrapper);
    }

    /**
     * trx转usdt汇率
     * 加5%手续费
     * @return
     */
    public BigDecimal getTrxRate() {
        BigDecimal earnings = okxService.getTrxRate().multiply(new BigDecimal(0.05));
        return okxService.getTrxRate().add(earnings).setScale(6,BigDecimal.ROUND_DOWN);
    }

    public static void main(String[] args) {
        BigDecimal decimals = new BigDecimal(0);
        BigDecimal add = decimals.add(new BigDecimal(0.01));
        System.out.println("-----"+add.toPlainString());
    }


}
