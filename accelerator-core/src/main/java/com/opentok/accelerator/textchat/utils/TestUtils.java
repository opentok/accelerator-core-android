package com.opentok.accelerator.textchat.utils;

import java.util.Random;

public class TestUtils {

    public static String generateString(int length){

        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch)
            tmp.append(ch);
        for (char ch = 'a'; ch <= 'z'; ++ch)
            tmp.append(ch);
        char[] symbols = tmp.toString().toCharArray();
        char[] buf = new char[length];
        Random random = new Random();

        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }
}
