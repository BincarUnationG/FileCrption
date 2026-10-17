package com.personal.methods.single;

import com.personal.CryptoCategory;
import com.personal.CryptoContext;
import com.personal.CryptoMethod;

import java.io.*;

public class CaesarMethod implements CryptoMethod {

    @Override public String getName() { return "凯撒密码"; }
    @Override public CryptoCategory getCategory() { return CryptoCategory.CLASSICAL_SINGLE; }
    @Override public String getDescription() { return "经典移位密码，密钥是 0-25 的位移量"; }
    @Override public boolean canEncrypt() { return true; }
    @Override public boolean canDecrypt() { return true; }
    @Override public String getEncryptedSuffix() { return ".caesar"; }
    @Override public String[] getExtraParamNames()    { return new String[]{"位移量"}; }
    @Override public String[] getExtraParamDefaults() { return new String[]{"3"}; }

    @Override
    public void encrypt(File in, File out, CryptoContext ctx) throws Exception {
        int k = ctx.getExtraInt("位移量", 3);
        transform(in, out, k);
    }

    @Override
    public void decrypt(File in, File out, CryptoContext ctx) throws Exception {
        int k = ctx.getExtraInt("位移量", 3);
        transform(in, out, -k);
    }

    private void transform(File in, File out, int shift) throws Exception {
        byte[] data = java.nio.file.Files.readAllBytes(in.toPath());
        for (int i = 0; i < data.length; i++) {
            data[i] += shift;
        }
        java.nio.file.Files.write(out.toPath(), data);
    }
}