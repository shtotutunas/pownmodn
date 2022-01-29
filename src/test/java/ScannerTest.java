import common.TaskExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import primes.Primes;
import scan.ScanSieve;
import scan.Scanner;

import java.math.BigInteger;
import java.util.Random;
import java.util.stream.IntStream;

public class ScannerTest {

    @Test
    public void testScan() {
        testScan(3701, -3264853849480005738L, new BigInteger("29889291908833087715846680937"),
                1784641993, 218699522, (long) 8e10, (long) 1e4);
    }

    private void testScan(int base, long target, BigInteger N, long C, long A, long length, long speedUp) {
        ScanSieve scanSieve = new ScanSieve(BigInteger.valueOf(base), BigInteger.valueOf(target), new Primes(100), null);
        Scanner scanner = new Scanner(BigInteger.valueOf(base), target, scanSieve,
                TaskExecutor.create(1), 1<<23, 1);

        A = Math.multiplyExact(A, speedUp);
        long B = N.divide(BigInteger.valueOf(C)).mod(BigInteger.valueOf(A)).longValueExact();
        var result = scanner.scan(BigInteger.valueOf(C), BigInteger.valueOf(A), BigInteger.valueOf(B),
                length/speedUp, true);
        Assertions.assertArrayEquals(new BigInteger[] {N.divide(BigInteger.valueOf(C))}, result.getFirst());
    }

    public static void main(String[] args) {
        int baseLimit = 1<<12;
        BigInteger C = BigInteger.valueOf(263L * 227L * 179L * 167L * 107L);// * 83L);
        BigInteger[] bases = IntStream.range(2, baseLimit).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new);
        Random random = new Random();

        BigInteger min = BigInteger.valueOf(Long.MIN_VALUE);
        BigInteger max = BigInteger.valueOf(Long.MAX_VALUE);
        while (true) {
            BigInteger P = BigInteger.probablePrime(64, random);
            BigInteger N = P.multiply(C);
            for (BigInteger base : bases) {
                BigInteger T = base.modPow(N, N);
                boolean good = false;
                if (T.compareTo(max) <= 0) {
                    good = true;
                } else {
                    T = T.subtract(N);
                    if (T.compareTo(min) >= 0) {
                        good = true;
                    }
                }

                if (good) {
                    System.out.println(base + "^" + N + " = " + T + " (mod " + N + "),  C = " + C + ",  P = " + P);
                    System.out.flush();
                }
            }
        }
    }
}
