package minicp.examples.tsptw;

import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.IntStream;

public class Main {

    private final String fname;
    private final int    timeout;

    private final long   start;
    private long         time;
    private double       objective;
    private int[]        solution;
    private boolean      crashed;
    private boolean      closed;
    private String       error;
    private long         seed;
    private int          verbosity;

    public boolean isCrashed() {
        return crashed;
    }

    public double getObjective() {
        return objective;
    }

    public int[] getSolution() {
        return solution;
    }

    /**
     * Creates a main to run
     *
     * @param fname file where the instance is described
     * @param timeout timeout for solving the instance (in seconds)
     * @param seed seed to use for random number generation of the solver
     * @param verbosity verbosity level: the higher, the more details are given
     */
    public Main(final String fname, final int timeout, final long seed, final int verbosity) {
        this.fname              = fname;
        this.timeout            = timeout;

        this.start              = System.currentTimeMillis();
        this.time               = this.start;
        this.crashed            = false;
        this.error              = null;
        this.closed             = false;
        this.seed               = seed;
        this.verbosity          = verbosity;
    }

    public static Main instanciate(String[] args) throws Exception {
        CommandLine cli = cli(args);

        String fname   = cli.getOptionValue("f");
        int    timeout = Integer.parseInt(cli.getOptionValue("t", "600"));
        long seed = Long.parseLong(cli.getOptionValue("r", String.valueOf(42)));
        int verbosity = Integer.parseInt(cli.getOptionValue("v", String.valueOf(1)));
        return new Main(fname, timeout, seed, verbosity);
    }

    /**
     * Example of usage:
     *
     * minicp.examples.tsptw.Main -f data/TSPTW/instances/AFG/rbg132.tw
     *
     * @param args refer to {@link Main#options()} for a list of valid arguments
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        Main main = instanciate(args);
        main.solve();
        System.out.println(main.toString());
    }

    private String status() {
        if (crashed) {
            return "crashed";
        }
        if (closed) {
            return "closed";
        } else {
            return "open";
        }
    }

    public void solve() {
        try {
            TsptwInstance instance = TsptwParser.fromFile(fname);
            TsptwSolver solver     = new TsptwSolver(instance, timeout);
            solver.setVerbosity(verbosity);
            solver.setSeed(seed);

            solver.addObserver((solution, objective) -> {
                this.time       = System.currentTimeMillis();
                double obj = (((double)objective) / TsptwInstance.PRECISION);
                this.objective  = obj;
                if (this.solution == null) {
                    this.solution = new int[instance.nbNodes];
                }
                System.arraycopy(solution, 0, this.solution, 0, this.solution.length);
            });
            TsptwResult result = solver.satisfy();
            //TsptwResult result = solver.optimize();
            this.closed  = result.isOptimum;
        } catch (Throwable e) {
            this.crashed = true;
            this.error   = e.getMessage();
        }
    }

    private static final String instanceName(final String fname) {
        String[] chunks = fname.split("/");
        if (chunks.length < 2) {
            return chunks[0];
        } else {
            return String.format("%s/%s", chunks[chunks.length-2], chunks[chunks.length-1]);
        }
    }

    private static Options options() {
        Options options = new Options();
        options.addOption("f", "filename",true, "instance file");
        options.addOption("t", true, "timeout");
        options.addOption("r", true, "seed");
        options.addOption("v", true, "verbosity");
        return options;
    }

    private static CommandLine cli(final String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options(), args);

        return cmd;
    }

    private static int[] initial(final String solution) {
        if (solution == null)
            return null;
        return IntStream.concat(
                IntStream.of(0),
                Arrays.stream(solution.split("\s+"))
                    .mapToInt(x -> Integer.parseInt(x))
            ).toArray();
    }

    @Override
    public String toString() {
        StringJoiner join = new StringJoiner(" ");
        Arrays.stream(solution)
                .skip(1)
                .forEach(x -> join.add(""+x));

        String solution = join.toString();

        return String.format("%10s | %10s | %10s | %10.2f | %10.2f | %s",
                instanceName(fname),
                "sequence",
                status(),
                objective,
                elapsedTime(),
                crashed ? error : solution);
    }

    public double elapsedTime() {
        return (time - start) / 1000.0;
    }
}
