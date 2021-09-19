package factorization;

import java.math.BigInteger;

public class GcdDetector {

    private final BigInteger N;
    private final BigInteger[] buf;
    private int pos;

    public GcdDetector(BigInteger N, int gcdCallDelay) {
        assert N.signum() > 0;
        assert gcdCallDelay > 0;
        this.N = N;
        this.buf = new BigInteger[gcdCallDelay];
        this.pos = 0;
    }

    public BigInteger add(BigInteger x) {
        buf[pos] = (pos == 0) ? x : buf[pos-1].multiply(x).mod(N);
        pos++;
        if (pos >= buf.length) {
            BigInteger gcd = N.gcd(buf[buf.length-1]);
            if (!gcd.equals(BigInteger.ONE)) {
                if (!gcd.equals(N)) {
                    return gcd;
                }
                for (int i = 0; i <= pos; i++) {
                    gcd = N.gcd(buf[i]);
                    if (!gcd.equals(BigInteger.ONE)) {
                        return gcd;
                    }
                }
            }
            pos = 0;
        }
        return null;
    }

    public BigInteger finish() {
        if (pos > 0) {
            BigInteger gcd = N.gcd(buf[pos-1]);
            if (gcd.equals(BigInteger.ONE)) {
                return null;
            } else if (!gcd.equals(N)) {
                return gcd;
            }
        }
        return add(BigInteger.ZERO);
    }
}
