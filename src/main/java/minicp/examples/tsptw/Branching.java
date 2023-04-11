package minicp.examples.tsptw;

import minicp.engine.constraints.sequence.Insert;
import minicp.engine.core.IntVar;
import minicp.engine.core.OldSeqVar;
import minicp.util.Procedure;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

public class Branching {

    public static int detourCost(int node, int pred, OldSeqVar route, IntVar[] time, int[][] distances) {
        int succ = route.nextMember(pred);
        int objChange = distances[node][succ] + distances[pred][node] - distances[pred][succ];
        return objChange;
    }
    
    public static int detourAndSlackCost(int node, int pred, OldSeqVar route, IntVar[] time, int[][] distances) {
        return detourCost(node, pred, route, time, distances) + slackCost(node, pred, route, time, distances);
    }
    
    public static int slackCost(int node, int pred, OldSeqVar route, IntVar[] time, int[][] distances) {
        int succ = route.nextMember(pred);
        return - (time[succ].max() - (time[pred].min() + distances[pred][node] + distances[node][succ]));
    }
    
    // variable / node selection

    public static Supplier<Integer> maxRegret(OldSeqVar OldSeqVar, IntVar[] time, int[][] distances) {
        int[] nodes = new int[OldSeqVar.nNode()];
        int[] insertions = new int[OldSeqVar.nNode()];
        return () -> {
            if (OldSeqVar.isFixed())
                return -1;
            int size = OldSeqVar.fillPossible(nodes);
            int bestNode = -1;
            int maxRegret = Integer.MIN_VALUE;
            for (int i = 0 ; i < size ; ++i) {
                int node = nodes[i];
                int nInsert = OldSeqVar.fillMemberPredInsert(node, insertions);
                int minCost1 = Integer.MAX_VALUE;
                int minCost2 = Integer.MAX_VALUE; // minCost1 < minCost2
                for (int j = 0 ; j < nInsert ; ++j) {
                    int cost = detourAndSlackCost(nodes[i], insertions[j], OldSeqVar, time, distances);
                    if (cost < minCost2) {
                        if (cost < minCost1) { // better than all values that were previously found
                            minCost2 = minCost1;
                            minCost1 = cost;
                        } else { // better than minCost2 but not than minCost1
                            minCost2 = cost;
                        }
                    }
                }
                int regret = minCost2 - minCost1;
                if (regret > maxRegret) {
                    bestNode = nodes[i];
                    maxRegret = regret;
                }
            }
            return bestNode;
        };
    }

    public static Supplier<Integer> maxRegretWithCutoff(OldSeqVar seqVar, IntVar[] time, int[][] distances) {
        int[] nodes = new int[seqVar.nNode()];
        int[] insertions = new int[seqVar.nNode()];
        return () -> {
            if (seqVar.isExcluded(67)) {
                int a = 0;
            }
            boolean cutoff = (((double) seqVar.nMember()) / seqVar.nNode()) >= 0.5;
            if (cutoff) {
                seqVar.excludeAllPossible();
                seqVar.getSolver().fixPoint();
                return -1;
            }
            if (seqVar.isFixed())
                return -1;
            int size = seqVar.fillPossible(nodes);
            int bestNode = -1;
            int maxRegret = Integer.MIN_VALUE;
            for (int i = 0 ; i < size ; ++i) {
                int node = nodes[i];
                int nInsert = seqVar.fillMemberPredInsert(node, insertions);
                int minCost1 = Integer.MAX_VALUE;
                int minCost2 = Integer.MAX_VALUE; // minCost1 < minCost2
                for (int j = 0 ; j < nInsert ; ++j) {
                    int cost = detourAndSlackCost(nodes[i], insertions[j], seqVar, time, distances);
                    if (cost < minCost2) {
                        if (cost < minCost1) { // better than all values that were previously found
                            minCost2 = minCost1;
                            minCost1 = cost;
                        } else { // better than minCost2 but not than minCost1
                            minCost2 = cost;
                        }
                    }
                }
                int regret = minCost2 - minCost1;
                if (regret > maxRegret) {
                    bestNode = nodes[i];
                    maxRegret = regret;
                }
            }
            return bestNode;
        };
    }

