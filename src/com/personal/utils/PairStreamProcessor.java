package com.personal.utils;

import java.util.List;

/**
 * 对文件内的明文读取(这板只读字符)
 * 去除其他字符只留字母
 * 两两一组，组内重复则插入'X'
 * 少也补上'X' (分组需要新建stream)
 */
public class PairStreamProcessor {
    private char carry = 0;

    public void processBlock(char[] block, List<char[]> groupList) {
        if (block == null || block.length == 0) return;
        int idx = 0;
        char c1;
        if (carry != 0) {
            c1 = carry;
            carry = 0;
            if (idx < block.length) {
                char c2 = block[idx++];
                char[] g = new char[2];
                if (c1 == c2) {
                    g[0] = c1;
                    g[1] = 'X';
                    carry = c2;
                } else {
                    g[0] = c1;
                    g[1] = c2;
                    carry = 0;
                }
                groupList.add(g);
            } else {
                carry = c1;
                return;
            }
        }

        while (idx < block.length) {
            c1 = block[idx++];
            if (idx < block.length) {
                char c2 = block[idx++];
                char[] g = new char[2];
                if (c1 == c2) {
                    g[0] = c1;
                    g[1] = 'X';
                    carry = c2;
                } else {
                    g[0] = c1;
                    g[1] = c2;
                    carry = 0;
                }
                groupList.add(g);
            } else {
                carry = c1;
                break;
            }
        }
    }


    public void finish(List<char[]> groupList) {
        if (carry != 0) {
            char[] g = {carry, 'X'};
            groupList.add(g);
            carry = 0;
        }
    }
}