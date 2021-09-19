import common.Modules;
import org.apache.commons.math3.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class TestUtils {
    public static Pair<Map<Long, Long>, Map<Long, Long>> generateAB(long base, long n, long absT) {
        Map<Long, Long> A = new HashMap<>();
        Map<Long, Long> B = new HashMap<>();
        long x = 1;
        long bn = Modules.pow(base, n, n);
        for (long e = 0; e <= 2*n; e++) {
            for (long y = x; y <= absT; y+=n) {
                Long b = B.get(y);
                if (b == null) {
                    B.put(y, e);
                } else if (!A.containsKey(y)) {
                    A.put(y, e-b);
                }
            }

            for (long y = x-n; y >= -absT; y-=n) {
                Long b = B.get(y);
                if (b == null) {
                    B.put(y, e);
                } else if (!A.containsKey(y)) {
                    A.put(y, e-b);
                }
            }

            x = (x*bn)%n;
        }
        return Pair.create(A, B);
    }
}
