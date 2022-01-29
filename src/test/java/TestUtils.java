import common.ModUtils;
import org.apache.commons.math3.util.Pair;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

public class TestUtils {
    public static Pair<Map<Long, Long>, Map<Long, Long>> generateAB(int base, int n, long absT) {
        Map<Long, Long> A = new HashMap<>();
        Map<Long, Long> B = new HashMap<>();
        long x = 1;
        long bn = ModUtils.pow(base, n, n);
        for (long e = 0; e <= 2*n; e++) {
            for (long y = x; y <= absT; y+=n) {
                Long b = B.get(y);
                if (b == null) {
                    B.put(y, e);
                } else if (!A.containsKey(y)) {
                    A.put(y, e-b);
                }
            }

            for (long y = x-n; y >= -absT; y-=n) {
                Long b = B.get(y);
                if (b == null) {
                    B.put(y, e);
                } else if (!A.containsKey(y)) {
                    A.put(y, e-b);
                }
            }

            x = (x*bn)%n;
        }
        return Pair.create(A, B);
    }

    public static BigInteger[] generateTestNumbers(int lowBitLength, int highBitLength, int testsPerBitLength,
                                                   boolean addAllSmaller, boolean withZero, Random random)
    {
        Stream.Builder<BigInteger> buf = Stream.builder();
        if (addAllSmaller) {
            for (int i = withZero ? 0 : 1; i < (1<<lowBitLength); i++) {
                buf.add(BigInteger.valueOf(i));
            }
        }
        for (int numBits = lowBitLength; numBits <= highBitLength; numBits++) {
            for (int i = 0; i < testsPerBitLength; i++) {
                BigInteger r = new BigInteger(numBits, random);
                while (r.bitLength() != numBits) {
                    r = new BigInteger(numBits, random);
                }
                buf.add(r);
            }
        }
        return buf.build().toArray(BigInteger[]::new);
    }
}
