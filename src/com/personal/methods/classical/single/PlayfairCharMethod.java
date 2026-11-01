package com.personal.methods.classical.single;

import com.personal.CryptoCategory;
import com.personal.CryptoContext;
import com.personal.CryptoMethod;
import com.personal.utils.PairStreamProcessor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Playfair 密码（字符级）：5×5 矩阵，I/J 合并，只替换 A-Z 字母。
 * 你自己填实现，我负责检查和讲思路。
 */
public class PlayfairCharMethod implements CryptoMethod {

    private static final char PAD = 'X';
    private char[][] forwardTable;
    private final int[] rowOf = new int[26];//行表
    private final int[] colOf = new int[26];//列表
    boolean[] checkTable = new boolean[26];
    private char carry = 0;

    @Override public String getName() { return "Playfair密码"; }
    @Override public CryptoCategory getCategory() { return CryptoCategory.CLASSICAL_SINGLE; }
    @Override public String getDescription() { return "字符级Playfair，5×5矩阵，I/J合并"; }
    @Override public boolean canEncrypt() { return true; }
    @Override public boolean canDecrypt() { return true; }
    @Override public String getEncryptedSuffix() { return ".pfc"; }
    @Override public String[] getExtraParamNames()    { return new String[]{"关键词"}; }
    @Override public String[] getExtraParamDefaults() { return new String[]{"PLAYFAIR"}; }

    @Override public void encrypt(File in, File out, CryptoContext ctx) throws Exception {
        String keyword = ctx.getExtra("关键词", "PLAYFAIR");
        fillKeyTable(CharDeduplicate(keyword));
        transfer(in,out,true);
    }

    @Override public void decrypt(File in, File out, CryptoContext ctx) throws Exception {
        String keyword = ctx.getExtra("关键词", "PLAYFAIR");
        fillKeyTable(CharDeduplicate(keyword));
        transfer(in,out,false);
    }

    /** 关键词字符去重与合并 */
    private String CharDeduplicate(String keywords) {

        StringBuilder keywordBuilder = new StringBuilder();

        for(char c:keywords
                .toUpperCase()
                .replaceAll("[^A-Z]", "").
                toCharArray()
        ) {
            // 合并IJ
            if (c == 'J') c = 'I';
            if (!checkTable[c - 'A']) {
                checkTable[c - 'A'] = true;
                keywordBuilder.append(c);
            }
        }
        return keywordBuilder.toString();
    }

    /**
     * 填充加解密矩阵，并建立字符→坐标的反查索引。
     * @param keywords 进过CharDeduplicate预处理的关键词：已大写、去重、J 已合并为 I、长度 ≤ 25
     */
    private void fillKeyTable(String keywords) {
        char[] key = keywords.toCharArray();
        if (key.length > 25) {
            throw new IllegalArgumentException("关键词去重后不能超过 25 个: " + key.length);
        }

        for (char c : key) {
            if (c < 'A' || c > 'Z' || c == 'J') {
                throw new IllegalArgumentException("关键词必须是去重后的大写字母且不含 J，非法字符: " + c);
            }
        }

        char[][] forwardTable = new char[5][5];
        int k = 0;

        for (char c : key) {
            forwardTable[k / 5][k % 5] = c;
            k++;
        }

        for (char c = 'A'; c <= 'Z' && k < 25; c++) {
            if (c == 'J') continue;
            if (!checkTable[c - 'A']) {
                forwardTable[k / 5][k % 5] = c;
                k++;
            }
        }

        if (k != 25) {
            throw new IllegalStateException("矩阵未填满，k = " + k);
        }

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                char ch = forwardTable[r][c];
                rowOf[ch - 'A'] = r;
                colOf[ch - 'A'] = c;
            }
        }

        this.forwardTable = forwardTable;
    }

    /**
     * 这波神力，忘了取模是有负数的
     */
    private int shift(int pos, boolean direct) {
        return (pos + (direct ? 1 : 4)) % 5;   // 加密+1，解密+4(即-1)
    }

    /**
     * 加解密
     * @param in 输入
     * @param out 输出
     * @param direct true为加密，false为解密
     */
    private void transfer(File in,File out,boolean direct) throws Exception{
        try (Reader br = new BufferedReader(new FileReader(in));
             Writer bw = new BufferedWriter(new FileWriter(out))) {

            char[] chs = new char[1024];
            int len;
            List<char[]> parts = new ArrayList<>();
            PairStreamProcessor psp = new PairStreamProcessor();
            while ((len = br.read(chs)) != -1) {
                for (int i = 0; i < len; i++) {
                    char c = chs[i];
                    if (c >= 'a' && c <= 'z') {
                        chs[i] = (char) (c - 32); // 小写转大写
                    }
                    if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                        chs[i] = ' ';
                    }
                }
                String tempStr = new String(chs, 0, len).replace(" ", "");
                char[] cleanArr = tempStr.toCharArray();

                psp.processBlock(cleanArr,parts);

            }
            psp.finish(parts);
            for (char[] group : parts) {
                if(rowOf[group[0]- 'A']==rowOf[group[1]-'A']) {
                    group[0] = forwardTable[rowOf[group[0]-'A']][shift(colOf[group[0]-'A'], direct)];
                    group[1] = forwardTable[rowOf[group[1]-'A']][shift(colOf[group[1]-'A'], direct)];
                }else if(colOf[group[0]-'A']==colOf[group[1]-'A']) {
                    group[0] = forwardTable[shift(rowOf[group[0]-'A'],direct)][colOf[group[0]- 'A']];
                    group[1] = forwardTable[shift(rowOf[group[1]-'A'],direct)][colOf[group[1]- 'A']];
                }else{
                    group[0] = forwardTable[rowOf[group[0]-'A']][colOf[group[1]- 'A']];
                    group[1] = forwardTable[rowOf[group[1]-'A']][colOf[group[0]- 'A']];
                }
            }
            for (char[] group : parts) {
                bw.write(group);
            }
        }
    }
}
