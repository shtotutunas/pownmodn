package common;

import org.apache.commons.math3.util.ArithmeticUtils;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class Common {

    public static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    public static final BigInteger MAX_LONG_SQRT_BIG = MAX_LONG.sqrt();
    public static final long MAX_LONG_SQRT = MAX_LONG_SQRT_BIG.longValueExact();
    public static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    public static final Locale LOCALE = Locale.CANADA;

    public static final Comparator<BigInteger> COMPARE_BY_BIT_LENGTH = Comparator
            .comparing(BigInteger::bitLength)
            .thenComparing(x -> x);

    public static BigInteger e(int m, int e) {
        return BigInteger.TEN.pow(e).multiply(BigInteger.valueOf(m));
    }

    public static long mod(long n, long mod) {
        assert mod > 0;
        n %= mod;
        return (n >= 0) ? n : n+mod;
    }

    public static BigInteger mod(BigInteger n, BigInteger mod) {
        assert mod.signum() > 0;
        return (n.signum() >= 0) && (n.compareTo(mod) < 0) ? n : n.mod(mod);
    }

    public static BigInteger gcd(BigInteger a, BigInteger b) {
        assert a.signum() >= 0;
        assert b.signum() >= 0;
        if (a.compareTo(MAX_LONG) <= 0 && b.compareTo(MAX_LONG) <= 0) {
            return BigInteger.valueOf(ArithmeticUtils.gcd(a.longValueExact(), b.longValueExact()));
        } else {
            return a.gcd(b);
        }
    }

    public static byte[] repeatToLength(byte[] cycle, int length) {
        assert cycle.length > 0;
        byte[] result = Arrays.copyOf(cycle, length);
        repeatFirstTerms(result, cycle.length);
        return result;
    }

    public static boolean[] repeatToLength(boolean[] cycle, int length) {
        assert cycle.length > 0;
        boolean[] result = Arrays.copyOf(cycle, length);
        repeatFirstTerms(result, cycle.length);
        return result;
    }

    private static void repeatFirstTerms(Object array, int cycleLength) {
        int length = Array.getLength(array);
        while (cycleLength < length) {
            int add = Math.min(cycleLength, length-cycleLength);
            System.arraycopy(array, 0, array, cycleLength, add);
            cycleLength += add;
        }
    }

    public static BigInteger multiply(BigInteger... numbers) {
        HashMap<BigInteger, Integer> map = new HashMap<>();
        BiFunction<BigInteger, Integer, Integer> remap = (a, n) -> (n == null) ? 1 : n+1;
        for (BigInteger a : numbers) {
            map.compute(a, remap);
        }

        TreeSet<BigInteger> set = new TreeSet<>(COMPARE_BY_BIT_LENGTH);
        map.forEach((a, n) -> {
            if (n == 1) {
                set.add(a);
            } else {
                set.add(a.pow(n));
            }
        });

        while (set.size() > 1) {
            BigInteger a = set.first();
            set.remove(a);
            BigInteger b = set.first();
            set.remove(b);
            set.add(a.multiply(b));
        }
        return (set.size() > 0) ? set.first() : BigInteger.ONE;
    }

    public static Long max(Long... a) {
        return Stream.of(a).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
    }
}
