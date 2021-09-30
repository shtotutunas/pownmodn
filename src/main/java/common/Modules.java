package common;

import java.math.BigInteger;
import java.util.function.LongBinaryOperator;

public class Modules {

    public static int pow(int b, int n, int mod) {
        assert n >= 0;
        assert mod > 0;

        long res = 1;
        long t = b;
        while (n > 0) {
            if ((n&1) == 1) {
                res = (res*t)%mod;
            }
            t = (t*t)%mod;
            n >>= 1;
        }
        return (int) (res%mod);
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
    public static BigInteger[] divideLinearSum(BigInteger A, BigInteger B, BigInteger P) {
        assert A.signum() > 0;
        assert B.signum() >= 0;
        assert P.signum() > 0;

        BigInteger gcd = A.gcd(P);
        if (gcd.compareTo(BigInteger.ONE) > 0) {
            if (B.divideAndRemainder(gcd)[1].signum() != 0) {
                return null;
            }
            A = A.divide(gcd);
            B = B.divide(gcd);
            P = P.divide(gcd);
        }

        BigInteger[] adr = A.divideAndRemainder(P);
        BigInteger[] bdr = B.divideAndRemainder(P);
        if (adr[1].signum() == 0) {
            if (bdr[1].signum() == 0) {
                return new BigInteger[] {adr[0], bdr[0]};
            } else {
                return null;
            }
        }

        BigInteger pb = (bdr[1].signum() == 0) ? BigInteger.ZERO : P.subtract(bdr[1]);
        BigInteger A_inv = adr[1].modInverse(P);
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

        BigInteger gcd = A0.gcd(A1);
        if (!B0.mod(gcd).equals(B1.mod(gcd))) {
            return null;
        }
        BigInteger[] r = rebalanceDivisors(A0, A1);

        BigInteger a0 = r[0];
        BigInteger b0 = a0.equals(A0) ? B0 : B0.remainder(A0);
        BigInteger a1 = r[1];
        BigInteger b1 = a1.equals(A1) ? B1 : B1.remainder(A1);

        BigInteger x = b1.subtract(b0).multiply(a0.modInverse(a1)).mod(a1);
        BigInteger A = a0.multiply(a1);
        BigInteger B = x.multiply(a0).add(b0);
        return new BigInteger[] {A, B};
    }

    // returns such {a, b} that:
    // 1) a*b = lcm(A, B)
    // 2) gcd(a, b) = 1
    // 3) A%a = 0
    // 4) B%b = 0
    public static BigInteger[] rebalanceDivisors(BigInteger A, BigInteger B) {
        assert A.signum() > 0;
        assert B.signum() > 0;
        BigInteger gcd = A.gcd(B);
        if (gcd.equals(BigInteger.ONE)) {
            return new BigInteger[] {A, B};
        }

        BigInteger gcdA = A.gcd(gcd.multiply(gcd));
        BigInteger prev = BigInteger.ONE;
        BigInteger next = gcdA.divide(gcd);
        while (!prev.equals(next)) {
            prev = next;
            next = A.gcd(prev.multiply(prev));
        }

        BigInteger Bs = gcd.gcd(next);
        BigInteger As = gcd.divide(Bs);
        return new BigInteger[] {A.divide(As), B.divide(Bs)};
    }
}
