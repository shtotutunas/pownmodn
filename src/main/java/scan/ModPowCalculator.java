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
            long _mod = mod.longValueExact();
            LongBinaryOperator modMultiplier = Modules.modMultiplier(_mod);
            if (modMultiplier != null) {
                long baseMod = (baseLong != 0) ? Common.mod(baseLong, _mod) : Common.mod(baseBig, mod).longValueExact();
                long result = pow(baseMod, exp, modMultiplier);
                if (multiplierLong != 1) {
                    result = modMultiplier.applyAsLong(result, Common.mod(multiplierLong, _mod));
                }
                return BigInteger.valueOf(result);
            }
        }

        if (multiplierLong == 1) {
            return baseBig.modPow(exp, mod);
        } else {
            return Common.mod(baseBig.modPow(exp, mod).multiply(multiplierBig), mod);
        }
    }

    private static long pow(long b, BigInteger n, LongBinaryOperator modMultiplier) {
        assert n.signum() >= 0;
        if (n.signum() == 0) {
            return modMultiplier.applyAsLong(1, 1);
        }
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
