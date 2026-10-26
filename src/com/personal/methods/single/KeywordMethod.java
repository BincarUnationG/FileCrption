package com.personal.methods.single;

import com.personal.CryptoCategory;
import com.personal.CryptoContext;
import com.personal.CryptoMethod;

import java.io.*;

/**
 * 关键词密码（字节级）：用关键词构建一个 256 项的置换表，对每个字节整体替换。
 * 加密: byte -> subst[byte]；解密: subst[byte] -> byte（用逆表）。
 * 与凯撒一致，可处理任意字节（含 UTF-8 中文）。密钥是关键词。
 */
public class KeywordMethod implements CryptoMethod {

    @Override public String getName() { return "关键词密码"; }
    @Override public CryptoCategory getCategory() { return CryptoCategory.CLASSICAL_SINGLE; }
    @Override public String getDescription() { return "字节级关键词置换密码，密钥是关键词"; }
    @Override public boolean canEncrypt() { return true; }
    @Override public boolean canDecrypt() { return true; }
    @Override public String getEncryptedSuffix() { return ".kwc"; }
    @Override public String[] getExtraParamNames()    { return new String[]{"关键词"}; }
    @Override public String[] getExtraParamDefaults() { return new String[]{"keyword"}; }

    @Override
    public void encrypt(File in, File out, CryptoContext ctx) throws Exception {
        byte[] subst = buildSubstitution(ctx.getExtra("关键词", "keyword"));
        transform(in, out, subst, true);
    }

    @Override
    public void decrypt(File in, File out, CryptoContext ctx) throws Exception {
        byte[] subst = buildSubstitution(ctx.getExtra("关键词", "keyword"));
        transform(in, out, subst, false);
    }

    /**
     * 用关键词构建 256 项置换表：关键词中出现的字节去重后排在前面，
     * 其余 0-255 中未出现的字节按升序补全。这样 0-255 恰好各出现一次（双射）。
     */
    private byte[] buildSubstitution(String keyword) {
        byte[] subst = new byte[256];
        boolean[] used = new boolean[256];
        int idx = 0;
        if (keyword != null && !keyword.isEmpty()) {
            for (byte b : keyword.getBytes()) {
                int v = b & 0xFF;
                if (!used[v]) { used[v] = true; subst[idx++] = (byte) v; }
            }
        }
        for (int v = 0; v < 256; v++) {
            if (!used[v]) subst[idx++] = (byte) v;
        }
        return subst;
    }

    private void transform(File in, File out, byte[] subst, boolean encrypt) throws Exception {
        byte[] map = new byte[256];
        if (encrypt) {
            System.arraycopy(subst, 0, map, 0, 256);
        } else {
            for (int v = 0; v < 256; v++) map[subst[v] & 0xFF] = (byte) v;  // 逆表
        }
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(in));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out))) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = bis.read(buf)) != -1) {
                for (int i = 0; i < len; i++) {
                    buf[i] = map[buf[i] & 0xFF];
                }
                bos.write(buf, 0, len);
            }
        }
    }
}
