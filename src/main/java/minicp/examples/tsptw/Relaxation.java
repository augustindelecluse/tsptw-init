package minicp.examples.tsptw;

import minicp.engine.constraints.sequence.Insert;
import minicp.engine.core.IntVar;
import minicp.engine.core.OldSeqVar;
import minicp.engine.core.SeqVar;
import minicp.util.exception.InconsistencyException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

public class Relaxation {

    private void relaxShaw() {

    }

    private void relaxSuccessiveVisited() {

    }

    private void relaxSuccessiveEarliest() {

    }

    /**
     * Relax the nodes having the biggest slack
     * @param seqVar
     * @param bestOrdering
     */
    public static void relaxMostSlack(OldSeqVar seqVar, int nRelax, int[] bestOrdering, int[] twStart, int[] twEnd, int[][] dist) {
        // computes the slack
        int[] slack = new int[seqVar.nNode()];// TODO attribute or whatever, prevent garbage collection in any case
        int[] arrival = new int[seqVar.nNode()]; // when we arrive at a node (possibly before the twStart)
        int[] earliestDeparture = new int[seqVar.nNode()]; // when we depart from the node (possibly after the twSTart)
        int[] latestDeparture = new int[seqVar.nNode()]; // when we must depart from the node at the latest (possibly before the twEnd)
        Arrays.fill(slack, Integer.MAX_VALUE);
        int pred = 0;
        int time = 0;
        int nVisited = 0;
        int nNode = seqVar.nNode();
        // forward pass -> set arrival and earliest departure
        for (int i = 1 ; i < nNode; ++i) {
            int current = bestOrdering[i];
            if (current == 0) {
                break;
            } else {
                nVisited += 1;
            }
            time += dist[pred][current];
            arrival[current] = time;
            time = Math.max(time, twStart[current]);
            earliestDeparture[current] = time;
            pred = current;
        }
        // backward pass -> set latest departure
        int succ = bestOrdering[nVisited-1];
        int current = bestOrdering[nVisited-2];
        time = twEnd[succ];
        latestDeparture[succ] = time;
        for (int i = nVisited - 2 ; i >= 0 ; --i) {
            time -= dist[current][succ]; // departure from the successor
            time = Math.min(time, twEnd[current]);
            latestDeparture[current] = time;

            succ = current;
            if (i > 0)
                current = bestOrdering[i-1];
        }
        int[] score = new int[nNode];
        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingInt(i -> score[i]));
        for (int i = 1 ; i < nVisited ; ++i) {
            current = bestOrdering[i];
            score[current] = -(2 * Math.max(0, earliestDeparture[current] - arrival[current]) +
                    (latestDeparture[current] - arrival[current]));
            pq.add(current);
        }
        HashSet<Integer> relaxed = new HashSet<>();
        while (nRelax > 0 && !pq.isEmpty()) {
            int toRelax = pq.poll();
            relaxed.add(toRelax);
            nRelax -= 1;
        }
        int prev = seqVar.begin();
        for (int i = 1 ; i < nVisited - 1 ; ++i) {
            current = bestOrdering[i];
            if (!relaxed.contains(current)) {
                seqVar.insert(prev, current); // the vehicle goes through this node
                prev = current; // only updated when a non-relaxed node is met, to complete the partial route
            }
        }
        try {
            seqVar.getSolver().fixPoint();
        } catch (InconsistencyException e) {
            System.err.println("failed to relax");
            throw e;
        }
    }


    /**
     * Relax successive nodes between the ones with the biggest waiting time
     * @param seqVar
     * @param bestOrdering
     */
    private void relaxBetweenTwoBiggestSlacks(SeqVar seqVar, int[] bestOrdering, int[] twStart, int[] twEnd, int[][] dist) {
        // computes the slack
    }

    private void relaxTimeOverlap() {

    }

}
