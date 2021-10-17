package primes;

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongLongHashMap;
import com.carrotsearch.hppc.LongLongMap;
import com.carrotsearch.hppc.LongSet;
import common.Common;
import common.Modules;
import factorization.Factorization;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

public class GoodPrimes {
    private static final Logger log = LoggerFactory.getLogger(GoodPrimes.class);

    private final long[] goodPrimes;
    private final long[] a;
    private final long[] b;
    private final long[] gcdAB;

    private GoodPrimes(long[] goodPrimes, long[] a, long[] b, long[] gcdAB) {
        this.goodPrimes = goodPrimes;
        this.a = a;
        this.b = b;
        this.gcdAB = gcdAB;
    }

    public long get(int i) {
        return goodPrimes[i];
    }

    public long getA(int i) {
        return a[i];
    }

    public long getB(int i) {
        return b[i];
    }

    public long getGcdAB(int i) {
        return gcdAB[i];
    }

    public int size() {
        return goodPrimes.length;
    }

    public static GoodPrimes generate(long limit, long base, long target, Primes primes, ExecutorService executor, int threadsNumber) {
        long startTime = System.currentTimeMillis();
        log.info("Start generating good primes up to {}...", limit);
        long[][][] result;
        if (executor == null) {
            result = new long[][][] { generate(limit, base, target, 0, 1, primes) };
        } else {
            Future<long[][]>[] tasks = new Future[threadsNumber];
            for (int i = 0; i < threadsNumber; i++) {
                int shift = i;
                tasks[i] = executor.submit(() -> generate(limit, base, target, shift, threadsNumber, primes));
            }
            result = new long[threadsNumber][][];
            try {
                for (int i = 0; i < threadsNumber; i++) {
                    result[i] = tasks[i].get();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        log.info("Finalizing good primes...");
        long[][] merged = mergeResults(result, BigInteger.valueOf(base), BigInteger.valueOf(target), primes);
        log.info("{} good primes found in {}ms", merged[0].length, System.currentTimeMillis() - startTime);
        return new GoodPrimes(merged[0], merged[1], merged[2], merged[3]);
    }

    private static long[][] generate(long limit, long base, long target, int shift, int step, Primes primes) {
        LongStream.Builder gp = LongStream.builder();
        LongStream.Builder a = LongStream.builder();
        LongStream.Builder b = LongStream.builder();
        LongStream.Builder gcdAB = LongStream.builder();
        for (int i = shift; i < primes.size(); i += step) {
            long p = primes.get(i);
            if (p > limit) {
                break;
            }
            long[] r = calculateRestriction(Common.mod(base, p), p, Common.mod(target, p), primes);
            if (r != null) {
                gp.add(p);
                a.add(r[0]);
                b.add(r[1]);
                gcdAB.add(ArithmeticUtils.gcd(r[0], r[1]));
            }
        }
        return new long[][] {gp.build().toArray(), a.build().toArray(), b.build().toArray(), gcdAB.build().toArray()};
    }

    private static long[] calculateRestriction(long a, long p, long b, Primes primes) {
        assert (a >= 0) && (a < p);
        assert (b >= 0) && (b < p);
        if (p < 10) {
            return Restrictions.calculateRestriction(a, p, b);
        }

        long[] pf = primes.factorize(p-1);
        long[] divisors = Factorization.generateDivisors(pf);
        long orderA = findOrder(a, p, divisors);
        long orderB = findOrder(b, p, divisors);
        if (orderA%orderB != 0) {
            return null;
        }

        LongStream.Builder w = LongStream.builder();
        LongStream.Builder r = LongStream.builder();
        int i = 0;
        while (i < pf.length) {
            long q = pf[i];
            while ((i+1 < pf.length) && (pf[i+1] == pf[i])) {
                q *= pf[i];
                i++;
            }
            i++;

            long dq = (p-1)/q;
            long A = Modules.pow(a, dq, p);
            long B = Modules.pow(b, dq, p);
            long[] x = Restrictions.calculateRestriction(A, p, B, q);
            if (x == null) {
                return null;
            }
            w.add(q);
            r.add(x[1]);
        }

        return new long[] {orderA, Modules.restoreByCRT(w.build().toArray(), r.build().toArray())%orderA};
    }

    private static long findOrder(long a, long p, long[] divisors) {
        assert divisors[0] == 1;
        assert divisors[divisors.length-1] == p-1;
        if (a < 2) {
            return 1;
        }

        long[] pows = new long[divisors.length];
        for (int i = 1; i+1 < divisors.length; i++) {
            long d = divisors[i];
            long b = a;
            for (int j = i-1; j > 1; j--) {
                if (divisors[i]%divisors[j] == 0) {
                    d = divisors[i]/divisors[j];
                    b = pows[j];
                    break;
                }
            }
            pows[i] = Modules.pow(b, d, p);
            if (pows[i] == 1) {
                return divisors[i];
            }
        }
        return p-1;
    }

    private static long[][] mergeResults(long[][][] results, BigInteger base, BigInteger target, Primes primes) {
        int n = results.length;
        LongStream.Builder gp = LongStream.builder();
        LongStream.Builder a = LongStream.builder();
        LongStream.Builder b = LongStream.builder();
        LongStream.Builder gcdAB = LongStream.builder();

        long maxGcd = 0;
        for (long[][] result : results) {
            for (int j = 0; j < result[3].length; j++) {
                maxGcd = Math.max(maxGcd, result[3][j]);
            }
        }

        int[] pos = new int[n];
        LongLongMap as = new LongLongHashMap();
        LongLongMap bs = new LongLongHashMap();
        LongSet goodGcd = new LongHashSet();
        LongSet badGcd = new LongHashSet();
        for (int i = 0; i < primes.size(); i += n) {
            for (int j = 0; (j < n) && (i+j < primes.size()); j++) {
                long p = primes.get(i+j);
                if ((pos[j] < results[j][0].length) && (p == results[j][0][pos[j]])) {
                    long g = results[j][3][pos[j]];
                    boolean good;
                    if (g == 1) {
                        good = true;
                    } else {
                        boolean inGood = goodGcd.contains(g);
                        if (inGood) {
                            good = true;
                        } else {
                            boolean inBad = badGcd.contains(g);
                            if (inBad) {
                                good = false;
                            } else {
                                if (isGcdGood(base, target, g, primes, as, bs)) {
                                    goodGcd.add(g);
                                    good = true;
                                } else {
                                    badGcd.add(g);
                                    good = false;
                                }
                            }
                        }
                    }

                    if (good) {
                        gp.add(p);
                        a.add(results[j][1][pos[j]]);
                        b.add(results[j][2][pos[j]]);
                        gcdAB.add(g);

                        if (p <= maxGcd) {
                            as.put(p, results[j][1][pos[j]]);
                            bs.put(p, results[j][2][pos[j]]);
                        }
                    }
                    pos[j]++;
                }
            }
        }
        return new long[][] {gp.build().toArray(), a.build().toArray(), b.build().toArray(), gcdAB.build().toArray()};
    }

    private static boolean isGcdGood(BigInteger base, BigInteger target, long gcd, Primes primes, LongLongMap as, LongLongMap bs) {
        BigInteger C = BigInteger.ONE;
        BigInteger A = BigInteger.ONE;
        BigInteger B = BigInteger.ZERO;

        for (int i = 0; i < primes.size(); i++) {
            long p = primes.get(i);
            if (gcd/p < p) {
                break;
            }
            if (gcd%p == 0) {
                BigInteger a = BigInteger.valueOf(as.get(p));
                if (a.signum() == 0) {
                    return false;
                }
                BigInteger b = BigInteger.valueOf(bs.get(p));
                BigInteger P = BigInteger.valueOf(p);
                do {
                    gcd /= p;
                    BigInteger[] newAB = Restrictions.merge(base, target, C, A, B, P, a, b);
                    if (newAB == null) {
                        return false;
                    }
                    C = C.multiply(P);
                    A = newAB[0];
                    B = newAB[1];
                } while (gcd%p == 0);

            }
        }

        if (gcd > 1) {
            BigInteger a = BigInteger.valueOf(as.get(gcd));
            if (a.signum() == 0) {
                return false;
            }
            BigInteger b = BigInteger.valueOf(bs.get(gcd));
            BigInteger[] AB = Restrictions.merge(base, target, C, A, B, BigInteger.valueOf(gcd), a, b);
            return AB != null;

        } else {
            return true;
        }
    }

}
