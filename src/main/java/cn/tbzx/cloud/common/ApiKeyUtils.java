package cn.tbzx.cloud.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 设备凭证工具: 设备编号 / ApiKey 生成 + SHA-256 (云端只存哈希)
 */
public class ApiKeyUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** 设备编号: TBZX- + 12 位大写 HEX */
    public static String newDeviceId() {
        return "TBZX-" + randomHex(12).toUpperCase();
    }

    /** 设备上报密钥: 32 位小写 HEX, 明文仅注册时返回一次 */
    public static String newApiKey() {
        return randomHex(32);
    }

    public static String sha256Hex(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String randomHex(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(HEX[RANDOM.nextInt(16)]);
        }
        return sb.toString();
    }

    private ApiKeyUtils() {
    }
}
