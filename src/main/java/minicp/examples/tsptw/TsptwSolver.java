package minicp.examples.tsptw;


import minicp.cp.Factory;
import minicp.engine.constraints.sequence.*;
import minicp.engine.core.*;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static minicp.cp.BranchingScheme.EMPTY;
import static minicp.cp.Factory.*;
import static minicp.util.exception.InconsistencyException.INCONSISTENCY;


public class TsptwSolver {

    // TODO distance matrix using all pair shortest path for satisfiability

    private int verbosity = 0;

    private final TsptwInstance instance;
    private final int           timeout;
    private List<BiConsumer<int[],Integer>> observers = new LinkedList<>();

    private int nNodes;
    private int nNodesWithDepot;
    private int begin;
    private int end;
    private int[][] distances;
    private int[] twStart;
    private int[] twEnd;
    private int maxTwStart; // TODO set the values ;D
    private int maxTwEnd;
    private int maxDistance;

    private Solver cp;
    private IntVar[] time; // time window of every node
    private OldSeqVar route; // route taken in the TSP
    private IntVar nVisitedNodes; // TODO enhance number of visited nodes objective

    // used for finding a satisfiable solution. Stores the nodes belonging to the current solution
    private Set<Integer> memberInSolution = new HashSet<>();

    private int bestNVisited; // best number of visited nodes

    private int[] nodes; // used for fill operations on nodes in the branching
    private int[] insertion; // used for fill operations on insertions in the branching
    private Procedure[] branching; // contain the branching procedures
    private Integer[] heuristicVal; // heuristic values
    private Integer[] branchingRange; // index used to sort using the heuristic values
    private Random random; // used to randomize the branching and the relaxation
    private long seed; // seed used by random

    private int[] visitOrder; // store the visit order within a sequence

    private int[] currentSolOrder; // current solution ordering
    private int[] bestSolOrder; // best solution ordering
    private int[] notYetVisited; // nodes that are not yet visited during the satisfiability process
    private int nNotYetVisited;
    private TsptwResult bestSol; // value for the best solution
    private int[] relaxedNodes; // set of relaxed nodes
    private Set<Integer> relaxed; // set of relaxed nodes
    private final boolean solProvided; // true if an initial solution was provided
    private long init; // time at which the solver has started, in millis
    private int[][] mostSimilar;  // mostSimilar[i][0] = node that is the most similar to node i

    private ArrayList<int[]> solRegistered;
    private Set<Integer> toRelax = new HashSet<>();
    private int toRelaxFromShaw = -1;

    public void addObserver(BiConsumer<int[],Integer> observer) {
        observers.add(observer);
    }

    private void notifySolution(int [] solution, int objective) {
        for (BiConsumer<int[],Integer> observer: observers) {
            observer.accept(solution, objective);
        }
    }

    /**
     * initialize a TSP with time window solver
     * @param instance instance to solve
     * @param timeout timeout for the solving [s]
     */
    public TsptwSolver(final TsptwInstance instance, final int timeout) {
        this.instance = instance;
        this.timeout  = timeout * 1000; // convert to millis
        //boolean respect = instance.respectTriangularInequality();
        //int threshold = TransitionTimes.thresholdRespectTriangularInequality(instance.distances);
        //System.out.println("respect = " + respect + " threshold = " + threshold);
        nNodes = instance.nbNodes;
        nNodesWithDepot = nNodes + 1; // node 0: begin node, last node: end depot
        nodes = new int[nNodesWithDepot];
        begin = 0;
        end = nNodes;
        visitOrder = new int[nNodes];
        heuristicVal = new Integer[nNodes];
        branchingRange = new Integer[nNodes];
        branching = new Procedure[nNodes];
        // transition from node to node
        distances = new int[nNodesWithDepot][nNodesWithDepot];
        twStart = new int[nNodesWithDepot];
        twEnd = new int[nNodesWithDepot];
        for (int i = 0 ; i < nNodes ; ++i) {
            System.arraycopy(instance.distances[i], 0, distances[i], 0, nNodes);
            distances[i][end] = distances[i][begin]; // getting to the end node is the same as getting to the beginning node
            twStart[i] = instance.timeWindows[i].getEarliest();
            twEnd[i] = instance.timeWindows[i].getLatest();
            maxTwStart = Math.max(twStart[i], maxTwStart);
            maxTwEnd = Math.max(twEnd[i], maxTwEnd);
            maxDistance = Math.max(Arrays.stream(distances[i]).max().getAsInt(), maxDistance);
        }
        // time window for end node
        twStart[end] = instance.timeWindows[begin].getEarliest();
        twEnd[end] = instance.timeWindows[begin].getLatest();
        seed = 42;
        random = new Random(seed);
        relaxed = new HashSet<>();
        relaxedNodes = new int[nNodes];
        bestSolOrder = new int[nNodesWithDepot];
        currentSolOrder = new int[nNodesWithDepot];
        notYetVisited = new int[nNodes];
        // solution
        bestSol = new TsptwResult(Integer.MAX_VALUE);
        solProvided = false;
        bestNVisited = 0;
        insertion = new int[nNodes];
        init = System.currentTimeMillis();
    }

