package scan;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.LongObjectMap;
import common.Common;
import org.apache.commons.math3.primes.Primes;
import org.apache.commons.math3.util.ArithmeticUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class QuadraticResidueSieve {

    private final boolean[] residue;
    private final long[][][] precalculatedBuffers;
    private final BigInteger M;

    public static QuadraticResidueSieve create(int n, boolean withPrecalculatedBuffers, boolean nullIfUseless) {
        for (long i = 2; i*i <= Math.abs(n); i++) {
            while (n%(i*i) == 0) {
                n /= (i*i);
            }
        }

        int m = Math.abs(StrictMath.multiplyExact(4, n));
        boolean[] residue = new boolean[m];
        boolean useless = true;
        for (int i = 1; i < m; i++) {
            if (ArithmeticUtils.gcd(i, m) == 1) {
                int p = findPrime(i, m);
                if (isResidue(n, p)) {
                    residue[i] = true;
                } else {
                    useless = false;
                }
            }
        }
        if (useless && nullIfUseless) {
            return null;
        }
        return new QuadraticResidueSieve(residue, withPrecalculatedBuffers ? generatePrecalculatedBuffers(residue) : null);
    }

    public BitSet generateBitSet(BigInteger start, BigInteger step, int length) {
        int a = (start.compareTo(Common.MAX_LONG) <= 0) ? (int) (start.longValueExact()%residue.length) : start.mod(M).intValueExact();
        int b = (step.compareTo(Common.MAX_LONG) <= 0) ? (int) (step.longValueExact()%residue.length) : step.mod(M).intValueExact();
        if (precalculatedBuffers != null) {
            long[] cycle = precalculatedBuffers[a][b];
            if (cycle == null) {
                return null;
            }
            int needLongs = ((length-1)>>6) + 1;
            return BitSet.valueOf(Common.repeatToLength(cycle, needLongs));
        } else {
            return generateBitSet(a, b, length, residue);
        }
    }

    private static BitSet generateBitSet(long a, int b, int length, boolean[] residue) {
        int m = residue.length;
        int gcd = ArithmeticUtils.gcd(b, m);

        int cycle = Math.min(m/gcd, length);
        boolean[] r = new boolean[cycle];
        for (int i = 0; i < cycle; i++) {
            r[i] = !residue[(int) (a%m)];
            a += b;
        }

        BitSet bitSet = new BitSet(length);
        int ps = 0;
        int pb = 0;
        while (pb < length) {
            if (r[ps]) {
                bitSet.set(pb);
            }
            pb++;
            ps++;
            if (ps >= cycle) {
                ps -= cycle;
            }
        }

        return bitSet;
    }

    public boolean isDivisor(long p) {
        return residue.length % p == 0;
    }

    public boolean isResidueFor(int p) {
        assert Primes.isPrime(p);
        return residue[p%residue.length];
    }

    public String shortDescription() {
        int residues = 0;
        for (int i = 0; i < residue.length; i++) {
            if (residue[i]) {
                residues++;
            }
        }
        return residues + "/" + residue.length + " = " + String.format(Common.LOCALE, "%.3f", residues * 1.0 / residue.length);
    }

    private QuadraticResidueSieve(boolean[] residue, long[][][] precalculatedBuffers) {
        this.residue = residue;
        this.precalculatedBuffers = precalculatedBuffers;
        this.M = BigInteger.valueOf(residue.length);
    }

    private static int findPrime(int r, int m) {
        while (true) {
            if (Primes.isPrime(r)) {
                return r;
            }
            r = StrictMath.addExact(r, m);
        }
    }

    private static boolean isResidue(int n, int p) {
        for (long i = 0 ; i < p; i++) {
            if ((i*i)%p == Common.mod(n, p)) {
                return true;
            }
        }
        return false;
    }

    private static long[][][] generatePrecalculatedBuffers(boolean[] residue) {
        int m = residue.length;
        LongObjectMap<List<long[]>> cache = new LongObjectHashMap<>();
        int longLength = m/ArithmeticUtils.gcd(m, 64);
        int bitLength = StrictMath.multiplyExact(longLength, 64);
        long[][][] buf = new long[m][m][];
        for (int a = 0; a < m; a++) {
            for (int b = 0; b < m; b++) {
                BitSet bitSet = generateBitSet(a, b, bitLength, residue);
                boolean empty = true;
                for (int i = 0; empty && (i < bitLength); i++) {
                    empty = bitSet.get(i);
                }
                if (!empty) {
                    long[] array = bitSet.toLongArray();
                    if (array.length != longLength) {
                        array = Arrays.copyOf(array, longLength);
                    }
                    buf[a][b] = intern(array, cache);
                }
            }
        }
        return buf;
    }

    private static long[] intern(long[] a, LongObjectMap<List<long[]>> cache) {
        List<long[]> list = cache.get(a[0]);
        if (list == null) {
            list = new ArrayList<>();
            cache.put(a[0], list);
        }
        for (long[] b : list) {
            if (Arrays.equals(a, b)) {
                return b;
            }
        }
        list.add(a);
        return a;
    }

}
