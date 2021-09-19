package common;

import java.math.BigInteger;
import java.util.function.LongBinaryOperator;

public interface LongMod {
    void setStepMultiplier(long multiplier);
    void setAuxMultiplier(long multiplier);
    long step();
    long getAux();
    long getMod();

    static LongMod create(long mod) {
        assert mod > 0;
        if (mod <= Common.MULTIPLY_HIGH_BOUND) {
            return new LongBased(mod);
        } else {
            return new BigIntegerBased(mod);
        }
    }

    class LongBased implements LongMod {
        private final long mod;
        private final LongBinaryOperator modMultiplier;
        private long step = 1;
        private long aux = 1;
        private long current = 1;

        private LongBased(long mod) {
            this.mod = mod;
            this.modMultiplier = (mod <= Common.MAX_LONG_SQRT) ? Modules.modMultiplier32bit(mod) : Modules.modMultiplier42bit(mod);
        }

        @Override
        public void setStepMultiplier(long multiplier) {
            this.step = Common.mod(multiplier, mod);
        }

        @Override
        public void setAuxMultiplier(long multiplier) {
            this.aux = Common.mod(multiplier, mod);
        }

        @Override
        public long step() {
            current = modMultiplier.applyAsLong(current, step);
            return current;
        }

        @Override
        public long getAux() {
            return modMultiplier.applyAsLong(current, aux);
        }

        @Override
        public long getMod() {
            return mod;
        }
    }

    class BigIntegerBased implements LongMod {
        private final long _mod;
        private final BigInteger mod;
        private BigInteger step = BigInteger.ONE;
        private BigInteger aux = BigInteger.ONE;
        private BigInteger current = BigInteger.ONE;

        private BigIntegerBased(long mod) {
            assert mod > 0;
            this._mod = mod;
            this.mod = BigInteger.valueOf(mod);
        }

        @Override
        public void setStepMultiplier(long multiplier) {
            this.step = BigInteger.valueOf(Common.mod(multiplier, _mod));
        }

        @Override
        public void setAuxMultiplier(long multiplier) {
            this.aux = BigInteger.valueOf(Common.mod(multiplier, _mod));
        }

        @Override
        public long step() {
            current = current.multiply(step).mod(mod);
            return current.longValueExact();
        }

        @Override
        public long getAux() {
            return current.multiply(aux).mod(mod).longValueExact();
        }

        @Override
        public long getMod() {
            return _mod;
        }
    }
}
