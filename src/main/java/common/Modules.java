package common;

import java.math.BigInteger;
import java.util.function.LongBinaryOperator;

public class Modules {

    public static long pow(long b, long n, long mod) {
        assert n >= 0;
        assert mod > 0;
        if (mod == 1) {
            return 0;
        }
        if (n == 0) {
            return 1;
        }

        LongBinaryOperator multiplier = modMultiplier(mod);
        if (multiplier == null) {
            return BigInteger.valueOf(b).modPow(BigInteger.valueOf(n), BigInteger.valueOf(mod)).longValueExact();
        }

        long t = Common.mod(b, mod);
        while ((n&1) == 0) {
            t = multiplier.applyAsLong(t, t);
            n >>= 1;
        }

        long res = t;
        n >>= 1;
        while (n > 0) {
            t = multiplier.applyAsLong(t, t);
            if ((n&1) == 1) {
                res = multiplier.applyAsLong(res, t);
            }
            n >>= 1;
        }
        return res;
    }

    public static BigInteger modInverse(BigInteger a, BigInteger n) {
        assert a.signum() > 0;
        assert n.signum() > 0;

        if (n.compareTo(Common.MAX_LONG) <= 0) {
            return BigInteger.valueOf(modInverse(Common.mod(a, n).longValueExact(), n.longValueExact()));
        } else {
            return a.modInverse(n);
        }
    }

    public static long modInverse(long a, long n) {
        long[] xy = new long[2];
        long gcd = gcdExt(a, n, xy);
        if (gcd != 1) {
            throw new IllegalArgumentException("Cannot invert " + a + " mod " + n);
        }
        return Common.mod(xy[0], n);
    }

    private static long gcdExt(long a, long b, long[] xy) {
        if (a == 0) {
            xy[0] = 0;
            xy[1] = 1;
            return b;
        }
        long d = gcdExt(b%a, a, xy);
        long x = xy[1] - (b/a)*xy[0];
        long y = xy[0];
        xy[0] = x;
        xy[1] = y;
        return d;
    }

    public static LongBinaryOperator modMultiplier(long mod) {
        if (mod == 1) {
            return (a, b) -> 0;
        }
        if (mod <= Common.MAX_LONG_SQRT) {
            return modMultiplier32bit(mod);
        } else {
            return modMultiplier42bit(mod);
        }
    }

    private static LongBinaryOperator modMultiplier32bit(long mod) {
        assert mod > 0;
        assert mod <= Common.MAX_LONG_SQRT;

        return (a, b) -> {
            assert a >= 0;
            assert a < mod;
            assert b >= 0;
            assert b < mod;
            return (a*b)%mod;
        };
    }

    private static LongBinaryOperator modMultiplier42bit(long mod) {
        assert mod > 0;
        if (mod >= (1L<<62)) {
            return null;
        }

        long r64 = ((Long.MAX_VALUE % mod + 1) << 1) % mod;
        if (mod > (1L<<42)) {
            long maxR = mod-1;
            long maxH = Math.multiplyHigh(maxR, maxR);
            if ((maxH > 0) && ((Long.MAX_VALUE - maxR) / maxH < r64)) {
                return null;
            }
        }

        return (a, b) -> {
            assert a >= 0;
            assert a < mod;
            assert b >= 0;
            assert b < mod;
            long high = Math.multiplyHigh(a, b);
            long low = a*b;
            long res = (high*r64) + (low%mod);
            if (low < 0) {
                res += r64;
            }
            return Common.mod(res, mod);
        };
    }

    // returns such {a, b} that Ax+B = p(ay+b)
    // in other words, if X%A == B and X%p == 0 then X%ap == bp
    // P should be prime
    public static BigInteger[] divideLinearSum(BigInteger A, BigInteger B, BigInteger P) {
        assert A.signum() > 0;
        assert B.signum() >= 0;
        assert P.signum() > 0;
        assert P.isProbablePrime(10);

        BigInteger[] adr = A.divideAndRemainder(P);
        BigInteger[] bdr = B.divideAndRemainder(P);
        if (adr[1].signum() == 0) {
            if (bdr[1].signum() == 0) {
                return new BigInteger[] {adr[0], bdr[0]};
            } else {
                return null;
            }
        }

        BigInteger A_inv;
        if (P.compareTo(Common.MAX_LONG) <= 0) {
            long p = P.longValueExact();
            A_inv = BigInteger.valueOf(Modules.pow(adr[1].longValueExact(), p-2, p));
        } else {
            A_inv = adr[1].modPow(P.subtract(BigInteger.TWO), P);
        }

        BigInteger pb = (bdr[1].signum() == 0) ? BigInteger.ZERO : P.subtract(bdr[1]);
        BigInteger C = pb.multiply(A_inv).mod(P);
        return new BigInteger[] {A, A.multiply(C).add(B).divide(P)};
    }

    // generalized Garner's algorithm for two pairs of numbers
    public static BigInteger[] merge(BigInteger A0, BigInteger B0, BigInteger A1, BigInteger B1) {
        assert A0.signum() > 0;
        assert B0.signum() >= 0;

        assert A1.signum() > 0;
        assert B1.signum() >= 0;

        if (A0.equals(BigInteger.ONE)) {
            return new BigInteger[] {A1, B1};
        }
        if (A1.equals(BigInteger.ONE)) {
            return new BigInteger[] {A0, B0};
        }

        BigInteger gcd = Common.gcd(A0, A1);
        if (!B0.mod(gcd).equals(B1.mod(gcd))) {
            return null;
        }
        BigInteger[] r = rebalanceDivisors(A0, A1, gcd);

        BigInteger a0 = r[0];
        BigInteger b0 = a0.equals(A0) ? B0 : B0.remainder(a0);
        BigInteger a1 = r[1];
        BigInteger b1 = a1.equals(A1) ? B1 : B1.remainder(a1);

        BigInteger x = b1.subtract(b0).multiply(Modules.modInverse(a0, a1)).mod(a1);
        BigInteger A = a0.multiply(a1);
        BigInteger B = x.multiply(a0).add(b0);
        return new BigInteger[] {A, B};
    }

    // returns such {a, b} that:
    // 1) a*b = lcm(A, B)
    // 2) gcd(a, b) = 1
    // 3) A%a = 0
    // 4) B%b = 0
    public static BigInteger[] rebalanceDivisors(BigInteger A, BigInteger B, BigInteger gcd) {
        assert A.signum() > 0;
        assert B.signum() > 0;
        if (gcd.equals(BigInteger.ONE)) {
            return new BigInteger[] {A, B};
        }

        BigInteger gcdA = Common.gcd(A, gcd.multiply(gcd));
        BigInteger prev = BigInteger.ONE;
        BigInteger next = gcdA.divide(gcd);
        while (!prev.equals(next)) {
            prev = next;
            next = Common.gcd(A, prev.multiply(prev));
        }

        BigInteger Bs = Common.gcd(gcd, next);
        BigInteger As = gcd.divide(Bs);
        return new BigInteger[] {A.divide(As), B.divide(Bs)};
    }

    public static long restoreByCRT(long[] p, long[] r) {
        long result = 0;
        long mult = 1;
        long[] x = new long[p.length];
        for (int i = 0; i < p.length; i++) {
            x[i] = r[i];
            for (int j = 0; j < i; j++) {
                x[i] = Common.mod(modInverse(p[j], p[i]) * (x[i] - x[j]), p[i]);
            }
            result += x[i]*mult;
            mult *= p[i];
        }
        return result;
    }
}
