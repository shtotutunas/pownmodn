package factorization;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FactorizationDB {
    private static final Logger log = LoggerFactory.getLogger(FactorizationDB.class);
    private static final String dbFileName = "factorizationDB.txt";

    private final Map<Integer, Factorization> factorizations;

    private FactorizationDB(Map<Integer, Factorization> factorizations) {
        this.factorizations = Map.copyOf(factorizations);
    }

    public Factorization get(int pow) {
        return factorizations.get(pow);
    }

    public int size() {
        return factorizations.size();
    }

    public static FactorizationDB initialize(BigInteger base, BigInteger target, int primeTestCertainty) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                FactorizationDB.class.getClassLoader().getResourceAsStream(dbFileName))))
        {
            Stream.Builder<String> lines = Stream.builder();
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            return initialize(base, target, primeTestCertainty, lines.build().toArray(String[]::new));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static FactorizationDB initialize(BigInteger base, BigInteger target, int primeTestCertainty, String[] lines) {
        Map<Integer, Factorization> factorizations = new HashMap<>();
        Set<BigInteger> checkedPrimes = new HashSet<>();
        Pattern pattern = Pattern.compile("^" + base + "\\^(\\d+)" + (target.signum() > 0 ? "-" : "\\+") + target.abs() + " = ([0-9p* ]+)");
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                try {
                    int pow = Integer.parseInt(matcher.group(1));
                    BigInteger N = base.pow(pow).subtract(target);
                    factorizations.put(pow, parseFactorization(N, matcher.group(2), primeTestCertainty, checkedPrimes));
                } catch (Exception e) {
                    log.error("Cannot process line: {}", line, e);
                }
            }
        }
        return new FactorizationDB(factorizations);
    }

    private static Factorization parseFactorization(BigInteger N, String s, int primeTestCertainty, Set<BigInteger> checkedPrimes) {
        String[] terms = Stream.of(s.split("\\*")).map(String::strip).toArray(String[]::new);
        List<BigInteger> factors = new ArrayList<>();
        boolean pFlag = false;
        for (String term : terms) {
            if (term.startsWith("p")) {
                if (pFlag) {
                    throw new IllegalArgumentException("Two p-terms in one factorization");
                }
                pFlag = true;
                continue;
            }
            BigInteger p = new BigInteger(term);
            if (!checkedPrimes.contains(p)) {
                if (p.isProbablePrime(primeTestCertainty)) {
                    checkedPrimes.add(p);
                } else {
                    throw new IllegalArgumentException("Not prime: " + p);
                }
            }
            BigInteger[] dr = N.divideAndRemainder(p);
            if (dr[1].signum() != 0) {
                throw new IllegalArgumentException("Does not divide " + p);
            }
            N = dr[0];
            factors.add(p);
        }

        if (!N.equals(BigInteger.ONE)) {
            if (!pFlag) {
                log.error("Last factor {} is missing in line: {}", N, s);
            }
            if (!checkedPrimes.contains(N)) {
                if (N.isProbablePrime(primeTestCertainty)) {
                    checkedPrimes.add(N);
                } else {
                    throw new IllegalArgumentException("Not prime: " + N);
                }
            }
            factors.add(N);
        }
        return Factorization.fromPrimeFactors(factors);
    }

    public static void logFactorizations(BigInteger base, BigInteger target, SortedMap<BigInteger, Long> scanTime,
                                         Factorizer factorizer, int threadsNumber)
    {
        ExecutorService executor = Executors.newFixedThreadPool(threadsNumber);
        SortedMap<BigInteger, Future<Factorization>> tasks = new TreeMap<>();
        scanTime.forEach((exp, x) -> tasks.put(exp, executor.submit(() -> factorizer.factorize(base.pow(exp.intValueExact()).subtract(target)))));
        List<Pair<BigInteger, Long>> notFactorized = new ArrayList<>();
        tasks.forEach((exp, task) -> {
            Factorization factorization;
            try {
                factorization = task.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println("---------------------------");
            System.out.println(visualize(base, exp, target) + " = " + factorization);
            if (factorization.compositeCount() > 0) {
                System.out.println("Composites: " + factorization.composites());
                notFactorized.add(Pair.create(exp, scanTime.get(exp)));
            }
            System.out.flush();
        });

        System.out.println("---------------------------");
        System.out.println("Prioritized list of not factorized yet:");
        notFactorized.sort(Comparator.comparing(Pair::getSecond, Comparator.reverseOrder()));
        for (var item : notFactorized) {
            System.out.println(visualize(base, item.getFirst(), target) + ": candidates=" + item.getSecond());
        }

        System.out.flush();
        executor.shutdownNow();
    }

    private static String visualize(BigInteger base, BigInteger exp, BigInteger target) {
        return base + "^" + exp + ((target.signum() > 0) ? "" : "+") + target.negate();
    }
}
