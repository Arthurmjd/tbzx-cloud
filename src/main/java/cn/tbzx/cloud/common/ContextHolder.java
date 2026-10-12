package cn.tbzx.cloud.common;

/**
 * 请求级上下文: 拦截器解析鉴权信息后写入, afterCompletion 清理
 * - userId: 家属 JWT 登录用户
 * - deviceId: 设备 Api-Key 鉴权通过的设备编号
 */
public class ContextHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> DEVICE_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setDeviceId(String deviceId) {
        DEVICE_ID.set(deviceId);
    }

    public static String getDeviceId() {
        return DEVICE_ID.get();
    }

    public static void clearAll() {
        USER_ID.remove();
        DEVICE_ID.remove();
    }
}
