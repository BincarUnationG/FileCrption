package com.personal;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 参数容器。GUI 根据算法声明决定填什么，算法实现从里面取自己需要的。
 */
public class CryptoContext {

    public char[] password;         // 密码/口令
    public File keyFile;            // 对称密钥文件
    public File publicKeyFile;      // 公钥文件
    public File privateKeyFile;     // 私钥文件

    /** 额外参数：算法名 → 用户填的值（例如"位移量"、"密钥长度"） */
    public final Map<String, String> extra = new HashMap<>();

    public String getExtra(String key) { return extra.get(key); }

    public String getExtra(String key, String def) {
        return extra.getOrDefault(key, def);
    }

    public int getExtraInt(String key, int def) {
        try {
            String v = extra.get(key);
            return v == null || v.isEmpty() ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}