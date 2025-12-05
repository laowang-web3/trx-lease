package com.laowang.coinbank.service;

import com.google.protobuf.ByteString;
import com.laowang.coinbank.common.SymbolTypeEnum;
import com.laowang.coinbank.config.MonitorProperties;
import com.laowang.coinbank.config.TelegramBotProperties;
import com.laowang.coinbank.utils.TronUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.tron.trident.abi.TypeDecoder;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.NumericType;
import org.tron.trident.api.GrpcAPI;
import org.tron.trident.api.WalletGrpc;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.utils.ByteArray;
import org.tron.trident.crypto.Hash;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class MonitorService {
    @Autowired
    private  ExchangeService exchangeService;
    @Autowired
    private  TronService tronService;
    // 注入配置类
    @Autowired
    private MonitorProperties monitorProperties;
    @Autowired
    private TelegramBotProperties telegramBotProperties;

    @Async("monitorExecutor")
    public void processBlock(WalletGrpc.WalletBlockingStub blockingStub,long blockHeight) {
        try {
            // 获取区块信息
            GrpcAPI.NumberMessage request = GrpcAPI.NumberMessage.newBuilder().setNum(blockHeight).build();
            Chain.Block block = blockingStub.getBlockByNum(request);
//            log.info("\n处理区块 #" + blockHeight + "，包含 " + block.getTransactionsCount() + " 笔交易");
            if (block == null || block.getTransactionsCount() == 0) {
                return;
            }
            // 遍历区块中的所有交易
            for (Chain.Transaction tx : block.getTransactionsList()) {
                processTransaction(blockHeight,tx);
            }
        } catch (Exception e) {
            log.error("处理区块 #" + blockHeight + " 时出错: " + e.getMessage());
        }
    }

    private void processTransaction(long blockId,Chain.Transaction tx) {
        try {
            // 交易ID
            String txId = Hex.toHexString(Hash.sha256(tx.getRawData().toByteArray()));
            // 遍历交易中的所有合约
            for ( Chain.Transaction.Contract contract : tx.getRawData().getContractList()) {
                // 只处理转账合约
                if (contract.getType() == Chain.Transaction.Contract.ContractType.TransferContract) {
                    Contract.TransferContract transferContract = contract.getParameter().unpack(Contract.TransferContract.class);
                    // 转换地址格式
                    String fromAddress = TronUtils.hexToBase58(ByteArray.toHexString(transferContract.getOwnerAddress().toByteArray()));
                    String toAddress = TronUtils.hexToBase58(ByteArray.toHexString(transferContract.getToAddress().toByteArray()));
                    long amount = transferContract.getAmount(); // SUN单位，1 TRX = 1e6 SUN

                    // 检查是否是监控地址相关的交易
                    if (toAddress.equals(telegramBotProperties.getCollectMoneyAddress())) {
                        BigDecimal trc20Decimals = new BigDecimal(tronService.SCALE);
                        log.info("=== 检测到TRX转账交易 ===");
                        log.info("交易ID: " + txId);
                        log.info("从地址: " + fromAddress);
                        log.info("到地址: " + toAddress);
                        BigDecimal trxAmount = BigDecimal.valueOf(amount).divide(trc20Decimals,6,BigDecimal.ROUND_DOWN);
                        log.info("金额: " + trxAmount.toPlainString()+ " TRX");
                        log.info("---------TRX------------");
                        exchangeService.distribute(blockId,txId, SymbolTypeEnum.TRX,fromAddress,toAddress,trxAmount);
                    }
                }
                if (contract.getType() == Chain.Transaction.Contract.ContractType.TriggerSmartContract) {
                    try {
                        // 合约地址
                        Contract.TriggerSmartContract contractStr = contract.getParameter().unpack(Contract.TriggerSmartContract.class);
                        ByteString contractAddress = contractStr.getContractAddress();
                        String hex = ApiWrapper.toHex(contractAddress);
                        String contractAddressStr = TronUtils.hexToBase58(hex);
                        if (monitorProperties.getTrc20UsdtAddress().equalsIgnoreCase(contractAddressStr)) {
                            // 转换地址格式
                            ByteString ownerAddress = contractStr.getOwnerAddress();
                            String hex2 = ApiWrapper.toHex(ownerAddress);
                            String fromAddress = TronUtils.hexToBase58(hex2);
                            ByteString data = contractStr.getData();
                            String hex1 = ApiWrapper.toHex(data);
                            // 交易类型是转账
                            String transferFunction = Hex.toHexString(new Keccak.Digest256().digest("transfer(address,uint256)".getBytes())).substring(0, 8);
                            String funcId = hex1.substring(0, 8);
                            if (!transferFunction.equals(funcId)) {
                                continue;
                            }
                            String toAddress = hex1.substring(32, 72);
                            String amount = hex1.substring(72, 136);
                            try {
                                Address address = (Address) TypeDecoder.instantiateType("address", toAddress);
                                // 地址
                                NumericType amountType = (NumericType) TypeDecoder.instantiateType("uint256", amount);
                                // 金额
                                BigDecimal trc20Decimals = new BigDecimal(tronService.SCALE);
                                BigDecimal amountee = new BigDecimal(amountType.getValue()).divide(trc20Decimals, 6, RoundingMode.FLOOR);
                                // 检查是否是监控地址相关的交易
                                if (address.getValue().equals(telegramBotProperties.getCollectMoneyAddress())) {
                                    log.info("=== 检测到USDT转账交易 ===");
                                    log.info("交易ID: " + txId);
                                    log.info("从地址: " + fromAddress);
                                    log.info("到地址: " + address);
                                    log.info("金额: " + amountee.toPlainString()+" USDT");
                                    log.info("---------USDT------------");
                                    exchangeService.distribute(blockId,txId, SymbolTypeEnum.USDT,fromAddress,address.toString(),amountee);
                                }
                            } catch (Exception e) {
                                log.error("----TRON监控报错----", e);
                            }
                        }
                    }catch (com.google.protobuf.InvalidProtocolBufferException e){

                    }

                }
            }
        } catch (Exception e) {
            log.error("处理交易时出错: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
