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
import minicp.search.SearchStatistics;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;
import minicp.util.NotImplementedExceptionAssume;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.function.Supplier;

import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;
import static org.junit.jupiter.api.Assertions.*;

public class ShortTableTest extends SolverTest {


    private int[][] randomTuples(Random rand, int arity, int nTuples, int minvalue, int maxvalue) {
        int[][] r = new int[nTuples][arity];
        for (int i = 0; i < nTuples; i++)
            for (int j = 0; j < arity; j++)
                r[i][j] = rand.nextInt(maxvalue - minvalue) + minvalue;
        return r;
    }

    @ParameterizedTest
    @MethodSource("solver")
    public void simpleTest0(Solver cp) {
        try {
            IntVar[] x = makeIntVarArray(cp, 2, 1);
            int[][] table = new int[][]{{0, 0}};
            cp.post(new ShortTableCT(x, table, -1));

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {

        }
    }


    @ParameterizedTest
    @MethodSource("solver")
    public void simpleTest3(Solver cp) {

        try {
            IntVar[] x = makeIntVarArray(cp, 3, 12);
            int[][] table = new int[][]{{0, 0, 2},
                    {3, 5, 7},
                    {6, 9, 10},
                    {1, 2, 3}};
            cp.post(new ShortTableCT(x, table, 0));

            assertEquals(12, x[0].size());
            assertEquals(12, x[1].size());
            assertEquals(4, x[2].size());

            assertEquals(0, x[0].min());
            assertEquals(11, x[0].max());
            assertEquals(0, x[1].min());
            assertEquals(11, x[1].max());
            assertEquals(2, x[2].min());
            assertEquals(10, x[2].max());


        } catch (InconsistencyException e) {
            fail("should not fail");

        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("solverSupplier")
    public void randomTest(Supplier<Solver> supplier) {
        Random rand = new Random(67292);

        for (int i = 0; i < 50; i++) {
            int[][] tuples1 = randomTuples(rand, 3, 50, 2, 8);
            int[][] tuples2 = randomTuples(rand, 3, 50, 1, 7);
            int[][] tuples3 = randomTuples(rand, 3, 50, 0, 6);
            int star = 3;
            try {
                testTable(supplier, tuples1, tuples2, tuples3, star);
            } catch (NotImplementedException e) {

            }
        }
    }


    public void testTable(Supplier<Solver> solverFactory, int[][] t1, int[][] t2, int[][] t3, int star) {

        SearchStatistics statsDecomp;
        SearchStatistics statsAlgo;

        try {
            Solver cp = solverFactory.get();
            IntVar[] x = makeIntVarArray(cp, 5, 9);
            cp.post(allDifferent(x));
            cp.post(new ShortTableDecomp(new IntVar[]{x[0], x[1], x[2]}, t1, star));
            cp.post(new ShortTableDecomp(new IntVar[]{x[2], x[3], x[4]}, t2, star));
            cp.post(new ShortTableDecomp(new IntVar[]{x[0], x[2], x[4]}, t3, star));
            statsDecomp = makeDfs(cp, firstFail(x)).solve();
        } catch (InconsistencyException e) {
            statsDecomp = null;
        }

        try {
            Solver cp = solverFactory.get();
            IntVar[] x = makeIntVarArray(cp, 5, 9);
            cp.post(allDifferent(x));
            cp.post(new ShortTableCT(new IntVar[]{x[0], x[1], x[2]}, t1, star));
            cp.post(new ShortTableCT(new IntVar[]{x[2], x[3], x[4]}, t2, star));
            cp.post(new ShortTableCT(new IntVar[]{x[0], x[2], x[4]}, t3, star));
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

    /**
     * The table should accept all values of x0 and x1. However, it prunes off
     * some values of x1.
     */
    @ParameterizedTest
    @MethodSource("solver")
    public void minicpReplayShortTableCtIsStrongerThanAc(Solver cp) {

        try {
            final int star = 2147483647;

            // This table should accept all values.
            final int[][] table = {
                    {2147483647, 2147483647}
            };

            final IntVar x0 = makeIntVar(cp, new HashSet<>(Arrays.asList(0)));
            final IntVar x1 = makeIntVar(cp, new HashSet<>(Arrays.asList(-1, 2)));


            cp.post(new ShortTableCT(new IntVar[]{x0, x1}, table, star));

            assertEquals(1, x0.size());
            assertEquals(2, x1.size());

        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("solver")
    public void issue13(Solver cp) {

        try {
            final int star = -2147483648;

            // This table should accept all values.
            final int[][] table = {{0, 0}};

            final IntVar x0 = makeIntVar(cp, new HashSet<>(Arrays.asList(-5)));
            final IntVar x1 = makeIntVar(cp, new HashSet<>(Arrays.asList(-5)));

            cp.post(new ShortTableCT(new IntVar[]{x0, x1}, table, star));

        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("solver")
    public void issue14(Solver cp) {

        try {

            final int arity = 2;
            final int star = 2147483647;
            final int[][] table = {
                    {2147483647, 2147483647} // means *, *
            };

            IntVar x0 = makeIntVar(cp, new HashSet<>(Arrays.asList(0)));
            IntVar x1 = makeIntVar(cp, new HashSet<>(Arrays.asList(-1, 2)));

            IntVar y = makeIntVar(cp, new HashSet<>(Arrays.asList(0, 1)));
            IntVar z = makeIntVar(cp, new HashSet<>(Arrays.asList(3)));

            IntVar[] data = new IntVar[]{x0, x1};

            cp.post(new ShortTableCT(data, table, star));
            assertEquals(-1, data[1].min());

        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }
}
