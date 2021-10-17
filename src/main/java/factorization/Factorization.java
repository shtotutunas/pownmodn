package factorization;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Factorization {
    private final BigInteger[] factors;
    private final Set<BigInteger> composites;

    private Factorization(BigInteger[] factors, Set<BigInteger> composites) {
        this.factors = factors;
        this.composites = composites;
    }

    public static Factorization fromSingleFactor(BigInteger factor, boolean isPrime) {
        return new Factorization(new BigInteger[] {factor}, isPrime ? Set.of() : Set.of(factor));
    }

    public static Factorization fromPrimeFactors(Collection<BigInteger> primeFactors) {
        return new Factorization(primeFactors.stream().sorted().toArray(BigInteger[]::new), Set.of());
    }

    public static Factorization multiply(Factorization a, Factorization b) {
        return new Factorization(Stream.concat(Arrays.stream(a.factors), Arrays.stream(b.factors)).sorted().toArray(BigInteger[]::new),
                Stream.concat(a.composites.stream(), b.composites.stream()).collect(Collectors.toUnmodifiableSet()));
    }

    public void forEachDivisor(Consumer<BigInteger> consumer) {
        forEachDivisor(factors, consumer);
    }

    public int compositeCount() {
        return composites.size();
    }

    public Set<BigInteger> composites() {
        return composites;
    }

    public static long[] generateDivisors(long[] primeFactorsSorted) {
        LongStream.Builder buf = LongStream.builder();
        forEachDivisor(Arrays.stream(primeFactorsSorted).boxed().toArray(Long[]::new), 1L, Math::multiplyExact, buf::add);
        return buf.build().sorted().toArray();
    }

    public static void forEachDivisor(BigInteger[] primeFactorsSorted, Consumer<BigInteger> consumer) {
        forEachDivisor(primeFactorsSorted, BigInteger.ONE, BigInteger::multiply, consumer);
    }

    private static <T> void forEachDivisor(T[] primeFactors, T one, BinaryOperator<T> multiplier, Consumer<T> consumer) {
        forEachDivisor(primeFactors, 0, one, multiplier, consumer);
    }

    private static <T> void forEachDivisor(T[] f, int pos, T cur, BinaryOperator<T> multiplier, Consumer<T> consumer) {
        if (pos >= f.length) {
            consumer.accept(cur);
            return;
        }
        int end = pos+1;
        while ((end < f.length) && Objects.equals(f[pos], f[end])) {
            end++;
        }
        forEachDivisor(f, end, cur, multiplier, consumer);
        for (int i = pos; i < end; i++) {
            cur = multiplier.apply(cur, f[pos]);
            forEachDivisor(f, end, cur, multiplier, consumer);
        }
    }

    @Override
    public String toString() {
        if (factors.length == 0) {
            return "1";
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < factors.length; i++) {
            if (i > 0) {
                buf.append(" * ");
            }
            buf.append(factors[i]);
        }
        return buf.toString();
    }
}
