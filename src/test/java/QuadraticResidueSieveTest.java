import org.apache.commons.math3.primes.Primes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scan.QuadraticResidueSieve;

public class QuadraticResidueSieveTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testResidueFor() {
        int residueLimit = 200;
        int primeLimit = 10000;

        long startTime = System.currentTimeMillis();
        QuadraticResidueSieve[] qr = new QuadraticResidueSieve[residueLimit];
        for (int i = 1; i < residueLimit; i++) {
            qr[i] = QuadraticResidueSieve.create(i, false, false);
        }
        for (int p = 3; p <= primeLimit; p++) {
            if (!Primes.isPrime(p)) {
                continue;
            }
            boolean[] checked = new boolean[p];
            for (long i = 1; i < p; i++) {
                int r = (int) (i*i)%p;
                checked[r] = true;
                while (r < residueLimit) {
                    Assertions.assertTrue(qr[r].isResidueFor(p), "p=" + p + ";  r=" + r);
                    r += p;
                }
            }
            for (int i = 1; i < p; i++) {
                if (!checked[i]) {
                    int r = i;
                    while (r < residueLimit) {
                        Assertions.assertFalse(qr[r].isResidueFor(p), "p=" + p + ";  r=" + r);
                        r += p;
                    }
                }
            }
        }
        log.info("OK - tested for residues in [1; {}) and all primes in [3; {}] in {}ms", residueLimit, primeLimit,
                System.currentTimeMillis() - startTime);
    }
}
