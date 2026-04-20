package com.yourcompany.assetmanager.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.add(b).setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.subtract(b).setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.multiply(b).setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null || b.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return a.divide(b, SCALE, ROUNDING_MODE);
    }
}
