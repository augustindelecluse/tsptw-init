/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.engine.constraints;

import minicp.engine.SolverTest;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Supplier;

import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;
import static org.junit.jupiter.api.Assertions.*;

public class NegTableTest extends SolverTest {

    private int[][] randomTuples(Random rand, int arity, int nTuples, int minvalue, int maxvalue, boolean noDuplicates) {
        int[][] r = new int[nTuples][arity];
        for (int i = 0; i < nTuples; i++)
            for (int j = 0; j < arity; j++)
                r[i][j] = rand.nextInt(maxvalue - minvalue) + minvalue;
        return noDuplicates ? removeDuplicates(r) : r;
    }

    public int[][] removeDuplicates(int[][] table) {
        ArrayList<int[]> tableList = new ArrayList<>();
        boolean[] duplicate = new boolean[table.length];
        for (int i = 0; i < table.length; i++) {
            if (!duplicate[i]) {
                tableList.add(table[i]);
                for (int j = i + 1; j < table.length; j++) {
                    if (i != j & !duplicate[j]) {
                        boolean same = true;
                        for (int k = 0; k < table[i].length; k++) {
                            same &= table[i][k] == table[j][k];
                        }
                        if (same) {
                            duplicate[j] = true;
                        }
                    }
                }
            }
        }
        return tableList.toArray(new int[0][]);
    }

    public int[][] toPositive(IntVar x, IntVar y, IntVar z, int[][] negTable) {
        ArrayList<int[]> posTableList = new ArrayList<>();
        for (int i = x.min(); i <= x.max(); i++) {
            if (x.contains(i)) {
                for (int j = y.min(); j <= y.max(); j++) {
                    if (y.contains(j)) {
                        for (int k = z.min(); k <= z.max(); k++) {
                            if (z.contains(k)) {
                                boolean add = true;
                                for (int ind = 0; ind < negTable.length && add; ind++) {
                                    if (negTable[ind][0] == i && negTable[ind][1] == j && negTable[ind][2] == k) {
                                        add = false;
                                    }
                                }
                                if (add) posTableList.add(new int[]{i, j, k});
                            }
                        }
                    }
                }
            }
        }
        return posTableList.toArray(new int[0][]);
    }


    @ParameterizedTest
    @MethodSource("solver")
    public void simpleTest0(Solver cp) {
        try {
            try {
                IntVar[] x = makeIntVarArray(cp, 3, 2);
                int[][] table = new int[][]{
                        {0, 0, 0},
                        {1, 0, 0},
                        {1, 1, 0},
                        {0, 1, 0},
                        {0, 1, 1},
                        {1, 0, 1},
                        {0, 0, 1}};
                cp.post(new NegTableCT(x, table));
                //cp.post(new TableCT(x, toPositive(x[0],x[1],x[2],table)));
                assertEquals(1, x[0].min());
                assertEquals(1, x[1].min());
                assertEquals(1, x[2].min());

            } catch (InconsistencyException e) {
                fail("should not fail");
            }
        } catch (NotImplementedException e) {

        }
    }

    @ParameterizedTest
    @MethodSource("solver")
    public void simpleTest1(Solver cp) {
        try {
            try {
                IntVar[] x = makeIntVarArray(cp, 3, 2);
                int[][] table = new int[][]{{1, 1, 1}};
                cp.post(new NegTableCT(x, table));
                DFSearch dfs = makeDfs(cp, firstFail(x));
                SearchStatistics stats = dfs.solve();
                assertEquals(7, stats.numberOfSolutions());

            } catch (InconsistencyException e) {
                fail("should not fail");
            }
        } catch (NotImplementedException e) {

        }
    }

    @ParameterizedTest
    @MethodSource("solver")
    public void simpleTest2(Solver cp) {
        try {
            try {
                IntVar[] x = makeIntVarArray(cp, 3, 2);
                int[][] table = new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
                cp.post(new NegTableCT(x, table));
                DFSearch dfs = makeDfs(cp, firstFail(x));
                SearchStatistics stats = dfs.solve();
                assertEquals(7, stats.numberOfSolutions());

            } catch (InconsistencyException e) {
                fail("should not fail");
            }
        } catch (NotImplementedException e) {

        }
    }


    @ParameterizedTest
    @MethodSource("solverSupplier")
    public void randomTest(Supplier<Solver> solverSupplier) {
        Random rand = new Random(67292);

        for (int i = 0; i < 20; i++) {
            int[][] tuples1 = randomTuples(rand, 3, 50, 2, 8, true);
            int[][] tuples2 = randomTuples(rand, 3, 50, 1, 3, true);
            int[][] tuples3 = randomTuples(rand, 3, 80, 0, 6, true);
            try {
                testTable(solverSupplier, tuples1, tuples2, tuples3);
            } catch (NotImplementedException e) {

            }
        }
    }

    @ParameterizedTest
    @MethodSource("solverSupplier")
    public void randomTestWithDuplicates(Supplier<Solver> solverSupplier) {
        Random rand = new Random(67292);

        for (int i = 0; i < 20; i++) {
            int[][] tuples1 = randomTuples(rand, 3, 50, 2, 8, false);
            int[][] tuples2 = randomTuples(rand, 3, 50, 1, 3, false);
            int[][] tuples3 = randomTuples(rand, 3, 80, 0, 6, false);
            try {
                testTable(solverSupplier, tuples1, tuples2, tuples3);
            } catch (NotImplementedException e) {

            }
        }
    }

    public void testTable(Supplier<Solver> solverFactory, int[][] t1, int[][] t2, int[][] t3) {

        SearchStatistics statsDecomp;
        SearchStatistics statsAlgo;

        try {
            Solver cp = solverFactory.get();
            IntVar[] x = makeIntVarArray(cp, 5, 9);
            cp.post(allDifferent(x));
            cp.post(new TableCT(new IntVar[]{x[0], x[1], x[2]}, toPositive(x[0], x[1], x[2], t1)));
            cp.post(new TableCT(new IntVar[]{x[2], x[3], x[4]}, toPositive(x[2], x[3], x[4], t2)));
            cp.post(new TableCT(new IntVar[]{x[0], x[2], x[4]}, toPositive(x[0], x[2], x[4], t3)));
            statsDecomp = makeDfs(cp, firstFail(x)).solve();
        } catch (InconsistencyException e) {
            statsDecomp = null;
        }

        try {
            Solver cp = solverFactory.get();
            IntVar[] x = makeIntVarArray(cp, 5, 9);
            cp.post(allDifferent(x));
            cp.post(new NegTableCT(new IntVar[]{x[0], x[1], x[2]}, t1));
            cp.post(new NegTableCT(new IntVar[]{x[2], x[3], x[4]}, t2));
            cp.post(new NegTableCT(new IntVar[]{x[0], x[2], x[4]}, t3));
            statsAlgo = makeDfs(cp, firstFail(x)).solve();
        } catch (InconsistencyException e) {
            statsAlgo = null;
        }

        assertTrue((statsDecomp == null && statsAlgo == null) || (statsDecomp != null && statsAlgo != null));
        if (statsDecomp != null) {
            assertEquals(statsDecomp.numberOfSolutions(), statsAlgo.numberOfSolutions());
            assertEquals(statsDecomp.numberOfFailures(), statsAlgo.numberOfFailures());
            assertEquals(statsDecomp.numberOfNodes(), statsAlgo.numberOfNodes());
        }
    }
}
