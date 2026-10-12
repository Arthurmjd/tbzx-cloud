package cn.tbzx.cloud.common;

/**
 * 地理计算工具: GCJ-02 坐标, haversine 球面距离
 */
public class GeoUtils {

    private static final double EARTH_RADIUS_M = 6371000.0;

    /** 两点间距离 (米) */
    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
    }

    /** 点是否在圆形区域内 */
    public static boolean insideCircle(double lat, double lng, double centerLat, double centerLng, double radiusM) {
        return haversineMeters(lat, lng, centerLat, centerLng) <= radiusM;
    }

    private GeoUtils() {
    }
}
