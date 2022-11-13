import common.ModUtils;
import org.apache.commons.math3.util.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
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

    public static long[] generateAP(long min, long max, long step) {
        LongStream.Builder a = LongStream.builder();
        for (long i = min; i <= max; i += step) {
            a.add(i);
        }
        return a.build().toArray();
    }

    public static String[] formatTable(String[][] cells, int spaceBetweenColumns) {
        int rowNum = cells.length;
        int colNum = Arrays.stream(cells).mapToInt(s -> s.length).max().orElse(0);
        int[] colWidth = IntStream.range(0, colNum).map(c -> IntStream.range(0, rowNum).filter(r -> (cells[r].length > c))
                .map(r -> (cells[r][c] != null) ? cells[r][c].length() : 0).max().orElse(0)).toArray();

        char[][] result = new char[rowNum][IntStream.of(colWidth).sum() + (colNum-1)*spaceBetweenColumns];
        for (int r = 0; r < rowNum; r++) {
            Arrays.fill(result[r], ' ');
            int shift = 0;
            for (int c = 0; c < cells[r].length; c++) {
                if (cells[r][c] != null) {
                    System.arraycopy(cells[r][c].toCharArray(), 0, result[r], shift + (colWidth[c]-cells[r][c].length()), cells[r][c].length());
                }
                shift += (colWidth[c] + spaceBetweenColumns);
            }
        }
        return Arrays.stream(result).map(String::new).toArray(String[]::new);
    }

    public static class Primes {
        private final Map<Integer, List<BigInteger>> map = new HashMap<>();

        public BigInteger get(int powerOfTwo, int idx) {
            List<BigInteger> list = map.computeIfAbsent(powerOfTwo, x -> new ArrayList<>());
            if (list.isEmpty()) {
                list.add(BigInteger.TWO.pow(powerOfTwo).nextProbablePrime());
            }
            while (idx >= list.size()) {
                list.add(list.get(list.size()-1).nextProbablePrime());
            }
            return list.get(idx);
        }
    }
}
