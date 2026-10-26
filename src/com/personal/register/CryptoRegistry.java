package com.personal.register;

import com.personal.CryptoCategory;
import com.personal.CryptoMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CryptoRegistry {

    public static List<CryptoMethod> all() {
        List<CryptoMethod> list = new ArrayList<>();


        // ===== 按教材顺序逐步放开注释 =====
        // 古典-单表
        list.add(new com.personal.methods.classical.single.CaesarMethod());
        list.add(new com.personal.methods.classical.single.KeywordMethod());
        list.add(new com.personal.methods.classical.single.AffineMethod());
        // 古典-多表
        list.add(new com.personal.methods.classical.multi.VigenereMethod());
        // 对称
        // list.add(new com.personal.methods.symmetric.DesMethod());
        // list.add(new com.personal.methods.symmetric.AesMethod());
        // 哈希
        // list.add(new com.personal.methods.hash.Md5Method());
        // list.add(new com.personal.methods.hash.Sha256Method());
        // 公钥
        // list.add(new com.personal.methods.asymmetric.RsaMethod());
        // 签名
        // list.add(new com.personal.methods.signature.RsaSignatureMethod());

        return list;
    }

    public static List<CryptoMethod> byCategory(CryptoCategory cat) {
        return all().stream()
                .filter(m -> m.getCategory() == cat)
                .collect(Collectors.toList());
    }
}