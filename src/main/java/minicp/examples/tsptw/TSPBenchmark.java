package minicp.examples.tsptw;

import minicp.util.Procedure;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * run a bunch of experiments for TSPTW
 */
public class TSPBenchmark {

    public enum MODE {
        SATISFY // find a first feasible solution
    }

    private record ModeSetup(int nRun, int timeout, String directory) {
        public String fullPath() {
            return "data/TSPTW/results/" + directory;
        }
    };

    private static final Map<MODE, ModeSetup> modeSetup = Map.of(
            MODE.SATISFY, new ModeSetup(100, 10, "initial")
    );

    private static final String instancePath = "data/TSPTW/instances"; // path to the instances folder
    private final int maxParallel; // maximum number of threads that can be used in parallel
    // best objective values ever found, written in the bestKnownSol directory
    private static Map<String, Double> bestObjective;
    private final String bestKnownSol = "data/TSPTW/best_known_sol";

    // TODO add unclosed instances
    // files where the initial solutions to start from are written
    private final String[] setToSolve = new String[] {
            "data/TSPTW/best_known_sol/Langevin.txt",
            /*"data/TSPTW/best_known_sol/Dumas.txt",
            "data/TSPTW/best_known_sol/AFG.txt",
            "data/TSPTW/best_known_sol/OhlmannThomas.txt",
            "data/TSPTW/best_known_sol/GendreauDumasExtended.txt",
            "data/TSPTW/best_known_sol/SolomonPesant.txt",
            "data/TSPTW/best_known_sol/SolomonPotvinBengio.txt"
             */
    };

    /**
     * Information from the written solutions on the website
     */
    private record SolutionInfo(String set, String instance, double cost, int CV, int[] permutation) {
        private String intermediatePath() {
            return String.format("%s/%s", set, instance);
        }
    }

    /**
     * An instance to run over several {@link Experiment}
     * {@link Experiment} run the instance and notify their solution to it
     * All solutions are kept
     */
    private class InstanceRun {

        private final String instanceSet; // set to which the instance belongs
        private final String instance; // instance to solve
        private final int[] initSol; // initial ordering to start from (possibly null if start from scratch)
        private String initSolString; // first solution to start from, encoded as a string
        private MODE mode;

        private ArrayList<Solution> resultList = new ArrayList<>(); // all results that were provided for this instance

        public InstanceRun(String instanceSet, String instance, int[] initSol, MODE mode) {
            this.instanceSet = instanceSet;
            this.instance = instance;
            this.initSol = initSol;
            initSolString = initSolString();
            this.mode = mode;
        }

        /**
         * @return Path to the instance
         */
        public String instancePath() {
            return TSPBenchmark.instancePath + '/' + instanceSet + '/' + instance;
        }

        private String intermediatePath() {
            return String.format("%s/%s", instanceSet, instance);
        }

        private String initSolString() {
            if (initSol == null)
                return null;
            return Arrays.stream(initSol)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(" "));
        }

        /**
         * Notifies a solution for solving the related instance
         *
         */
        public void notifySolution(Solution solution) {
            resultList.add(solution);
        }

        public String bestSolution() {
            if (resultList.isEmpty())
                return "";
            if (resultList.size() == 1)
                return resultList.get(0).solutionLine();
            return resultList.stream().min(Solution::compareTo).get().solutionLine();
        }

