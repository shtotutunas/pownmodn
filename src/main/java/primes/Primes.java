package primes;

import com.carrotsearch.hppc.BitSet;

public class Primes {

    private final long[] primes;

    public Primes(long limit) {
        if (limit < 2) {
            primes = new long[0];
            return;
        }

        BitSet b = new BitSet(limit+1);
        b.set(2, limit+1);
        long sqrt = (long) Math.floor(Math.sqrt(limit));
        for (long p = 2; p <= sqrt; p++) {
            if (b.get(p)) {
                for (long i = p*p; i <= limit; i += p) {
                    b.clear(i);
                }
            }
        }

        int count = 0;
        for (long p = 2; p > 1; p = b.nextSetBit(p+1)) {
            count++;
        }

        primes = new long[count];
        int i = 0;
        for (long p = 2; p > 1; p = b.nextSetBit(p+1)) {
            primes[i] = p;
            i++;
        }
    }

    public long get(int i) {
        return primes[i];
    }

    public int size() {
        return primes.length;
    }
}
