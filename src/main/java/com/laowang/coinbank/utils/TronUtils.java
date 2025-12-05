package com.laowang.coinbank.utils;


import org.bouncycastle.util.encoders.Hex;
import org.tron.trident.core.utils.Base58;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class TronUtils {
    // TRON地址前缀（41表示主网）58
    private static final String TRON_PREFIX_58 = "41";
    // TRON地址前缀字节（41对应主网）16进制
    private static final byte TRON_PREFIX_16 = 0x41;

    public static String hexToBase58(String hexString) {
        String prefixedHex = hexString;
        if(!hexString.contains(TRON_PREFIX_58)){
            // 添加TRON地址前缀
             prefixedHex = TRON_PREFIX_58 + hexString;
        }

        byte[] bytes = Hex.decode(prefixedHex);

        // 计算双SHA256校验码
        byte[] hash1 = sha256(bytes);
        byte[] hash2 = sha256(hash1);

        // 组合原始字节和校验码（取前4字节）
        byte[] checksum = new byte[4];
        System.arraycopy(hash2, 0, checksum, 0, 4);

        byte[] addressWithChecksum = new byte[bytes.length + 4];
        System.arraycopy(bytes, 0, addressWithChecksum, 0, bytes.length);
        System.arraycopy(checksum, 0, addressWithChecksum, bytes.length, 4);

        // Base58编码
        return Base58.encode(addressWithChecksum);
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }


    public static String base58ToHex(String base58Address) {
        // Base58解码
        byte[] decoded = Base58.decode(base58Address);

        // 分离原始数据和校验码（最后4字节）
        byte[] data = Arrays.copyOfRange(decoded, 0, decoded.length - 4);
        byte[] checksum = Arrays.copyOfRange(decoded, decoded.length - 4, decoded.length);

        // 验证校验码
        byte[] hash1 = sha256(data);
        byte[] hash2 = sha256(hash1);
        byte[] calculatedChecksum = Arrays.copyOfRange(hash2, 0, 4);

        if (!Arrays.equals(checksum, calculatedChecksum)) {
            throw new IllegalArgumentException("Base58校验和验证失败");
        }

        // 移除TRON前缀并转为16进制
        if (data[0] != TRON_PREFIX_16) {
            throw new IllegalArgumentException("非TRON主网地址");
        }

        byte[] hexBytes = Arrays.copyOfRange(data, 1, data.length);
        return bytesToHex(hexBytes);
    }


    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 判断 dividend 对 divisor 取模是否为0
     * @param dividend 被除数
     * @param divisor 除数
     * @return 如果取模结果为0则返回true，否则返回false
     */
    public static Boolean isModuloZero(BigDecimal dividend, BigDecimal divisor) {
        // 检查除数是否为0
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        // 计算余数
        BigDecimal remainder = dividend.remainder(divisor);
        // 比较余数是否为0
        return remainder.compareTo(BigDecimal.ZERO) == 0;
    }

    public static void main(String[] args) {
        TronUtils tronUtils = new TronUtils();
        tronUtils.isModuloZero(new BigDecimal("6"),new BigDecimal("3"));
    }
}