package com.personal.methods.classical.transposition;

import com.personal.CryptoCategory;
import com.personal.CryptoContext;
import com.personal.CryptoMethod;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 头部存储真实长度，代替专属填充标记
 * 最小单元：1字节
 * 密钥：决定读取列的顺序，密钥长度 = 矩阵列数
 * 加密：数据按行填入矩阵 → 按密钥顺序逐列读出
 * 解密：把密文按密钥顺序回填到对应列 → 按行读出 → 去掉填充
 */
public class ColumnarMethod implements CryptoMethod {
    @Override public boolean canDecrypt() {return true;}
    @Override public boolean canEncrypt() {return true;}
    @Override public String getName() {return "列置换密码";}
    @Override public CryptoCategory getCategory() {return CryptoCategory.CLASSICAL_TRANSPOSITION;}
    @Override public String getDescription() {return "字节级列置换密码，需要关键词或密钥,用数字表示需用空格隔开";}
    @Override public String getEncryptedSuffix() {return ".cln";}
    @Override public String[] getExtraParamNames() {return new String[]{"关键词"};}
    @Override public String[] getExtraParamDefaults() {return new String[]{"keyword"};}

    @Override
    public void encrypt(File in, File out, CryptoContext ctx) throws Exception {
        String keywords = ctx.getExtra("关键词", "keyword");
        int[] orderArr = shift(keywords);
        transfer(in, out, orderArr, true);
    }

    @Override
    public void decrypt(File in, File out, CryptoContext ctx) throws Exception {
        String keywords = ctx.getExtra("关键词", "keyword");
        int[] orderArr = shift(keywords);
        transfer(in, out, orderArr, false);
    }

    /**
     * 关键词预处理
     *
     */
    private int[] shift(String keywords) {
        char[] chs = keywords.toCharArray();

        for (int i = 0; i < chs.length; i++) {
            if (!((chs[i] >= 'a' && chs[i] <= 'z') || (chs[i] >= 'A' && chs[i] <= 'Z') || (chs[i] >= '0' && chs[i] <= '9'))) {
                chs[i] = ' ';
            }
        }

        String[] tempStr = new String(chs).trim().split("\\s+");
        int totalLen = 0;

        for (String str : tempStr) {
            if (!(str.matches("\\d+"))) {
                totalLen += str.length();
            } else totalLen++;
        }

        int[] originRes = new int[totalLen];
        int count = 0;

        for (String str : tempStr) {
            if (str.matches("\\d+")) {
                originRes[count++] = Integer.parseInt(str);
            } else {
                char[] strChs = str.toCharArray();
                TreeSet<Character> sortedCharSet = new TreeSet<>();

                for (char c : strChs) sortedCharSet.add(c);

                Map<Character, Integer> rankMap = new HashMap<>();
                int r = 0;

                for (char c : sortedCharSet) {
                    rankMap.put(c, r++);
                }

                for (char c : strChs) {
                    originRes[count++] = rankMap.get(c);
                }
            }
        }
        List<Integer> arr = Arrays.stream(originRes).boxed().collect(Collectors.toList());
        boolean changed;
        do {
            changed = false;
            int len = arr.size();
            List<Integer> next = new ArrayList<>();
            for (int num : arr) {
                if (num < len) {
                    next.add(num);
                } else {
                    changed = true;
                }
            }
            arr = next;
        } while (changed);
        return arr.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 置换主逻辑
     * @param direct true=加密 false=解密
     */
    private void transfer(File in, File out, int[] orderArr, boolean direct) throws Exception {
        int blockSize = orderArr.length;
        int cow = (4096 + blockSize - 1) / blockSize;
        int matrixTotal = cow * blockSize;

        for (int col : orderArr) {
            if (col < 0 || col >= blockSize) {
                throw new IllegalArgumentException("密钥列序号越界：" + col + "，总列数：" + blockSize);
            }
        }

        byte[] readBuf = new byte[4096];
        int readLen;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(in));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out))) {
            while ((readLen = bis.read(readBuf, 2, 4094)) != -1) {
                if (readLen <= 0) continue;

                readBuf[0] = (byte) (readLen >>> 8);
                readBuf[1] = (byte) (readLen & 0xFF);
                Arrays.fill(readBuf, 2 + readLen, 4096, (byte) 0x00);

                byte[][] bufMatrix = new byte[cow][blockSize];
                int pos = 0;

                for (; pos < readBuf.length; pos++) {
                    int row = pos / blockSize;
                    int col = pos % blockSize;
                    bufMatrix[row][col] = readBuf[pos];
                }

                for (; pos < matrixTotal; pos++) {
                    int row = pos / blockSize;
                    int col = pos % blockSize;
                    bufMatrix[row][col] = 0x00;
                }

                byte[][] transedMatrix = transform(bufMatrix, orderArr, direct);
                for (byte[] row : transedMatrix) {
                    bos.write(row);
                }
            }
        }
    }

    /**
     * 核心列置换变换
     * @param matrix 原始矩阵
     * @param orderArr 密钥顺序
     * @param direct true=加密，false=解密
     */
    private byte[][] transform(byte[][] matrix, int[] orderArr, boolean direct) {
        int rowCount = matrix.length;
        int colCount = matrix[0].length;

        if (direct) {
            byte[][] cipherMatrix = new byte[rowCount][colCount];
            int pos = 0;

            for (int col : orderArr) {
                for (int r = 0; r < rowCount; r++) {
                    int tr = pos / colCount;
                    int tc = pos % colCount;
                    cipherMatrix[tr][tc] = matrix[r][col];
                    pos++;
                }
            }
            return cipherMatrix;
        } else {
            byte[][] plainMatrix = new byte[rowCount][colCount];
            int pos = 0;

            for (int col : orderArr) {
                for (int r = 0; r < rowCount; r++) {
                    int srcR = pos / colCount;
                    int srcC = pos % colCount;
                    byte b = matrix[srcR][srcC];
                    plainMatrix[r][col] = b;
                    pos++;
                }
            }
            return plainMatrix;
        }
    }
}
