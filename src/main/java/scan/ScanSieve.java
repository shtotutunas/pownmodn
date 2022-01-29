package scan;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import common.Common;
import common.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.Primes;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class ScanSieve {
    private static final Logger log = LoggerFactory.getLogger(ScanSieve.class);

    private final Primes primes;
    private final QuadraticResidueSieve evenQRSieve;
    private final QuadraticResidueSieve oddQRSieve;

    private final AtomicLong primeStepCount = new AtomicLong(0);

    public ScanSieve(BigInteger base, BigInteger target, Primes primes, Boolean qrSievePrecalculated) {
        this.primes = primes;
        if (qrSievePrecalculated != null) {
            long startTime = System.currentTimeMillis();
            this.evenQRSieve = QuadraticResidueSieve.create(target.intValueExact(), qrSievePrecalculated, true);
            log.info("QR sieve for even exponents is created in {}ms: {}", System.currentTimeMillis() - startTime,
                    (evenQRSieve != null) ? evenQRSieve.shortDescription() : null);

            startTime = System.currentTimeMillis();
            this.oddQRSieve = QuadraticResidueSieve.create(target.multiply(base).intValueExact(), qrSievePrecalculated, true);
            log.info("QR sieve for odd exponents is created in {}ms: {}", System.currentTimeMillis() - startTime,
                    (oddQRSieve != null) ? oddQRSieve.shortDescription() : null);
        } else {
            this.evenQRSieve = null;
            this.oddQRSieve = null;
        }
    }

    public BitSetGenerator createBitSetGenerator(BigInteger start, BigInteger step, long length, boolean oddPower) {
        BigInteger maxValue = start.add(step.multiply(BigInteger.valueOf(length-1)));
        int bound = (int) Math.sqrt(maxValue.doubleValue());

        IntStream.Builder startBuf = IntStream.builder();
        IntStream.Builder stepBuf = IntStream.builder();
        IntStream.Builder invBuf = IntStream.builder();
        int maxPrime = 0;
        if ((start.compareTo(Common.MAX_LONG) <= 0) && (step.compareTo(Common.MAX_LONG) <= 0)) {
            long startLong = start.longValueExact();
            long stepLong = step.longValueExact();
            for (int i = 0; (i < primes.size()) && (primes.get(i) <= bound) && (i <= length); i++) {
                int p = Math.toIntExact(primes.get(i));
                int stepMod = (int) (stepLong%p);
                startBuf.add((int) (startLong%p));
                stepBuf.add(stepMod);
                invBuf.add(getInv(stepMod, p));
                maxPrime = p;
            }
        } else {
            for (int i = 0; (i < primes.size()) && (primes.get(i) <= bound) && (i <= length); i++) {
                int p = Math.toIntExact(primes.get(i));
                BigInteger P = BigInteger.valueOf(p);
                int stepMod = step.mod(P).intValueExact();
                startBuf.add(start.mod(P).intValueExact());
                stepBuf.add(stepMod);
                invBuf.add(getInv(stepMod, p));
                maxPrime = p;
            }
        }

        IntIntMap toSkip = new IntIntHashMap();
        BigInteger x = start;
        int i = 0;
        while (x.compareTo(Common.MAX_INT) <= 0) {
            int intValue = x.intValueExact();
            if (x.intValueExact() <= maxPrime) {
                toSkip.put(intValue, i);
            } else {
                break;
            }
            x = x.add(step);
            i++;
        }

        return new BitSetGenerator(start, step, oddPower ? oddQRSieve : evenQRSieve,
                startBuf.build().toArray(), stepBuf.build().toArray(), invBuf.build().toArray(), toSkip);
    }

    private int getInv(int r, int p) {
        assert r >= 0;
        assert r < p;
        assert p >= 2;

        if ((r < 2) || (r == p-1)) {
            return r;
        } else {
            // tests show that by caching calculated results we can get rid of ~50% mod-pow calculations
            return (int) ModUtils.pow(r, p-2, p);
        }
    }

    public class BitSetGenerator {
        private final BigInteger start;
        private final BigInteger step;
        private final QuadraticResidueSieve qrSieve;
        private final int[] startMod;
        private final int[] stepMod;
        private final int[] inv;
        private final IntIntMap toSkip;

        public BitSetGenerator(BigInteger start, BigInteger step, QuadraticResidueSieve qrSieve,
                               int[] startMod, int[] stepMod, int[] inv, IntIntMap toSkip)
        {
            this.start = start;
            this.step = step;
            this.qrSieve = qrSieve;
            this.startMod = startMod;
            this.stepMod = stepMod;
            this.inv = inv;
            this.toSkip = toSkip;
        }


        public BitSet generate(long from, int length) {
            BitSet bitSet;
            if (qrSieve != null) {
                bitSet = qrSieve.generateBitSet((from == 0) ? start : start.add(step.multiply(BigInteger.valueOf(from))), step, length);
            } else {
                bitSet = new BitSet(length);
            }
            if (bitSet == null) {
                return null;
            }

            for (int i = 0; i < startMod.length; i++) {
                int p = (int) primes.get(i);
                if ((qrSieve != null) && qrSieve.isDivisor(p)) {
                    continue;
                }
                long startModP = Common.mod(startMod[i] + stepMod[i]*Common.mod(from, p), p);
                int pStart;
                int pStep;
                if (startModP == 0) {
                    pStart = 0;
                    if (stepMod[i] == 0) {
                        return null;
                    } else {
                        pStep = p;
                    }
                } else {
                    pStart = (int) Common.mod(-startModP*inv[i], p);
                    if (stepMod[i] == 0) {
                        pStep = -1;
                    } else {
                        pStep = p;
                    }
                }

                if (pStep > 0) {
                    int cnt = 0;
                    for (int j = pStart; j < length; j += pStep) {
                        cnt++;
                        bitSet.set(j);
                    }
                    primeStepCount.addAndGet(cnt);

                    int pos = toSkip.getOrDefault(p, -1);
                    if (pos - from >= 0) {
                        bitSet.clear((int) (pos - from));
                    }
                }
            }
            return bitSet;
        }
    }

    public void logPrimeSieveStats() {
        log.info("ScanSieve PrimeSieve stats --- total iterations: {}", primeStepCount.get());
    }

}
