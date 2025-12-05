package com.laowang.coinbank.service;


import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laowang.coinbank.order.dao.entity.EnergyRentalDO;
import com.laowang.coinbank.order.dao.entity.PayOrderDO;
import com.laowang.coinbank.order.dao.entity.ReceiveOrderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * ApiTrx平台API接口
 */
@Slf4j
@Service
public class ApiTrxService {
    //trxKey
    private final String APIKEY = "4E42E94D-D11D-423D-8C64-aaaa";
    //usdtKey
    private final String VIPUSDTKEY = "a1752b15-0ea5-4321-a1c6-bbbb";
    private final String baseUrl = "https://web.apitrx.com";


    /**
     * 能量下单
     * address 接收能量的波场地址
     * num 数量 65000
     * time 时效 1
     */
    public EnergyRentalDO getEnergy(EnergyRentalDO energyRentalDO, String address, Integer num, Integer time) {
//        energyRentalDO.setCost(new BigDecimal("1.17"));
//        energyRentalDO.setTxid("20d25f3c81e8433df2373472c2592bb3f4b0bd053a2abf534cadf9699e9604f0");
//        return energyRentalDO;
        String url = baseUrl + "/getenergy?apikey=" + APIKEY + "&add=" + address + "&value=" + num + "&hour=" + time + "";
        String result =  HttpRequest.get(url)
                .timeout(20000)
                .execute().body();
        System.out.println("-------getEnergy--result----------" + result);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        if (null == jsonObject) {
            return null;
        }
        Integer code = jsonObject.getInt("code");
        if(200 == code){
            String message = jsonObject.getStr("message");
            JSONObject dataObject = JSONUtil.parseObj("data");
            //剩余金额(TRX)
            String balance = dataObject.getStr("balance");
            String txid = dataObject.getStr("txid");
            energyRentalDO.setTxid(txid);
            //本次消费金额(TRX)
            String amount = dataObject.getStr("amount");
            energyRentalDO.setCost(new BigDecimal(amount));
            return energyRentalDO;
        }

        return null;
    }
    /**
     * 笔数下单
     * address 接收能量的波场地址
     * num 数量 65000
     * time 时效 1
     */
    public PayOrderDO createAuto(PayOrderDO payOrderDO , String address, Integer num) {
//        payOrderDO.setCost(new BigDecimal("2.5"));
//        payOrderDO.setTronId("20d25f3c81e8433df2373472c2592bb3f4b0bd053a2abf534cadf9699e9604f0");
//        return payOrderDO;
        String url = baseUrl + "/auto?apikey=" + APIKEY + "&add=" + address + "&count=" + num;
        String result =  HttpRequest.get(url)
                .timeout(20000)
                .execute().body();
        System.out.println("-------getEnergy--result----------" + result);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        if (null == jsonObject) {
            return null;
        }
        Integer code = jsonObject.getInt("code");
        if(200 == code){
            String message = jsonObject.getStr("message");
            JSONObject dataObject = JSONUtil.parseObj("data");
            //剩余金额(TRX)
            String balance = dataObject.getStr("balance");
            String txid = dataObject.getStr("txid");
            payOrderDO.setTronId(txid);
            //本次消费金额(TRX)
            String amount = dataObject.getStr("amount");
            payOrderDO.setCost(new BigDecimal(amount));
            return payOrderDO;
        }

        return null;
    }

    /**
     * 查询最新报价
     */
    public Boolean getPrice() {
        String url = baseUrl + "/price?apikey=" + APIKEY + "&value=65000";
        String result =  HttpRequest.get(url)
                .timeout(20000)
                .execute().body();
        log.info("-------getPrice--result----------" + result);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        if (null == jsonObject) {
            return false;
        }
        Integer code = (Integer)jsonObject.get("code");
        if(200 == code){
            String message = jsonObject.get("message").toString();
            if("SUCCESS".contains(message)){
                JSONObject dataObject = JSONUtil.parseObj(jsonObject.get("data"));
                //1小时最新报价
                String price = dataObject.get("1").toString();
                log.info("-----最新报价----"+price);
                return true;
            }

        }

        return false;
    }
    /**
     * 开通会员 trx计价
     */
    public PayOrderDO createPremium(PayOrderDO payOrderDO,String username,String month) {
//        payOrderDO.setCost(new BigDecimal("12.5"));
//        return payOrderDO;
        String url = "https://web.apitrx.com/premium";
        HttpRequest post = HttpRequest.post(url);
        post.form("apikey",APIKEY);
        post.form("username",username);
        post.form("month",month);
//      post.form("callback_url","成功后回调");
        String result =   post.timeout(20000)
                .execute().body();
        log.info("-------getPrice--result----------" + result);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        if (null == jsonObject) {
            return null;
        }
        Integer code = jsonObject.getInt("code");
        if(200 == code){
            String message = jsonObject.getStr("message");
            if("SUCCESS".contains(message)){
                JSONObject dataObject = JSONUtil.parseObj(jsonObject.get("data"));
                //订单ID
                String orderId = dataObject.getStr("orderId");
                //用户名
                String username1 = dataObject.getStr("username");
                //昵称
                String nickname = dataObject.getStr("username");
                //订阅时长
                String month1 = dataObject.getStr("month");
                //开通状态	0开通中 1已开通 2开通失败
                Integer status = dataObject.getInt("status");
                //备注
                String remark = dataObject.getStr("remark");
                //消费金额
                Double amount = dataObject.getDouble("amount");
                 payOrderDO.setCost(new BigDecimal(amount));
                //余额
                Double balance1 = dataObject.getDouble("balance");
                return payOrderDO;
            }

        }

        return null;
    }

    /**
     * 查询会员是否存在
     */
    public int queryUser(String username) {
//        return 0;
        String url = "https://web.apitrx.com/query";
        HttpRequest post = HttpRequest.post(url);
        post.form("apikey",APIKEY);
        post.form("username",username);
        String result =   post.timeout(20000)
                .execute().body();
        System.out.println("-------queryUser--result----------" + result);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        if (null == jsonObject) {
            return -1;
        }
        Integer code = jsonObject.getInt("code");
        if(200 == code){
            String message = jsonObject.getStr("message");
            if("SUCCESS".equals(message)){
                return 0;
            }else if("ERROR".equals(message)){
                return 1;
            }else if("已经是会员了".equals(message)){
                return 2;
            }else if("用户不存在".equals(message)){
                return 1;
            }

        }

        return -1;
    }
}