    public void setSeed(long seed) {
        this.seed = seed;
        random.setSeed(seed);
    }

    public long getSeed() {
        return seed;
    }

    /**
     * Gives a first feasible solution in the available time
     * @return first feasible solution in the available time
     */
    public TsptwResult satisfy() {
        solveSatisfy();
        return bestSol;
    }

    /**
     * Tries to visit as many nodes as possible using a greedy search.
     * Stops at the first failure encountered
     * @return first feasible solution in the available time
     */
    public TsptwResult satisfy_greedy() {
        cp = makeSolver();
        initCpVars();
        postSatisfactionConstraint();
        DFSearch search = makeDfs(cp, this::maxRegretBranching);
        Procedure solutionNotifier = () -> {
            int nVisit = this.nVisitedNodes.max();
            if (nVisit > bestNVisited) {
                updateCurrentOrder();
                nNotYetVisited = route.fillExcluded(notYetVisited);
                if (verbosity > 0) {
                    String excludedString = "{" + Arrays.stream(notYetVisited, 0, nNotYetVisited)
                            .mapToObj(Integer::toString).collect(Collectors.joining(", ")) + "}";
                    System.out.println("#visit: " + (nVisit-1) + "/" + (nNodes) + " (closed sequence = " + (route.nExcluded() == 0) +
                            "). ordering: 0 " + route.ordering(false, " ") + " excluded = " + excludedString);
                }
                updateSatisfiabilitySolution(currentSolOrder, nVisit);
                // stores within the set all nodes that have been visited
                memberInSolution.clear();
                for (int i = 1 ; i < nVisit ; ++i) {
                    memberInSolution.add(currentSolOrder[i]);
                }
                if (bestNVisited == nNodesWithDepot) {
                    notifySolution(currentSolOrder, cost());
                } else {
                    notifySolution(currentSolOrder, Integer.MAX_VALUE);
                }
            }
        };
        search.onSolution(solutionNotifier);

        SearchStatistics stats = search.solve(s -> s.numberOfSolutions() >= 1); // find an initial number of possible nodes
        return bestSol;
    }

    /**
     * Computes the cost manually
     * Requires {@link TsptwSolver#currentSolOrder to be set with all the nodes}
     */
    private int cost() {
        int cost = 0;
        for (int i = 1; i < currentSolOrder.length ; ++i) {
            cost += distances[currentSolOrder[i-1]][currentSolOrder[i]];
        }
        return cost;
    }