        /**
         * Gives all {@link Solution#solutionLine()} from all provided solutions, separated by \n
         * @return description of all solutions split over multiple lines
         */
        public String allSolutions() {
            return resultList.stream().map(Solution::solutionLine).collect(Collectors.joining("\n"));
        }

    }

    /**
     * An experiment / run over one {@link InstanceRun}
     */
    private record Experiment(InstanceRun instanceParam, int run, long seed) {

        public String detail() {
            return instanceParam.instancePath();
        }

        /**
         * Arguments to launch the {@link Main}
         * @return
         */
        public String[] getArgs() {
            List<String> specialParam = null;
            switch (instanceParam.mode) {
                case SATISFY -> {
                    specialParam = List.of();
                }
                default -> throw new IllegalArgumentException("unrecognized mode");
            }
            List<String> commonParam = new ArrayList<>(List.of(
                    "-f", instanceParam.instancePath(),
                    "-t", String.valueOf(modeSetup.get(instanceParam.mode).timeout),
                    "-r", String.valueOf(seed),
                    "-v", "0"
            ));
            commonParam.addAll(specialParam);
            return commonParam.toArray(new String[0]);
        }

        /**
         * Runs an experiment over the instance
         */
        private void launch() {
            String[] args = getArgs();
            String seedInfo = " with seed = " + seed;
            try {
                System.out.println("solving " + detail() + seedInfo);
                Main main = Main.instanciate(args);
                main.solve();
                System.out.printf("solved %s in %.3f [s] %s %n", detail(), main.elapsedTime(), seedInfo);
                Solution result = new Solution(main.getSolution(), main.getObjective(), main.toString(), seed, run);
                // stores the results
                if (!main.isCrashed()) {
                    withSemaphore(() -> {
                        instanceParam.notifySolution(result);
                    });
                }
            } catch (Exception ignored) {
                System.err.println("Error when running " + detail() + seedInfo);
            }
        }

    }

    /**
     * Encodes the result of an {@link Experiment}
     */
    private record Solution(int[] ordering, double objective, String detail, long seed, int run) implements Comparable<Solution> {

        /**
         * Gives the solution over one line of strings, where the parameters are split by " | "
         * The format is "instance | solver | status | cost | time elapsed | seed: seed_number | id"
         * Example:
         *
         * AFG/rbg010a.tw |   sequence | open (initial) |     671.00 |       0.00 | 3 1 2 5 4 6 8 7 9 10 | seed: 1625205192 | 0
         *
         * @return
         */
        public String solutionLine() {
            return String.format("%s | seed: %d | %d", detail, seed, run);
        }

        @Override
        public int compareTo(Solution solution) {
            return (int) (this.objective - solution.objective());
        }
    }

    private static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Reads a line and provide a solution from it
     * Null if the line is commented
     *
     * @param line line where the solution is written
     * @return solution associated to the line, null if no solution is provided
     */
    private SolutionInfo readFromLine(String set, String line) {
        if (line.strip().startsWith("#")) // comment line
            return null;
        ArrayList<Integer> initalList = new ArrayList<>();
        String instance = null;
        Double cost = null;
        Integer cv = null;
        for (String val: line.split("\\s+")) {
            if (isInt(val)) {
                int i = Integer.parseInt(val);
                if (cost == null) {
                    cost = (double) i;
                } else if (cv == null) {
                    cv = i;
                } else {
                    initalList.add(i);
                }
            } else if (isDouble(val)) {
                double d = Double.parseDouble(val);
                if (cost == null) {
                    cost = d;
                } else {
                    throw new IllegalArgumentException("the only existing Double in a solution is the cost value");
                }
            } else {
                if (instance == null)
                    instance = val;
                if (val.contains("#"))
                    break;
            }
        }
        if (cv == null || cost == null) {
            throw new IllegalArgumentException("invalid line format:" + line);
        }
        return new SolutionInfo(set, instance, cost, cv, initalList.stream().mapToInt(i -> i).toArray());
    }

    /**
     * Prepares a bunch of instances run from a folder
     * No initial solution for the instances is provided
     *
     * @param solutionFile folder where the solutions are written
     * @return list of instances to run
     * @throws IOException if folder does not exist
     */

    private List<InstanceRun> prepareInstanceRun(String solutionFile, MODE mode) throws IOException {
        ArrayList<InstanceRun> instanceRuns = new ArrayList<>();
        String instanceSet = Paths.get(solutionFile).getFileName().toString().replace(".txt","");
        try (BufferedReader reader = new BufferedReader(new FileReader(solutionFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                SolutionInfo sol = readFromLine(instanceSet, line);
                if (sol != null)
                    instanceRuns.add(new InstanceRun(instanceSet, sol.instance, sol.permutation(), mode));
            }
        }
        return instanceRuns;
    }

    public TSPBenchmark() {
        maxParallel = Runtime.getRuntime().availableProcessors() - 1;
        try {
            bestObjective = retrieveBestObjective();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Reads the data/TSPTW/best_known_sol directory and extract the best objective values from it
     * @return map of {instance: best_objective_value}
     */
    private Map<String, Double> retrieveBestObjective() throws IOException {
        Map<String, Double> map = new HashMap<>();
        for (File f: Objects.requireNonNull(new File(Paths.get(bestKnownSol).toString()).listFiles())) {
            String set = f.getName().replace(".txt","");
            try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    SolutionInfo s = readFromLine(set, line);
                    if (s != null)
                        map.put(s.intermediatePath(), s.cost);
                }
            }
        }
        return map;
    }

    private static final Semaphore writerSemaphore = new Semaphore(1); //used to write results to file and notify solutions

    public String getCurrentLocalDateTimeStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm"));
    }

    /**
     * Writes the best results to a file
     * @param instanceRunList
     */
    private void writeBestResults(List<InstanceRun> instanceRunList, String date) {
        writeResults(instanceRunList, InstanceRun::bestSolution, "", date);
    }

    /**
     * Write the results for all runs to a file
     * @param instanceRunList
     */
    private void writeAllResults(List<InstanceRun> instanceRunList, String date) {
        writeResults(instanceRunList, InstanceRun::allSolutions, "/all_xp", date);
    }

    private void writeResults(List<InstanceRun> instanceRunList, Function<InstanceRun, String> f, String subfolder, String date) {
        StringBuilder results = new StringBuilder();
        String set = null;
        InstanceRun sample = null;
        for (InstanceRun instanceRun : instanceRunList) {
            if (sample == null)
                sample = instanceRun;
            String bestSolution = f.apply(instanceRun);
            results.append(bestSolution);
            results.append('\n');
            if (set == null) {
                set = instanceRun.instanceSet;
            }
        }
        assert sample != null;
        writeResults(modeSetup.get(sample.mode).fullPath() + subfolder, set, results.toString(), date);
    }

    private void writeResults(String path, String instanceSet, String results, String date) {
        String filePath = path + "/" + instanceSet + "_" + date + ".txt";
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write(results);
            writer.close();
        } catch (IOException exception) {
            System.err.println("failed to write results to " + filePath);
            System.err.println("results = " + results);
        }
    }


    private static void withSemaphore(Procedure procedure) {
        try {
            writerSemaphore.acquire();
            procedure.call();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            writerSemaphore.release();
        }
    }

    /**
     * Tries to find a first feasible solution on the instances
     */
    public void solve_satisfy() {
        solve(MODE.SATISFY, setToSolve);
    }

    public void solve(MODE mode, String[] setOfInstances) {
        int maxListSize = 2 * maxParallel;
        int nRun = modeSetup.get(mode).nRun;
        for (String instanceSet: setOfInstances) {
            List<InstanceRun> instanceRunsList;
            try {
                instanceRunsList = prepareInstanceRun(instanceSet, mode);
            } catch (IOException e) {
                System.err.println("failed to read " + instanceSet);
                continue;
            }
            // prepare a bunch of experiments
            ArrayList<Experiment> experimentsList = new ArrayList<>();
            for (InstanceRun instanceRun: instanceRunsList) {
                for (int i = 0; i < nRun; ++i) {
                    long seed = new Random().nextLong();
                    experimentsList.add(new Experiment(instanceRun, i, seed));
                    if (experimentsList.size() == maxListSize) { // run experiments
                        experimentsList.stream().parallel().forEach(Experiment::launch);
                        experimentsList.clear();
                    }
                }
            }
            experimentsList.stream().parallel().forEach(Experiment::launch);
            String date = getCurrentLocalDateTimeStamp();
            writeBestResults(instanceRunsList, date);
            writeAllResults(instanceRunsList, date);
        }
    }

    public static void main(String[] args) {
        TSPBenchmark benchmark = new TSPBenchmark();
        benchmark.solve_satisfy();
    }

}
