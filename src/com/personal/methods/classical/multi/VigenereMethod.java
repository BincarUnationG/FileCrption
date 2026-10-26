package com.personal.methods.classical.multi;

import com.personal.CryptoCategory;
import com.personal.CryptoContext;
import com.personal.CryptoMethod;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Vigenere密码（字节级）：vigenere表本质行列编码相加。
 */
public class VigenereMethod implements CryptoMethod {
    @Override public String getName(){ return "Vigenere密码";}
    @Override public CryptoCategory getCategory() { return CryptoCategory.CLASSICAL_MULTI; }
    @Override public String getDescription(){ return "多表代替密码，选择关键词和明文查表";}
    @Override public boolean canEncrypt() { return true; }
    @Override public boolean canDecrypt(){ return true; }
    @Override public String getEncryptedSuffix(){ return ".vig";}
    @Override public String[] getExtraParamNames(){ return new String[]{"关键词"}; }
    @Override public String[] getExtraParamDefaults(){return new String[]{"MEC"};}

    @Override public void encrypt(File in, File out, CryptoContext ctx) throws Exception{
        transform(in,out,ctx.getExtra("关键词","MEC"),true);
    }

    @Override public void decrypt(File in, File out, CryptoContext ctx) throws Exception{
        transform(in,out,ctx.getExtra("关键词","MEC"),false);
    }

    private void transform(File in,File out,String keywords,boolean encrypt) throws Exception{
        byte[] key = keywords.getBytes(StandardCharsets.UTF_8);
        int kl = key.length;
        int pos=0;
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(in));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out))
        ){
            byte[] buf = new byte[4096];
            int len;
            while ((len = bis.read(buf)) != -1){
                for(int i=0;i<len;i++){
                    int ki = key[pos % kl] & 0xFF;
                    int v = buf[i] & 0xFF;
                    buf[i] = (byte)(encrypt?(v+ki) & 0xFF:(v-ki+256) & 0xFF);
                    pos++;
                }
                bos.write(buf,0,len);
            }
        }
    }

}