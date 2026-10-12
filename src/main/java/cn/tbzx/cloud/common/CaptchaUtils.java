package cn.tbzx.cloud.common;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 图形验证码工具: 生成随机字符, 渲染成带干扰线/噪点的 PNG (返回 data URI)
 * headless 环境下安全可用 (只依赖 java.awt, 不开窗口)
 */
public final class CaptchaUtils {

    /** 字母表: 去掉易混的 0 O 1 I (保留 L) */
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RND = new SecureRandom();
    private static final int W = 120;
    private static final int H = 40;

    private CaptchaUtils() {
    }

    public static String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHABET[RND.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    public static String toPngDataUri(String code) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(0x0E2A4A));
            g.fillRect(0, 0, W, H);

            // 干扰线
            for (int i = 0; i < 6; i++) {
                g.setColor(randColor(110, 180));
                g.setStroke(new BasicStroke(1.2f));
                g.drawLine(RND.nextInt(W), RND.nextInt(H), RND.nextInt(W), RND.nextInt(H));
            }
            // 噪点
            for (int i = 0; i < 90; i++) {
                g.setColor(randColor(120, 210));
                g.fillRect(RND.nextInt(W), RND.nextInt(H), 2, 2);
            }
            // 字符 (逐个轻微旋转, 位置抖动)
            int n = code.length();
            int step = (W - 20) / n;
            for (int i = 0; i < n; i++) {
                g.setColor(randColor(175, 255));
                g.setFont(new Font("SansSerif", Font.BOLD, 26 + RND.nextInt(4)));
                int x = 12 + i * step;
                int y = 29 + RND.nextInt(6) - 3;
                double angle = (RND.nextDouble() - 0.5) * 0.5;
                AffineTransform old = g.getTransform();
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.setTransform(old);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码渲染失败", e);
        }
    }

    private static Color randColor(int min, int max) {
        int span = max - min;
        return new Color(min + RND.nextInt(span), min + RND.nextInt(span), min + RND.nextInt(span));
    }
}