    public static Supplier<Integer> minEarliest(OldSeqVar OldSeqVar, IntVar[] tw) {
        int[] nodes = new int[OldSeqVar.nNode()];
        return () -> {
            if (OldSeqVar.isFixed())
                return -1;
            int size = OldSeqVar.fillPossible(nodes);
            int bestNode = -1;
            int minEarliest = Integer.MAX_VALUE;
            for (int i = 0 ; i < size ; ++i) {
                int node = nodes[i];
                if (tw[node].min() < minEarliest) {
                    bestNode = node;
                    minEarliest = tw[node].min();
                }
            }
            return bestNode;
        };
    }


    public static Supplier<Integer> minLatest(OldSeqVar OldSeqVar, IntVar[] tw) {
        int[] nodes = new int[OldSeqVar.nNode()];
        return () -> {
            if (OldSeqVar.isFixed())
                return -1;
            int size = OldSeqVar.fillPossible(nodes);
            int bestNode = -1;
            int minLatest = Integer.MAX_VALUE;
            for (int i = 0 ; i < size ; ++i) {
                int node = nodes[i];
                if (tw[node].max() < minLatest) {
                    bestNode = node;
                    minLatest = tw[node].max();
                }
            }
            return bestNode;
        };
    }


    public static Supplier<Integer> minTWSize(OldSeqVar OldSeqVar, IntVar[] tw) {
        int[] nodes = new int[OldSeqVar.nNode()];
        return () -> {
            if (OldSeqVar.isFixed())
                return -1;
            int size = OldSeqVar.fillPossible(nodes);
            int bestNode = -1;
            int minTWSize = Integer.MAX_VALUE;
            for (int i = 0 ; i < size ; ++i) {
                int node = nodes[i];
                if (tw[node].size() < minTWSize) {
                    bestNode = node;
                    minTWSize = tw[node].size();
                }
            }
            return bestNode;
        };
    }


    public static Supplier<Integer> minInsert(OldSeqVar OldSeqVar, Random random) {
        int[] nodes = new int[OldSeqVar.nNode()];
        int[] insertion = new int[OldSeqVar.nNode()];
        return () -> {
            if (OldSeqVar.isFixed()) // all nodes have been sequenced
                return -1;

            // select the node with the least insertions points
            int size = OldSeqVar.fillPossible(nodes);
            int minInsert = Integer.MAX_VALUE;
            int nFound = 0;
            for (int i = 0; i < size; ++i) {
                int nInsert = OldSeqVar.nMemberPredInsert(nodes[i]);
                if (nInsert < minInsert && nInsert > 0) {
                    minInsert = nInsert;
                    insertion[0] = nodes[i];
                    nFound = 1;
                } else if (nInsert == minInsert) {
                    insertion[nFound++] = nodes[i];
                }
            }
            if (nFound == 0)
                throw INCONSISTENCY;

            return insertion[random.nextInt(nFound)];
        };
    }

    // branching itself

    public static Supplier<Function<Integer, Procedure[]>> minDetour(OldSeqVar OldSeqVar, IntVar[] tw, int[][] dist) {
        int[] nodes = new int[OldSeqVar.nNode()];
        Procedure[] branching = new Procedure[OldSeqVar.nNode()];
        Integer[] heuristicVal = new Integer[OldSeqVar.nNode()];
        Integer[] branchingRange = new Integer[OldSeqVar.nNode()];
        return () -> (Integer node) -> {
            int minInsert = OldSeqVar.fillMemberPredInsert(node, nodes);
            for (int i = 0; i < minInsert; ++i) {
                int pred = nodes[i];
                branchingRange[i] = i;
                heuristicVal[i] = detourCost(node, pred, OldSeqVar, tw, dist);
                branching[i] = () -> OldSeqVar.getSolver().post(new Insert(OldSeqVar, pred, node));
            }
            // sort according to the detourAndSlackCost
            Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
            Procedure[] branchingSorted = new Procedure[minInsert];
            for (int i = 0; i < minInsert; ++i)
                branchingSorted[i] = branching[branchingRange[i]];
            return branchingSorted;
        };
    }

