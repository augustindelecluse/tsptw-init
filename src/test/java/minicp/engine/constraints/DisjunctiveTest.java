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
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;
import minicp.util.NotImplementedExceptionAssume;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DisjunctiveTest extends SolverTest {

    private static void decomposeDisjunctive(IntVar[] start, int[] duration) {
        Solver cp = start[0].getSolver();
        for (int i = 0; i < start.length; i++) {
            IntVar end_i = plus(start[i], duration[i]);
            for (int j = i + 1; j < start.length; j++) {
                // i before j or j before i

                IntVar end_j = plus(start[j], duration[j]);
                BoolVar iBeforej = makeBoolVar(cp);
                BoolVar jBeforei = makeBoolVar(cp);

                cp.post(new IsLessOrEqualVar(iBeforej, end_i, start[j]));
                cp.post(new IsLessOrEqualVar(jBeforei, end_j, start[i]));
                cp.post(new NotEqual(iBeforej, jBeforei), false);

            }
        }

    }

    @ParameterizedTest
    @MethodSource("solver")
    public void testAllDiffDisjunctive(Solver cp) {

        try {

            IntVar[] s = makeIntVarArray(cp, 5, 5);
            int[] d = new int[5];
            Arrays.fill(d, 1);

            cp.post(new Disjunctive(s, d));

            SearchStatistics stats = makeDfs(cp, firstFail(s)).solve();
            assertEquals(120, stats.numberOfSolutions(), "disjunctive alldiff expect makeIntVarArray permutations");

        } catch (InconsistencyException e) {
            assert (false);
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }

    }

    @ParameterizedTest
    @MethodSource("solver")
    public void testNotRemovingSolutions(Solver cp) {

        try {

            IntVar[] s = makeIntVarArray(cp, 4, 20);
            int[] d = new int[]{5, 4, 6, 7};
            DFSearch dfs = makeDfs(cp, firstFail(s));


            cp.getStateManager().saveState();

            cp.post(new Disjunctive(s, d));

            SearchStatistics stat1 = dfs.solve();

            cp.getStateManager().restoreState();

            decomposeDisjunctive(s, d);

            SearchStatistics stat2 = dfs.solve();


            assertEquals(stat1.numberOfSolutions(), stat2.numberOfSolutions());


        } catch (InconsistencyException e) {
            assert (false);
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("solver")
    public void testBinaryDecomposition(Solver cp) {
        IntVar s1 = makeIntVar(cp, 0, 10);
        int d1 = 10;
        IntVar s2 = makeIntVar(cp, 6, 15);
        int d2 = 6;

        try {
            cp.post(new Disjunctive(new IntVar[]{s1, s2}, new int[]{d1, d2}));
            assertEquals(10, s2.min());
        } catch (InconsistencyException e) {
            assert (false);
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    @ParameterizedTest
    @MethodSource("solver")
    public void testOverloadChecker(Solver cp) {
        IntVar sA = makeIntVar(cp, 0, 9);
        int d1 = 5;
        IntVar sB = makeIntVar(cp, 1, 10);
        int d2 = 5;
        IntVar sC = makeIntVar(cp, 3, 7);
        int d3 = 6;

        try {
            cp.post(new Disjunctive(new IntVar[]{sA, sB, sC}, new int[]{d1, d2, d3}));
            assert (false);
        } catch (InconsistencyException e) {
            assert (true);
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    @ParameterizedTest
    @MethodSource("solver")
    public void testDetectablePrecedence(Solver cp) {
        IntVar sA = makeIntVar(cp, 0, 9);
        int d1 = 5;
        IntVar sB = makeIntVar(cp, 1, 10);
        int d2 = 5;
        IntVar sC = makeIntVar(cp, 8, 15);
        int d3 = 3;

        try {
            cp.post(new Disjunctive(new IntVar[]{sA, sB, sC}, new int[]{d1, d2, d3}));
            assertEquals(10, sC.min(), "not last should set est(C)=10");
        } catch (InconsistencyException e) {
            assert (false);
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("solver")
    public void testNotLast(Solver cp) {
        IntVar sA = makeIntVar(cp, 0, 9);
        int d1 = 5;
        IntVar sB = makeIntVar(cp, 1, 10);
        int d2 = 5;
        IntVar sC = makeIntVar(cp, 3, 9);
        int d3 = 4;

        try {
            cp.post(new Disjunctive(new IntVar[]{sA, sB, sC}, new int[]{d1, d2, d3}));
            assertEquals(6, sC.max(), "not last should set lst(C)=6");
        } catch (InconsistencyException e) {
            assert (false);
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


}