    /**
     * Finds a first feasible solution
     */
    private void solveSatisfy() {
        if (solProvided) { // an initial solution is already given
            return;
        }

        cp = makeSolver();
        initCpVars();
        postSatisfactionConstraint();

        DFSearch search = makeDfs(cp, this::maxRegretBranching);
        Procedure solutionNotifier = () -> {
            int nVisit = this.nVisitedNodes.max();
            if (nVisit > bestNVisited) {
                updateCurrentOrder();
                nNotYetVisited = route.fillExcluded(notYetVisited);
                if (verbosity > 0) {
                    String excludedString = "{" + Arrays.stream(notYetVisited, 0, nNotYetVisited)
                            .mapToObj(Integer::toString).collect(Collectors.joining(", ")) + "}";
                    System.out.println("#visit: " + (nVisit-1) + "/" + (nNodes) + " (closed sequence = " + (route.nExcluded() == 0) +
                            "). ordering: 0 " + route.ordering(false, " ") + " excluded = " + excludedString);
                }
                updateSatisfiabilitySolution(currentSolOrder, nVisit);
                // stores within the set all nodes that have been visited
                memberInSolution.clear();
                for (int i = 1 ; i < nVisit ; ++i) {
                    memberInSolution.add(currentSolOrder[i]);
                }
                if (bestNVisited == nNodesWithDepot) {
                    notifySolution(currentSolOrder, cost());
                }
            }
        };
        search.onSolution(solutionNotifier);

        SearchStatistics stats;
        stats = search.solve(s -> s.numberOfSolutions() >= 1); // find an initial number of possible nodes
        boolean foundFirstSol = bestNVisited == nNodesWithDepot;
        if (!foundFirstSol) {
            if (verbosity > 1)
                System.out.println("switching branching");
            search = makeDfs(cp, this::branchOnOneInsertionVar);
            search.onSolution(solutionNotifier);
            Objective objective = cp.maximize(nVisitedNodes);

            boolean running = System.currentTimeMillis() - init < timeout;
            // VLNS parameters
            int minNeighborhoodStart = 5;
            int range = 5;
            int numIters = 3;
            int nSearchNodeLimit = 1000;
            int maxRange = Math.max(nNodes / 2 - range, nNodes - range);

            for (int minNeighborhood = minNeighborhoodStart; minNeighborhood <= maxRange && running; ++minNeighborhood) {
                if (minNeighborhood == maxRange)
                    minNeighborhood = minNeighborhoodStart; // reset of the neighborhood
                for (int offsetNeighborhood = 0; offsetNeighborhood < range && running; ++offsetNeighborhood) {
                    int nRelax = minNeighborhood + offsetNeighborhood;
                    if (verbosity > 1)
                        System.out.println("relaxing " + nRelax + " nodes / " + nNodes);
                    for (int i = 0; i < numIters && running; ++i) {
                        if (running) {
                            stats = search.optimizeSubjectTo(objective,
                                    searchStatistics -> (
                                            System.currentTimeMillis() - init >= timeout
                                                    || searchStatistics.numberOfNodes() > nSearchNodeLimit),
                                    () -> relaxShaw(nRelax, notYetVisited[random.nextInt(nNotYetVisited)]));
                            if (stats.numberOfSolutions() >= 1 && verbosity > 1) {
                                System.out.println("improved with shaw relaxation");
                            }
                            if (stats.numberOfSolutions() >= 1) {
                                // reset all parameters of the lns
                                minNeighborhood = maxRange-1;
                                offsetNeighborhood = range;
                                i = numIters;
                            }
                        }

                        foundFirstSol = bestNVisited == nNodesWithDepot;
                        running = !foundFirstSol && System.currentTimeMillis() - init < timeout;

                    }
                }
            }
        }
        if (foundFirstSol) {
            if (verbosity > 0)
                System.out.println("found first solution");
        }
    }

    /* ================================ relaxation operators =======================================================  */


