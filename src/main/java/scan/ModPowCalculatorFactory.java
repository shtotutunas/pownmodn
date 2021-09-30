package scan;

import common.Common;

import java.math.BigInteger;
import java.util.stream.Stream;

public class ModPowCalculatorFactory {
    private final BigInteger base;
    private final BigInteger[] basePowers;
    private final BigInteger lastPower;

    public ModPowCalculatorFactory(BigInteger base) {
        assert base.compareTo(BigInteger.TWO) >= 0;

        Stream.Builder<BigInteger> basePowersBuf = Stream.builder();
        BigInteger basePow = BigInteger.ONE;
        while (basePow.compareTo(Common.MAX_LONG) <= 0) {
            basePowersBuf.add(basePow);
            basePow = basePow.multiply(base);
        }

        this.base = base;
        this.basePowers = basePowersBuf.build().toArray(BigInteger[]::new);
        this.lastPower = BigInteger.valueOf(basePowers.length-1);
    }

    public ModPowCalculator createCalculator(BigInteger exp) {
        if (basePowers.length < 2) {
            return new ModPowCalculator(base, exp, BigInteger.ONE);
        }
        BigInteger[] dr = exp.divideAndRemainder(lastPower);
        return new ModPowCalculator(basePowers[basePowers.length-1], dr[0], basePowers[dr[1].intValueExact()]);
    }

}
