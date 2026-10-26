package com.personal.methods.single;

import com.personal.CryptoCategory;
import com.personal.CryptoContext;
import com.personal.CryptoMethod;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * 仿射加密(字节级):
 *  加密：E(x) = (a·x + b) mod m(256)
 *  解密：D(y) = a⁻¹·(y − b) mod m(256)
 *  模乘法逆元有唯一解的充要条件是a和 m的最大公约数:gcd(a,m)=1。
 *  要求 a 与 256 互质（即 a 为奇数），否则无乘法逆元、解密一对多。
 */
public class AffineMethod implements CryptoMethod {

    @Override public String getName() { return "仿射密码"; }
    @Override public CryptoCategory getCategory() { return CryptoCategory.CLASSICAL_SINGLE; }
    @Override public String getDescription() { return "字节级仿射变换 E(x)=(a*x+b) mod 256，要求 a 为奇数"; }
    @Override public boolean canEncrypt() { return true; }
    @Override public boolean canDecrypt() { return true; }
    @Override public String getEncryptedSuffix() { return ".aff"; }
    @Override public String[] getExtraParamNames()    { return new String[]{"a", "b"}; }
    @Override public String[] getExtraParamDefaults() { return new String[]{"3", "5"}; }

    /** 求 gcd，判断 a 与 256 是否互质 */
    private boolean isCoprimeWith256(int a) {
        return gcd(a & 0xFF, 256) == 1;
    }

    private int gcd(int x, int y) {
        x = Math.abs(x);
        while (y != 0) {
            int t = x % y;
            x = y;
            y = t;
        }
        return x;
    }
    /** 求 a 在 mod 256 下的乘法逆元，a 为奇数；不存在返回 -1 */
    private int buildInverse(int a) {
        int av = a & 0xFF;
        if (!isCoprimeWith256(av)) return -1;
        for (int i = 1; i < 256; i += 2) {
            if (((av * i) & 0xFF) == 1) return i;
        }
        return -1;
    }

    /**
     * 单字节仿射映射。
     * @param v       原始字节值 0~255
     * @param a       系数 a（奇数）
     * @param bShift  偏移 b
     * @param inverse true=解密（用逆元），false=加密
     */
    private int mapByte(int v, int a, int bShift, boolean inverse) {
        int av = a & 0xFF;
        if (inverse) {
            int aInv = buildInverse(av);
            return (aInv * ((v - bShift + 256) & 0xFF)) & 0xFF;   // D(y)=a⁻¹(y−b)
        } else {
            return (av * v + bShift) & 0xFF;                     // E(x)=(ax+b)
        }
    }

    // ============ 文件流 IO ============

    @Override
    public void encrypt(File in, File out, CryptoContext ctx) throws Exception {
        int a = ctx.getExtraInt("a", 3);
        int b = ctx.getExtraInt("b", 5);
        if (buildInverse(a) < 0)
            throw new IllegalArgumentException("a 必须为奇数（与256互质），当前 a=" + a);
        transform(in, out, a, b, false);
    }

    @Override
    public void decrypt(File in, File out, CryptoContext ctx) throws Exception {
        int a = ctx.getExtraInt("a", 3);
        int b = ctx.getExtraInt("b", 5);
        if (buildInverse(a) < 0)
            throw new IllegalArgumentException("a 必须为奇数（与256互质），当前 a=" + a);
        transform(in, out, a, b, true);
    }

    /** 按字节流读取，对每个字节做仿射映射并写出 */
    private void transform(File in, File out, int a, int bShift, boolean inverse) throws Exception {
        int av = a & 0xFF;
        int bv = bShift & 0xFF;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(in));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out))) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = bis.read(buf)) != -1) {
                for (int i = 0; i < len; i++) {
                    buf[i] = (byte) mapByte(buf[i] & 0xFF, av, bv, inverse);
                }
                bos.write(buf, 0, len);
            }
        }
    }
}
