package com.personal;

public enum CryptoCategory {
    SYMMETRIC("对称加密"),
    ASYMMETRIC("非对称加密"),
    HASH("哈希"),
    MAC("消息认证"),
    SIGNATURE("数字签名"),
    ENCODING("编码"),
    CLASSICAL_SINGLE("古典-单表代换"),
    CLASSICAL_MULTI("古典-多表代换"),
    CLASSICAL_TRANSPOSITION("古典-置换");

    private final String label;
    CryptoCategory(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}