package com.opentok.accelerator.utils

import java.util.Random

object TestUtils {
    fun generateString(length: Int): String {
        val tmp = StringBuilder()
        run {
            var ch = '0'
            while (ch <= '9') {
                tmp.append(ch)
                ++ch
            }
        }
        var ch = 'a'
        while (ch <= 'z') {
            tmp.append(ch)
            ++ch
        }
        val symbols = tmp.toString().toCharArray()
        val buf = CharArray(length)
        val random = Random()
        for (idx in buf.indices) buf[idx] = symbols[random.nextInt(symbols.size)]
        return String(buf)
    }
}