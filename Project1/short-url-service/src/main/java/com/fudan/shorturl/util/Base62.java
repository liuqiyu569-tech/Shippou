package com.fudan.shorturl.util;

/**
 * Base62 编解码工具
 * <p>
 * 字符集：0-9 A-Z a-z 共 62 个字符
 * 用途：将雪花算法生成的 long 类型 ID 转成短字符串作为短链 code
 */
public final class Base62 {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int BASE = 62;

    private Base62() {}

    public static String encode(long num) {
        if (num == 0) return "0";
        long n = num < 0 ? -num : num;
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            sb.append(ALPHABET[(int) (n % BASE)]);
            n /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String s) {
        long result = 0;
        for (char c : s.toCharArray()) {
            int v;
            if (c >= '0' && c <= '9') v = c - '0';
            else if (c >= 'A' && c <= 'Z') v = c - 'A' + 10;
            else if (c >= 'a' && c <= 'z') v = c - 'a' + 36;
            else throw new IllegalArgumentException("Invalid base62 char: " + c);
            result = result * BASE + v;
        }
        return result;
    }
}
