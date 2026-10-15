package com.personal;

import java.io.File;

/**
 * 所有算法统一接口。
 * 你只需实现本算法"能做的事"，其他方法有默认实现，会自动抛出"不支持"。
 * GUI 会根据 canXxx() / needsXxx() 自动调整按钮和输入框。
 */
public interface CryptoMethod {

    // ========== 基本信息 ==========
    String getName();

    default CryptoCategory getCategory() { return CryptoCategory.SYMMETRIC; }

    default String getDescription() { return ""; }

    // ========== 能力声明 ==========
    default boolean canEncrypt() { return false; }
    default boolean canDecrypt() { return false; }
    default boolean canHash()    { return false; }
    default boolean canSign()    { return false; }
    default boolean canVerify()  { return false; }

    // ========== 输入需求 ==========
    default boolean needsPassword()   { return false; }
    default boolean needsKeyFile()    { return false; }   // 对称密钥文件
    default boolean needsPublicKey()  { return false; }   // 公钥文件
    default boolean needsPrivateKey() { return false; }   // 私钥文件

    /** 算法自定义的额外参数名，GUI 会为每个名字生成一个输入框 */
    default String[] getExtraParamNames()    { return new String[0]; }
    default String[] getExtraParamDefaults() { return new String[0]; }

    // ========== 文件后缀 ==========
    default String getEncryptedSuffix() { return ".enc"; }
    default String getHashSuffix()      { return ".hash"; }
    default String getSignatureSuffix() { return ".sig"; }

    // ========== 输出路径推导 ==========
    default File getDecryptTarget(File input) {
        String absolutePath = input.getAbsolutePath();
        String suf = getEncryptedSuffix();
        if (absolutePath.toLowerCase().endsWith(suf.toLowerCase())) {
            return new File(absolutePath.substring(0, absolutePath.length() - suf.length()));
        }
        return new File(absolutePath + ".dec");
    }

    default File getHashTarget(File input) {
        return new File(input.getAbsolutePath() + getHashSuffix());
    }

    default File getSignatureTarget(File input) {
        return new File(input.getAbsolutePath() + getSignatureSuffix());
    }

    // ========== 核心操作：默认抛"不支持"，子类按需覆盖 ==========
    default void encrypt(File in, File out, CryptoContext ctx) throws Exception {
        throw new UnsupportedOperationException(getName() + " 不支持加密");
    }

    default void decrypt(File in, File out, CryptoContext ctx) throws Exception {
        throw new UnsupportedOperationException(getName() + " 不支持解密");
    }

    default void hash(File in, File out, CryptoContext ctx) throws Exception {
        throw new UnsupportedOperationException(getName() + " 不支持哈希");
    }

    default void sign(File in, File out, CryptoContext ctx) throws Exception {
        throw new UnsupportedOperationException(getName() + " 不支持签名");
    }

    /**
     * 验证。
     */
    default boolean verify(File in, File sigFile, CryptoContext ctx) throws Exception {
        throw new UnsupportedOperationException(getName() + " 不支持验证");
    }
}