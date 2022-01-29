package common;

import java.math.BigInteger;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

public class ModUtils {

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

    public static long pow(long b, BigInteger n, long mod) {
        assert n.signum() >= 0;
        assert mod > 0;
        if (mod == 1) {
            return 0;
        }
        if (n.signum() == 0) {
            return 1;
        }

        LongBinaryOperator multiplier = modMultiplier(mod);
        if (multiplier == null) {
            return BigInteger.valueOf(b).modPow(n, BigInteger.valueOf(mod)).longValueExact();
        }

        int pos = 0;
        long t = Common.mod(b, mod);
        while (!n.testBit(pos)) {
            t = multiplier.applyAsLong(t, t);
            pos++;
        }

        long res = t;
        int bitLength = n.bitLength();
        pos++;
        while (pos < bitLength) {
            t = multiplier.applyAsLong(t, t);
            if (n.testBit(pos)) {
                res = multiplier.applyAsLong(res, t);
            }
            pos++;
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
        assert mod > 0;

        if (mod == 1) {
            return (a, b) -> 0;
        } else if (mod <= Common.MAX_LONG_SQRT) {
            return (a, b) -> {
                assert a >= 0;
                assert a < mod;
                assert b >= 0;
                assert b < mod;
                return (a*b)%mod;
            };
        } else if (mod < (1L<<42)) {
            long r64 = (((1L<<62)%mod)<<2)%mod;
            return modMultiplier(mod, r64, h -> h*r64);
        } else if (mod < (1L<<47)) {
            long r64 = (((1L<<62)%mod)<<2)%mod;
            long r79 = (r64<<15)%mod;
            long mask = (1L<<15)-1;
            return modMultiplier(mod, r64, h -> {
                long h0 = (h & mask);
                long h1 = (h >> 15);
                return (h0*r64) + (h1*r79);
            });
        } else if (mod < (1L<<50)) {
            long r64 = (((1L<<62)%mod)<<2)%mod;
            long r76 = (r64<<12)%mod;
            long r88 = (r76<<12)%mod;
            long mask = (1L<<12)-1;
            return modMultiplier(mod, r64, h -> {
                long h0 = (h & mask);
                long h1 = (h >> 12) & mask;
                long h2 = (h >> 24);
                return (h0*r64) + (h1*r76) + (h2*r88);
            });
        } else if (mod < (1L<<52)) {
            long r64 = (((1L<<62)%mod)<<2)%mod;
            long r74 = (r64<<10)%mod;
            long r84 = (r74<<10)%mod;
            long r94 = (r84<<10)%mod;
            long mask = (1L<<10)-1;
            return modMultiplier(mod, r64, h -> {
                long h0 = (h & mask);
                long h1 = (h >> 10) & mask;
                long h2 = (h >> 20) & mask;
                long h3 = (h >> 30);
                return (h0*r64) + (h1*r74) + (h2*r84) + (h3*r94);
            });
        } else {
            return null;
        }
    }

    private static LongBinaryOperator modMultiplier(long mod, long r64, LongUnaryOperator H) {
        return (a, b) -> {
            assert a >= 0;
            assert a < mod;
            assert b >= 0;
            assert b < mod;
            long low = a*b;
            long high = H.applyAsLong(Math.multiplyHigh(a, b));

            int c = 0;
            if (low < 0) {
                c++;
            }
            if (high < 0) {
                c++;
            }
            c += Common.addHigh(low, high);
            long res = low + high;
            while (c != 0) {
                long add = c * r64;
                int t = Common.addHigh(res, add);
                res += add;
                c = t;
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
            A_inv = BigInteger.valueOf(ModUtils.pow(adr[1].longValueExact(), p-2, p));
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

        BigInteger x = b1.subtract(b0).multiply(ModUtils.modInverse(a0, a1)).mod(a1);
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