    public static Supplier<Function<Integer, Procedure[]>> maxSlack(OldSeqVar OldSeqVar, int[][] dist, IntVar[] tw) {
        int[] nodes = new int[OldSeqVar.nNode()];
        Procedure[] branching = new Procedure[OldSeqVar.nNode()];
        Integer[] heuristicVal = new Integer[OldSeqVar.nNode()];
        Integer[] branchingRange = new Integer[OldSeqVar.nNode()];
        return () -> (Integer node) -> {
            int minInsert = OldSeqVar.fillMemberPredInsert(node, nodes);
            for (int i = 0; i < minInsert; ++i) {
                int pred = nodes[i];
                branchingRange[i] = i;
                heuristicVal[i] = slackCost(node, pred, OldSeqVar, tw, dist);
                branching[i] = () -> OldSeqVar.getSolver().post(new Insert(OldSeqVar, pred, node));
            }
            // sort according to the detourAndSlackCost
            Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
            Procedure[] branchingSorted = new Procedure[minInsert];
            for (int i = 0; i < minInsert; ++i)
                branchingSorted[i] = branching[branchingRange[i]];
            return branchingSorted;
        };
    }

    public static Supplier<Function<Integer, Procedure[]>> minDetourAndMaxSlack(OldSeqVar OldSeqVar, IntVar[] tw, int[][] dist) {
        int[] nodes = new int[OldSeqVar.nNode()];
        Procedure[] branching = new Procedure[OldSeqVar.nNode()];
        Integer[] heuristicVal = new Integer[OldSeqVar.nNode()];
        Integer[] branchingRange = new Integer[OldSeqVar.nNode()];
        return () -> (Integer node) -> {
            int minInsert = OldSeqVar.fillMemberPredInsert(node, nodes);
            for (int i = 0; i < minInsert; ++i) {
                int pred = nodes[i];
                branchingRange[i] = i;
                heuristicVal[i] = detourAndSlackCost(node, pred, OldSeqVar, tw, dist);
                branching[i] = () -> OldSeqVar.getSolver().post(new Insert(OldSeqVar, pred, node));
            }
            // sort according to the detourAndSlackCost
            Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
            Procedure[] branchingSorted = new Procedure[minInsert];
            for (int i = 0; i < minInsert; ++i)
                branchingSorted[i] = branching[branchingRange[i]];
            return branchingSorted;
        };
    }

    public static Supplier<Function<Integer, Procedure[]>> mostUrgent(OldSeqVar OldSeqVar, IntVar[] tw, int[][] dist) {
        int[] nodes = new int[OldSeqVar.nNode()];
        Procedure[] branching = new Procedure[OldSeqVar.nNode()];
        Integer[] heuristicVal = new Integer[OldSeqVar.nNode()];
        Integer[] branchingRange = new Integer[OldSeqVar.nNode()];
        return () -> (Integer node) -> {
            int minInsert = OldSeqVar.fillMemberPredInsert(node, nodes);
            for (int i = 0; i < minInsert; ++i) {
                int pred = nodes[i];
                branchingRange[i] = i;
                heuristicVal[i] = tw[node].min() - tw[pred].min();
                branching[i] = () -> OldSeqVar.getSolver().post(new Insert(OldSeqVar, pred, node));
            }
            // sort according to the detourAndSlackCost
            Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
            Procedure[] branchingSorted = new Procedure[minInsert];
            for (int i = 0; i < minInsert; ++i)
                branchingSorted[i] = branching[branchingRange[i]];
            return branchingSorted;
        };
    }
    
}
