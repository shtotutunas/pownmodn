package factorization;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Factorization {
    private final NavigableMap<BigInteger, Integer> factors;

    private Factorization(NavigableMap<BigInteger, Integer> factors) {
        this.factors = Collections.unmodifiableNavigableMap(factors);
    }

    public static Factorization fromSingleFactor(BigInteger factor, boolean isPrime) {
        NavigableMap<BigInteger, Integer> factors = new TreeMap<>();
        factors.put(factor, isPrime ? 1 : -1);
        return new Factorization(factors);
    }

    public static Factorization fromPrimeFactors(Collection<BigInteger> primeFactors) {
        NavigableMap<BigInteger, Integer> factors = new TreeMap<>();
        var func = remappingFunction(1);
        primeFactors.forEach(f -> factors.compute(f, func));
        return new Factorization(factors);
    }

    public static Factorization multiply(Factorization a, Factorization b) {
        NavigableMap<BigInteger, Integer> factors = new TreeMap<>(a.factors);
        b.factors.forEach((p, n) -> factors.compute(p, remappingFunction(n)));
        return new Factorization(factors);
    }

    private static BiFunction<BigInteger, Integer, Integer> remappingFunction(int power) {
        return (p, prev) -> {
            if (prev == null) {
                return power;
            } else if (Math.signum(prev) == Math.signum(power)) {
                return prev + power;
            } else {
                return Math.abs(prev) + Math.abs(power);
            }
        };
    }

    public void forEachDivisor(Consumer<BigInteger> consumer) {
        forEachDivisor(BigInteger.ONE, BigInteger.ONE, consumer);
    }

    private void forEachDivisor(BigInteger d, BigInteger prev, Consumer<BigInteger> consumer) {
        Map.Entry<BigInteger, Integer> entry = factors.higherEntry(prev);
        if (entry == null) {
            consumer.accept(d);
            return;
        }
        forEachDivisor(d, entry.getKey(), consumer);
        for (int i = 1; i <= Math.abs(entry.getValue()); i++) {
            d = d.multiply(entry.getKey());
            forEachDivisor(d, entry.getKey(), consumer);
        }
    }

    public int compositeCount() {
        return (int) factors.values().stream().filter(x -> x < 0).count();
    }

    public Set<BigInteger> composites() {
        return factors.entrySet().stream().filter(e -> e.getValue() < 0).map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String toString() {
       StringBuilder buf = new StringBuilder();
       factors.forEach((p, n) -> {
           if (buf.length() > 0) {
               buf.append(" * ");
           }
           buf.append(p);
           for (int i = 2; i <= Math.abs(n); i++) {
               buf.append(" * ").append(p);
           }
       });
       return buf.toString();
    }
}
