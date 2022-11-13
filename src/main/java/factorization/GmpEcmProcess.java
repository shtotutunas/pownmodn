package factorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;

public class GmpEcmProcess implements Closeable {
    private static final String GMP_ECM_PARAM = "gmpecm.path";
    private static final Logger log = LoggerFactory.getLogger(GmpEcmProcess.class);

    private final Process process;
    private final BufferedWriter writer;
    private final BufferedReader reader;

    private GmpEcmProcess(Process process) {
        this.process = process;
        this.writer = process.outputWriter();
        this.reader = process.inputReader();
    }

    public static GmpEcmProcess create(long B1, long curvesNum) {
        String gmpEcmPath = System.getProperty(GMP_ECM_PARAM);
        if (gmpEcmPath == null) {
            throw new IllegalStateException("VM option -D" + GMP_ECM_PARAM + " with path to GMP-ECM executable file is not found. Cannot use GMP-ECM for factorization");
        }
        try {
            Process process = new ProcessBuilder(gmpEcmPath, "-q", "-one", "-c", String.valueOf(curvesNum), String.valueOf(B1)).start();
            return new GmpEcmProcess(process);
        } catch (IOException e) {
            throw new RuntimeException("Exception when try to run GMP-ECM", e);
        }
    }

    public BigInteger tryFindFactor(BigInteger N) {
        try {
            String Nstr = String.valueOf(N);
            String input = Nstr + "*1";
            writer.write(input);
            writer.write("\n");
            writer.flush();

            while (true) {
                String line = reader.readLine().strip();
                if (line.equals(input)) {
                    return null;
                } else if (line.equals(Nstr)) {
                    return N;
                }

                int p = 0;
                while (p < line.length() && Character.isDigit(line.charAt(p))) {
                    p++;
                }
                if (p > 0) {
                    BigInteger d = new BigInteger(line.substring(0, p));
                    if (N.mod(d).signum() == 0) {
                        return d;
                    }
                }

                p = line.length()-1;
                while (p >= 0 && Character.isDigit(line.charAt(p))) {
                    p--;
                }
                if (p < line.length()-1) {
                    BigInteger d = new BigInteger(line.substring(p+1));
                    if (N.mod(d).signum() == 0) {
                        return d;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception while trying to factorize " + N, e);
        }
    }

    public void close() {
        process.destroy();
    }

}
