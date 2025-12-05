package com.laowang.coinbank.service;

import cn.hutool.core.util.StrUtil;
import com.laowang.coinbank.config.MonitorProperties;
import com.laowang.coinbank.config.TelegramBotProperties;
import com.laowang.coinbank.config.TronProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.contract.Contract;
import org.tron.trident.core.contract.Trc20Contract;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Convert;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * 操作波场网络服务
 */
@Slf4j
@Service
public class TronService {
    //最小单位转换比
    public  final int SCALE = 1000000;
    // 注入配置类
    @Autowired
    private MonitorProperties monitorProperties;
    @Autowired
    private TronProperties tronProperties;
    private static  ApiWrapper apiWrapper = null;



    public   ApiWrapper getApiWrapper() {
        if(null != apiWrapper){
            return apiWrapper;
        }
        //配置是否是测试链还是正式链
        if (monitorProperties.getOnline()) {
            // 正式环境需要到Tron申请一个apiKey进行使用
            // APIKEY获取往后会讲如何获取
                apiWrapper =  ApiWrapper.ofMainnet(tronProperties.getHexPrivateKey(),tronProperties.getApiKey());
            return apiWrapper;
        } else {
            // 测试环境不需要apikey
                apiWrapper =  new ApiWrapper(monitorProperties.getGrpcHost()+":"+monitorProperties.getGrpcHostPort(), monitorProperties.getGrpcHost()+":"+monitorProperties.getGrpcHostSolidityPort(),tronProperties.getHexPrivateKey());
            return apiWrapper;
        }
    }
    /**
     * TRC20余额
     * @param address 我的钱包地址
     * @return 余额
     */
    public BigDecimal getUsdtBalance(String address) {
        ApiWrapper client = getApiWrapper();
        // Trc20合约地址
        Contract contract = client.getContract(monitorProperties.getTrc20UsdtAddress());
        Trc20Contract token = new Trc20Contract(contract, address, client);
        BigInteger balanceOf = token.balanceOf(address);
        BigDecimal divisor = new BigDecimal(SCALE);
        BigDecimal divide = new BigDecimal(balanceOf).divide(divisor, 4, RoundingMode.HALF_UP);
//        client.close();
        return divide;
    }
    /**
     * TRx余额
     * @param address
     * @return
     */
    public BigDecimal getTRxBalance(String address) {
        ApiWrapper wrapper = getApiWrapper();
        Long balance = wrapper.getAccountBalance(address);
        BigDecimal divisor = new BigDecimal(SCALE);
        BigDecimal divide = new BigDecimal(balance).divide(divisor, 4, RoundingMode.HALF_UP);
//        wrapper.close();
        return divide;
    }
    /**
     * TRC20 转账
     * @param
     * @param toAddress
     * @param amount
     * @return
     */
    public String transUsdt(String toAddress, BigDecimal amount) {
        String fromAddress = tronProperties.getWalletAddress();
        try {
            ApiWrapper client = getApiWrapper();
            // 获取合约地址信息
            Contract contract = client.getContract(monitorProperties.getTrc20UsdtAddress());
            Trc20Contract token = new Trc20Contract(contract, fromAddress, client);
            String transfer;
            // 获取转账账户的TRX余额
            BigInteger trc20Value = token.balanceOf(fromAddress);
            // 获取想要转账的数额
            BigInteger sunAmountValue = Convert.toSun(amount, Convert.Unit.TRX).toBigInteger();
            // 进行比较
            if (trc20Value.compareTo(sunAmountValue) >= 0) {
                log.info("开始转账.........");
                // 设置最大矿工费用
                long feeLimit = Convert.toSun("100", Convert.Unit.TRX).longValue();
                //转账
                transfer = token.transfer(toAddress, sunAmountValue.longValue(), 0, amount.stripTrailingZeros().toPlainString()+"trx闪兑", feeLimit);
            } else {
                return "error_error";
            }
            if (StrUtil.isEmpty(transfer)) {
                return "error_error";
            }
            log.info("主动转账usdt返回交易ID:{}", transfer);
//            client.close();
            return transfer;
        } catch (Exception ex) {
            String message = ex.getMessage();
            return "error_" + message;
        }
    }
    /**
     * TRX转账
     * @param
     * @param toAddress 转账地址
     * @param amount 金额
     * @return
     */
    public String transferTRX( String toAddress, BigDecimal amount) {
        String fromAddress = tronProperties.getWalletAddress();
        ApiWrapper wrapper = getApiWrapper();
        try {
            BigDecimal divisor = new BigDecimal(SCALE);
            Long rechangeAmount = amount.multiply(divisor).longValue();
            // 创建交易
            Response.TransactionExtention transaction = wrapper.transfer(fromAddress, toAddress, rechangeAmount);
            log.info("transaction:",transaction);
            log.info("transaction.Txid:"+transaction.getTxid());
            // 签名交易
            Chain.Transaction signTransaction = wrapper.signTransaction(transaction);
            log.info("signTransaction:{}", signTransaction);
            // 计算交易所需要的宽带
            long byteSize = wrapper.estimateBandwidth(signTransaction);
            log.info("byteSize:{}", byteSize);
            // 广播交易
            String hashTx = wrapper.broadcastTransaction(signTransaction);
            log.info("hashTRX:{}", hashTx);
            return hashTx;
        } catch (Exception e) {
            log.error("TransactionService#transfer error: {}", e.getMessage());
        }
//        wrapper.close();
        return null;
    }
}
