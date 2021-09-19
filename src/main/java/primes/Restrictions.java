package primes;

import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import common.Common;
import common.LongMod;
import common.Modules;

import java.math.BigInteger;

public class Restrictions {

    public static long[] calculateRestriction(long base, long p, long target) {
        return calculateRestriction(base, LongMod.create(p), target);
    }

    private static long[] calculateRestriction(long base, LongMod modP, long target) {
        assert base > 1;
        base = Common.mod(base, modP.getMod());
        target = Common.mod(target, modP.getMod());
        long x = base;
        if (x == 0) {
            if (target == 0) {
                return new long[] {1, 0};
            } else {
                return null;
            }
        }

        int n = StrictMath.toIntExact((long) Math.sqrt(modP.getMod()));
        LongIntMap map = new LongIntHashMap(n);
        boolean cycled = false;
        long xn = 1;
        modP.setStepMultiplier(x);
        for (int i = 1; i <= n; i++) {
            xn = modP.step();
            map.put(xn, i);
            if (xn == 1) {
                cycled = true;
                break;
            }
        }

        if (cycled) {
            int pos = map.get(target);
            if (pos == 0) {
                return null;
            } else {
                return new long[]{map.size(), pos%map.size()};
            }
        }

        long pos = map.get(target);
        modP.setStepMultiplier(xn);
        modP.setAuxMultiplier(target);
        long nk = n;
        long len;
        while (true) {
            if ((pos == 0) && (target != 1)) {
                long ct = modP.getAux();
                int pt = map.get(ct);
                if (pt > 0) {
                    pos = pt - nk;
                }
            }
            long xnk = modP.step();
            nk += n;
            int t = map.get(xnk);
            if (t > 0) {
                len = nk - t;
                break;
            }
        }

        if ((pos == 0) && (target != 1)) {
            return null;
        }
        if (pos < 0) {
            pos += len;
        }
        return new long[]{len, pos%len};
    }

    public static BigInteger[] merge(long base, long targetRemainder, long C, long A, long B, long p, long a, long b) {
        return merge(BigInteger.valueOf(base), BigInteger.valueOf(targetRemainder),
                BigInteger.valueOf(C), BigInteger.valueOf(A), BigInteger.valueOf(B),
                BigInteger.valueOf(p), BigInteger.valueOf(a), BigInteger.valueOf(b));
    }

    public static BigInteger[] merge(BigInteger base, BigInteger targetRemainder,
                                     BigInteger C, BigInteger A, BigInteger B,
                                     BigInteger p, BigInteger a, BigInteger b)
    {
        assert base.compareTo(BigInteger.ONE) > 0;
        BigInteger[] withP = Modules.divideLinearSum(A, B, p);
        if (withP == null) {
            return null;
        }
        BigInteger[] newAB = Modules.merge(withP[0].multiply(C), withP[1].multiply(C), a, b);
        if (newAB == null) {
            return null;
        }
        A = newAB[0].divide(C);
        B = newAB[1].divide(C);
        BigInteger N = C.multiply(p);
        targetRemainder = Common.mod(targetRemainder, N);

        if (Modules.pow(base, B.add(A).multiply(N), N).equals(targetRemainder)) {
            return new BigInteger[] {A, B};
        } else {
            return null;
        }
    }
}