    /**
     * Relax nodes similar to another one
     *
     * @param nRelax number of nodes to relax
     * @param initialNode node to which the comparison is applied. Relax nodes that are similar to this one
     */
    private void relaxShaw(int nRelax, int initialNode) {
        if (nRelax >= nNodes || nRelax >= memberInSolution.size())
            return;
        toRelaxFromShaw = initialNode;
        int p = 6; // diversification factor
        if (mostSimilar == null) {
            // initializes the similarity matrix between the nodes
            int[][] similarity = new int[nNodesWithDepot][nNodesWithDepot];
            for (int i = 0 ; i < nNodesWithDepot ; ++i) {
                for (int j = 0 ; j < nNodesWithDepot ; ++j) {
                    similarity[i][j] = similarity(i, j);
                }
            }
            mostSimilar = new int[nNodesWithDepot][];
            for (int i = 0 ; i < nNodesWithDepot ; ++i) {
                int finalI = i;
                mostSimilar[i] = IntStream.range(0, nNodesWithDepot)
                        .boxed().sorted(Comparator.comparingInt(x -> similarity[finalI][x]))
                        .mapToInt(ele -> ele).toArray();
            }
        }
        toRelax.clear(); // all nodes that must be relaxed
        toRelax.add(initialNode);
        nodes[0] = initialNode;
        int nSelected = 1;
        int nIter = 0;
        while (nSelected < nRelax && nIter < 200) {
            // select randomly one of the nodes in the set to relax
            int request = nodes[random.nextInt(nSelected)];
            // get the similar nodes that are member not yet selected
            // TODO prevent object creation
            int[] L = Arrays.stream(mostSimilar[request])
                    .filter(i -> !toRelax.contains(i) && memberInSolution.contains(i) && i != begin && i != end)
                    .toArray();
            if (L.length > 0) {
                // select a node in the beginning of the array
                int index = (int) (Math.pow(random.nextDouble(1), p) * (L.length - 1));
                toRelax.add(L[index]);
                nodes[nSelected] = L[index];
                nSelected += 1;
            }
            nIter++; // don't want to loop indefinitely
        }
        int pred = begin;
        int node = 0;
        for (int i = 1; i < bestNVisited - 1; ++i) {
            node = currentSolOrder[i];
            if (!toRelax.contains(node)) {
                route.insert(pred, node);
                pred = node;
            }
        }
        cp.fixPoint();
    }

    /**
     * Computes the similarity between 2 nodes
     *
     * @return similarity between nodes: the lower the value, the more similar are the nodes
     */
    private int similarity(int from, int to) {
        int PHI = 9;
        int XI = 3;
        return (int) (1000 * (PHI * ((double) distances[from][to] / maxDistance)
                + XI * (Math.abs((double) (twStart[from] - twStart[to]) / maxTwStart)
                      + Math.abs((double) (twEnd[from] - twEnd[to]) / maxTwEnd))));
    }


    /**
     * relax nRelax nodes randomly from the current solution
     * @param nRelax number of nodes to relax
     * @param nVisit number of visited nodes in the solution (including end depot)
     */
    private void relaxRandomly(int nRelax, int nVisit) {
        if (nRelax >= nNodes)
            return;
        toRelaxFromShaw = -1;
        Arrays.setAll(relaxedNodes, i-> i);
        int relaxEnd = 0;
        int toRelax;
        int cRelaxed;
        while (relaxEnd < nRelax && relaxEnd < nVisit) { // relax as many nodes as asked
            toRelax = relaxEnd + random.nextInt(nVisit - relaxEnd);
            cRelaxed = relaxedNodes[toRelax];
            relaxedNodes[toRelax] = relaxedNodes[relaxEnd];
            relaxedNodes[relaxEnd] = cRelaxed;
            ++relaxEnd;
        }
        // relaxedNodes[0..relaxEnd-1] contains the relaxed nodes
        relaxed.clear();
        for (int i = 0 ; i < relaxEnd; ++i)
            relaxed.add(relaxedNodes[i]);
        // relaxedNodes[relaxEnd..] are set to the previous value
        int prev = begin;
        //for (int current: bestSolOrder) {
        for (int i = 1 ; i < nVisit - 1 ; ++i) {
            int current = currentSolOrder[i];
            if (!relaxed.contains(current)) {
                try {
                    cp.post(new Insert(route, prev, current), false); // the vehicle goes through this node
                } catch (InconsistencyException e) {
                    throw e;
                }
                prev = current; // only updated when a non-relaxed node is met, to complete the partial route
            }
        }
        //System.out.println(route.ordering(false, " "));
    }

    /**
     * relax nRelax nodes randomly from the current solution
     * @param nRelax number of nodes to relax
     */
    private void relaxRandomly(int nRelax) {
        if (nRelax >= nNodes)
            return;
        toRelaxFromShaw = -1;
        Arrays.setAll(relaxedNodes, i-> i);
        int relaxEnd = 0;
        int toRelax;
        int cRelaxed;
        while (relaxEnd < nRelax && relaxEnd < nNodes) { // relax as many nodes as asked
            toRelax = relaxEnd + random.nextInt(nNodes - relaxEnd);
            cRelaxed = relaxedNodes[toRelax];
            relaxedNodes[toRelax] = relaxedNodes[relaxEnd];
            relaxedNodes[relaxEnd] = cRelaxed;
            ++relaxEnd;
        }
        // relaxedNodes[0..relaxEnd-1] contains the relaxed nodes
        relaxed.clear();
        for (int i = 0 ; i < relaxEnd; ++i)
            relaxed.add(relaxedNodes[i]);
        // relaxedNodes[relaxEnd..] are set to the previous value
        int prev = begin;
        for (int current: bestSolOrder) {
            if (!relaxed.contains(current)) {
                cp.post(new Insert(route, prev, current), false); // the vehicle goes through this node
                prev = current; // only updated when a non-relaxed node is met, to complete the partial route
            }
        }
        // the objective that must be respected
        // cost.removeBelow(bestSol.cost);
    }


