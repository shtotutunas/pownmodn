package primes;

import com.carrotsearch.hppc.BitSet;

import java.util.Arrays;
import java.util.stream.LongStream;

public class Primes {

    private final long[] primes;
    private final BitSet isPrime;

    public Primes(long limit) {
        if (limit < 2) {
            this.primes = new long[0];
            this.isPrime = new BitSet();
            return;
        }

        this.isPrime = new BitSet(limit+1);
        isPrime.set(2, limit+1);
        long sqrt = (long) Math.floor(Math.sqrt(limit));
        for (long p = 2; p <= sqrt; p++) {
            if (isPrime.get(p)) {
                for (long i = p*p; i <= limit; i += p) {
                    isPrime.clear(i);
                }
            }
        }

        int count = 0;
        for (long p = 2; p > 1; p = isPrime.nextSetBit(p+1)) {
            count++;
        }

        this.primes = new long[count];
        int i = 0;
        for (long p = 2; p > 1; p = isPrime.nextSetBit(p+1)) {
            this.primes[i] = p;
            i++;
        }
    }

    public long get(int i) {
        return primes[i];
    }

    public int size() {
        return primes.length;
    }

    public int floorIdx(long n) {
        int idx = Arrays.binarySearch(primes, n);
        return (idx >= 0) ? idx : -(idx+1) - 1;
    }

    public long[] factorize(long n) {
        assert n > 0;
        long lastPrime = (primes.length > 0) ? primes[primes.length-1] : 1;
        LongStream.Builder buf = LongStream.builder();
        int i = 0;
        while (n > 1) {
            long p = (i < primes.length) ? primes[i] : lastPrime + (i-primes.length) + 1;
            i++;
            if (n%p != 0) {
                continue;
            }

            do {
                n /= p;
                buf.add(p);
            } while (n%p == 0);

            if ((n <= lastPrime) && isPrime.get(n)) {
                buf.add(n);
                n = 1;
            }
        }
        return buf.build().toArray();
    }
}
