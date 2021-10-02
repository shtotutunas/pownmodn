package scan;

import common.Common;
import common.Modules;

import java.math.BigInteger;
import java.util.function.LongBinaryOperator;

public class ModPowCalculator {
    private final long baseLong;
    private final BigInteger baseBig;
    private final BigInteger exp;
    private final long multiplierLong;
    private final BigInteger multiplierBig;

    ModPowCalculator(BigInteger base, BigInteger exp, BigInteger multiplier) {
        this.baseLong = (base.compareTo(Common.MAX_LONG) <= 0) ? base.longValueExact() : 0;
        this.baseBig = base;
        this.exp = exp;
        this.multiplierLong = multiplier.longValueExact();
        this.multiplierBig = multiplier;
    }

    public BigInteger calculate(BigInteger mod) {
        if (exp.signum() == 0) {
            return Common.mod(multiplierBig, mod);
        }
        if (mod.compareTo(Common.MAX_LONG) <= 0) {
            long result = tryWithLong(mod.longValueExact(), mod);
            if (result >= 0) {
                return BigInteger.valueOf(result);
            }
        }
        return calculateWithBigInteger(mod);
    }

    public long calculate(long mod) {
        if (exp.signum() == 0) {
            return Common.mod(multiplierLong, mod);
        }
        long result = tryWithLong(mod, null);
        if (result >= 0) {
            return result;
        }
        return calculateWithBigInteger(BigInteger.valueOf(mod)).longValueExact();
    }

    private long tryWithLong(long mod, BigInteger modBig) {
        LongBinaryOperator modMultiplier = Modules.modMultiplier(mod);
        if (modMultiplier == null) {
            return -1;
        }

        long baseMod;
        if (baseLong != 0) {
            baseMod = Common.mod(baseLong, mod);
        } else {
            if (modBig == null) {
                modBig = BigInteger.valueOf(mod);
            }
            baseMod = Common.mod(baseBig, modBig).longValueExact();
        }

        long result = pow(baseMod, exp, modMultiplier);
        if (multiplierLong != 1) {
            result = modMultiplier.applyAsLong(result, Common.mod(multiplierLong, mod));
        }
        return result;
    }

    private BigInteger calculateWithBigInteger(BigInteger mod) {
        if (multiplierLong == 1) {
            return baseBig.modPow(exp, mod);
        } else {
            return Common.mod(baseBig.modPow(exp, mod).multiply(multiplierBig), mod);
        }
    }

    private static long pow(long b, BigInteger n, LongBinaryOperator modMultiplier) {
        assert n.signum() > 0;
        int len = n.bitLength();

        int p = 0;
        long t = b;
        while (!n.testBit(p)) {
            t = modMultiplier.applyAsLong(t, t);
            p++;
        }

        long res = t;
        for (int i = p+1; i < len; i++) {
            t = modMultiplier.applyAsLong(t, t);
            if (n.testBit(i)) {
                res = modMultiplier.applyAsLong(res, t);
            }
        }
        return res;
    }
}
