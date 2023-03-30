package minicp.examples.tsptw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Generates statistics regarding the instance
 */
public class InstanceStat {

    private final TsptwInstance instance;

    private final int longestDistance;
    private final int longestDuration;

    public InstanceStat(TsptwInstance instance) {
        this.instance = instance;
        int longest = Integer.MIN_VALUE;
        for (int[] distArray: instance.distances) {
            for (int dist: distArray) {
                longest = Math.max(longest, dist);
            }
        }
        longestDistance = longest;
        longest = Integer.MIN_VALUE;
        for (TimeWindow tw: instance.timeWindows) {
            longest = Math.max(longest, tw.latest - tw.earliest);
        }
        longestDuration = longest;
    }

    /**
     *
     * @param node
     * @return number of successors for a node
     */
    public int nSucc(int node) {
        int earliest = instance.timeWindows[node].earliest;
        int nSucc = 0;
        for (int i = 0 ; i < instance.nbNodes ; ++i) {
            if (i != node) {
                if (earliest + instance.distances[node][i] < instance.timeWindows[i].latest) {
                    nSucc += 1;
                }
            }
        }
        return nSucc;
    }

    public int nSucc() {
        int n = 0 ;
        for (int i = 0 ; i < instance.nbNodes ; ++i) {
            n += nSucc(i);
        }
        return n;
    }

    public double normalizedNSucc(int node) {
        return ((double) nSucc(node)) / (instance.nbNodes - 1);
    }

    public double normalizedNSucc() {
        return ((double) nSucc()) / ((instance.nbNodes - 1)*(instance.nbNodes));
    }

    public int overlap(int nodeA, int nodeB) {
        return Math.max(0, Math.min(instance.timeWindows[nodeA].latest, instance.timeWindows[nodeB].latest)
                - Math.max(instance.timeWindows[nodeA].earliest, instance.timeWindows[nodeB].earliest));
    }

    public double normalizedOverlap(int nodeA, int nodeB) {
        double overlap = (double) overlap(nodeA, nodeB);
        assert overlap < longestDuration;
        return overlap / longestDuration;
    }

    public int overlap() {
        int overlap = 0;
        for (int i = 0 ; i < instance.nbNodes ; ++i) {
            for (int j = i + 1; j < instance.nbNodes ; ++j) {
                overlap += overlap(i, j);
            }
        }
        return overlap;
    }

    public double normalizedOverlap() {
        double overlap = 0;
        for (int i = 0 ; i < instance.nbNodes ; ++i) {
            for (int j = i + 1; j < instance.nbNodes ; ++j) {
                overlap += normalizedOverlap(i, j);
            }
        }
        double nPairs = ((double) instance.nbNodes * (instance.nbNodes - 1)) / 2;
        return overlap / nPairs;
    }

    @Override
    public String toString() {
        return String.format("%.4f | %.4f", normalizedNSucc(), normalizedOverlap());
    }

    public static void printAllStats() throws IOException {
        String[] setToSolve = new String[] {
                "data/TSPTW/instances/Langevin",
                "data/TSPTW/instances/Dumas",
                "data/TSPTW/instances/AFG",
                "data/TSPTW/instances/OhlmannThomas",
                "data/TSPTW/instances/GendreauDumasExtended",
                "data/TSPTW/instances/SolomonPesant",
                "data/TSPTW/instances/SolomonPotvinBengio"
        };
        for (String set: setToSolve) {
            for (File f: Objects.requireNonNull(new File(Paths.get(set).toString()).listFiles())) {
                TsptwInstance instance = TsptwParser.fromFile(f.getPath());
                InstanceStat stats = new InstanceStat(instance);
                String name = f.getParentFile().getName() + "/" + f.getName();
                System.out.println(name + " | " + stats);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        printAllStats();
        //TsptwInstance instance = TsptwParser.fromFile("data/TSPTW/instances/OhlmannThomas/n200w140.001.txt");
        //InstanceStat stats = new InstanceStat(instance);
        //System.out.println(stats);
    }

}