    /**
     * relax consecutive nodes in the sequence while
     * @param nRelax number of nodes to relax
     * @param nVisited number of visited nodes in the current solution
     */
    private void relaxSuccessiveNodes(int nRelax, int nVisited) {
        if (nRelax >= nNodes)
            return;
        toRelaxFromShaw = -1;
        // look in the current solution array and select randomly the first node to relax
        if (nRelax >= nVisited - 1) // all nodes must be relaxed
            return;
        int firstNodeIdx = 1 + random.nextInt(nVisited - 1 - nRelax);

        boolean verbose = verbosity > 3;
        if (verbose) {
            System.out.println("given ");
            for (int i = 0 ; i < nVisited ; ++i) {
                System.out.print(currentSolOrder[i] + " ");
            }
            System.out.println("\nI plan to relax to ");
            for (int i = 0 ; i < firstNodeIdx ; ++i) {
                System.out.print(currentSolOrder[i] + " ");
            }
            System.out.print("----- ");
            for (int i = firstNodeIdx + nRelax ; i < nVisited ; ++i) {
                System.out.print(currentSolOrder[i] + " ");
            }
            System.out.println("\n");
        }

        int pred = begin;
        int current;
        for (int i = 1 ; i < nVisited - 1 ; ++i) {
            current = currentSolOrder[i];
            if (i == firstNodeIdx) { // in the relaxed nodes set
                for (; i < firstNodeIdx + nRelax ; ++i) { // for all relaxed nodes
                    current = currentSolOrder[i];
                    for (int j = 0; j < firstNodeIdx - 1; ++j) { // for all nodes before the set of relaxed node except the one before
                        int invalidPred = currentSolOrder[j];
                        route.removePredInsert(invalidPred, current); // remove the insertion
                    }
                    for (int j = firstNodeIdx + nRelax; j < nVisited; ++j) { // for all nodes after the set of relaxed node
                        int invalidPred = currentSolOrder[j];
                        route.removePredInsert(invalidPred, current); // remove the insertion
                    }
                    if (route.nPredInsert(current) != nRelax) {
                        int a = 0;
                    }
                }
                // end of loop, i = firstNodeIdx + nRelax
                i = firstNodeIdx + nRelax;
                current = currentSolOrder[i];
            }
            try {
                cp.post(new Insert(route, pred, current), false);
            } catch (Exception e) {
                int a = 0;
                throw e;
            }
            pred = current;
        }
        // close the sequence
        //System.out.println(route.ordering(false, " "));
    }

    /**
     * relax a number of consecutive nodes in the sequence
     *
     * the sequence will be split in three parts:
     *  1. before the set of relaxed nodes (A)
     *  2. set of relaxed nodes (B)
     *  3. after the set of relaxed nodes (C)
     * the relaxed nodes in (B) can only be inserted between (A) and (C)
     *
     * @param nRelax number of nodes to relax
     */
    private void relaxSuccessiveNodes(int nRelax) {
        relaxSuccessiveNodes(nRelax, nNodesWithDepot);
    }

    /* ================================ branching ================================================================  */

    /**
     * branching procedure to solve the TSPTW instance
     *
     * variable selection: select the node with the maximum regret
     *  the regret is computed as the difference between the best insertiError when running data/TSPTW/instances/SolomonPotvinBengio/rc_204.1.txt with seed = 6512865725848787841on and the second best one
     *  this is more expensive than {@link TsptwSolver#branchForSatisfiability()}
     * value selection: branch on every scheduled insertion in increasing heuristic value
     *
     * @return branching to solve the TSPTW instance
     */
    public Procedure[] maxRegretBranching() {
        if (route.isFixed())
            return EMPTY;
        // selects the node having the maximum regret
        int bestNode = -1;
        int bestRegret = Integer.MIN_VALUE;
        int size = route.fillPossible(nodes);
        for (int i = 0 ; i < size ; ++i) {
            int nInsert = route.fillMemberPredInsert(nodes[i], insertion);
            int minCost1 = Integer.MAX_VALUE;
            int minCost2 = Integer.MAX_VALUE; // minCost1 < minCost2
            for (int j = 0 ; j < nInsert ; ++j) {
                // TODO incorpore detour cost in heuristic, use different alpha and beta
                int cost = heuristic(nodes[i], insertion[j]);
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
            if (regret > bestRegret) {
                bestNode = nodes[i];
                bestRegret = regret;
            }
        }
        int branchingNode = bestNode;
        // branch on every member insertion
        int minInsert = route.fillMemberPredInsert(branchingNode, nodes);
        for (int i = 0 ; i < minInsert; ++i) {
            int pred = nodes[i];
            branchingRange[i] = i;
            heuristicVal[i] =  satisfiabilityHeuristic(branchingNode, pred);
            branching[i] = () -> cp.post(new Insert(route, pred, branchingNode));
        }
        // sort according to the heuristic
        Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
        Procedure[] branchingSorted = new Procedure[minInsert];
        for (int i = 0 ; i < minInsert ; ++i)
            branchingSorted[i] = branching[branchingRange[i]];
        // exclude all possible nodes, binding the sequence and giving a solution
        //branchingSorted[minInsert] = () -> {cp.post(new ExcludeAllPossible(route));};
        return branchingSorted;
    }

    /**
     * branching procedure to find a first feasible solution
     *
     * each ordering is a solution
     *
     * variable selection: select the node with the least scheduled insertions
     * value selection: branch on every scheduled insertion in increasing heuristic value
     *
     * @return branching to solve the TSPTW instance
     */
    public Procedure[] branchForSatisfiability() {
        if (route.isFixed()) // all nodes have been sequenced
            return EMPTY;
        int branchingNode;
        if (route.isPossible(toRelaxFromShaw)) {
            // first try the insertion from this node
            branchingNode = toRelaxFromShaw;
        } else {
            // select the node with the least insertions points
            int size = route.fillPossible(nodes);
            int bestFound = Integer.MAX_VALUE;
            int nFound = 0;
            for (int i = 0; i < size; ++i) {
                int nInsert = route.nPredInsert(nodes[i]); // best so far
                //int nInsert = route.nPossiblePredInsert(nodes[i]) * 2 + route.nMemberPredInsert(nodes[i]); // could it be worth?
                if (nInsert < bestFound && nInsert > 0) {
                    bestFound = nInsert;
                    insertion[0] = nodes[i];
                    nFound = 1;
                } else if (nInsert == bestFound) {
                    insertion[nFound++] = nodes[i];
                }
            }
            if (nFound == 0) {
                return EMPTY;
            }
            branchingNode = insertion[random.nextInt(nFound)]; // randomly select the node amongst the nodes that have been selected
        }

        // branch on every scheduled insertion
        int minInsert = route.fillMemberPredInsert(branchingNode, nodes);
        if (minInsert == 0) {
            return new Procedure[]{() -> cp.post(new Exclude(route, branchingNode))};
        }
        for (int i = 0 ; i < minInsert; ++i) {
            int pred = nodes[i];
            branchingRange[i] = i;
            heuristicVal[i] =  satisfiabilityHeuristic(branchingNode, pred);
            branching[i] = () -> cp.post(new Insert(route, pred, branchingNode));
        }
        // sort according to the heuristic
        Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
        Procedure[] branchingSorted = new Procedure[minInsert];
        for (int i = 0 ; i < minInsert ; ++i)
            branchingSorted[i] = branching[branchingRange[i]];
        // exclude all possible nodes, binding the sequence and giving a solution
        //branchingSorted[minInsert] = () -> {cp.post(new ExcludeAllPossible(route));};
        return branchingSorted;
    }

    /**
     * branching procedure to solve the TSPTW instance
     *
     * variable selection: select the node with the least scheduled insertions
     * value selection: branch on every scheduled insertion in increasing heuristic value
     *
     * @return branching to solve the TSPTW instance
     */
    public Procedure[] branchOnOneInsertionVar() {
        if (route.isFixed()) // all nodes have been sequenced
            return EMPTY;

        // select the node with the least insertions points
        int size = route.fillPossible(nodes);
        int minInsert = Integer.MAX_VALUE;
        int nFound = 0;
        for (int i = 0 ; i < size; ++i) {
            int nInsert = route.nMemberPredInsert(nodes[i]);
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

        int branchingNode = insertion[random.nextInt(nFound)]; // randomly select the node amongst the nodes that have been selected

        // branch on every scheduled insertion
        route.fillMemberPredInsert(branchingNode, nodes);
        for (int i = 0 ; i < minInsert; ++i) {
            int pred = nodes[i];
            branchingRange[i] = i;
            heuristicVal[i] =  heuristic(branchingNode, pred);
            branching[i] = () -> cp.post(new Insert(route, pred, branchingNode));
        }
        // sort according to the heuristic
        Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
        Procedure[] branchingSorted = new Procedure[minInsert];
        for (int i = 0 ; i < minInsert ; ++i)
            branchingSorted[i] = branching[branchingRange[i]];
        return branchingSorted;
    }

    /* ================================ heuristic ================================================================  */

    /**
     * heuristic value when inserting a node at a given position
     * @param node node that will be inserted
     * @param pred predecessor after which the node will be inserted
     * @return heuristic value for the insertion: the lower the better
     */
    public int heuristic(int node, int pred) {
        int succ = route.nextMember(pred);
        int slack = time[succ].max() - (time[pred].min() + distances[pred][node] + distances[node][succ]);
        int objChange = distances[node][succ] + distances[pred][node] - distances[pred][succ];
        return objChange - slack;
    }

    /**
     * minus slack when inserting a node at a given position
     * @param node node that will be inserted
     * @param pred predecessor after which the node will be inserted
     * @return heuristic value for the insertion: the lower the better
     */
    public int satisfiabilityHeuristic(int node, int pred) {
        int succ = route.nextMember(pred);
        return - (time[succ].max() - (time[pred].min() + distances[pred][node] + distances[node][succ]));
    }

    /* ================================ model ======================================================================  */

    private void initCpVars() {
        // sequence
        route = Factory.makeSequenceVar(cp, nNodesWithDepot, begin, end);

        // time window
        time = new IntVar[nNodesWithDepot];
        for (int i = 0 ; i < nNodes ; ++i) {
            time[i] = makeIntVar(cp, twStart[i], twEnd[i], true);
        }
        time[end] = makeIntVar(cp, twStart[begin], twEnd[begin], true);

        // visited nodes cost
        nVisitedNodes = makeIntVar(cp, 2, nNodesWithDepot);
    }

    /**
     * post constraints to visit as much node as possible
     * nodes can be excluded from the problem
     */
    private void postSatisfactionConstraint() {
        int[] servingDuration = new int[nNodesWithDepot];
        // respect the transitions between nodes
        cp.post(new TransitionTimes(route, time, distances, servingDuration));
        // cost is the number of visited nodes
        cp.post(new NMember(route, nVisitedNodes));
    }


    /* ================================ solution update ============================================================  */

    /**
     * update the values of the current order of the sequence into the order array
     * does not store the beginning nor the end depot
     */
    private void updateCurrentOrder() {
        int current = route.nextMember(begin);
        int i = 1;
        while (current != end) {
            currentSolOrder[i] = current;
            current = route.nextMember(current);
            ++i;
        }
    }

    /**
     * update the most promising satisfiable solution found with nNodes in the sequences
     * @param solFound best solution found. Include the beginning and ending nodes
     * @param nNodes number of nodes (beginning and ending nodes included) in the solFound
     */
    private void updateSatisfiabilitySolution(int[] solFound, int nNodes) {
        if (nNodes > bestNVisited) {
            bestSolOrder = solFound;
            bestNVisited = nNodes;
        }
    }

    public int getVerbosity() {
        return verbosity;
    }

    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }


}

class TsptwResult {
    boolean isOptimum = false;
    int cost = Integer.MAX_VALUE;

    public TsptwResult(int cost) {
        this.cost = cost;
    }
}

