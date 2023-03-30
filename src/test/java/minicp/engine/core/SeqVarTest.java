package minicp.engine.core;

import minicp.cp.Factory;
import minicp.engine.SolverTest;
import minicp.state.StateManager;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static minicp.engine.core.SeqVarAssertion.assertGraphSeqValid;
import static org.junit.jupiter.api.Assertions.*;

public class SeqVarTest extends SolverTest {
    
    static int nNodes = 12;
    static int begin = 10;
    static int end = 11;

    private void resetPropagatorsArrays(boolean[]... propagator) {
        for (boolean[] p: propagator) {
            Arrays.fill(p, false);
        }
    }

    private static Stream<Arguments> seqWithInserts() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            return Arguments.of(
                    Factory.makeGraphSeqVar(cp, nNodes, begin, end),
                    new int[nNodes-2]);
        });
    }

    private static Stream<Arguments> seqVar() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            return Arguments.of(Factory.makeGraphSeqVar(cp, nNodes, begin, end));
        });
    }

    private static Stream<Arguments> SeqVarPropArrays() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            return Arguments.of(
                    Factory.makeGraphSeqVar(cp, nNodes, begin, end),
                    new boolean[nNodes], new boolean[nNodes],new boolean[nNodes],new boolean[nNodes],new boolean[nNodes]);
        });
    }

    private static Stream<Arguments> SeqVarPropAtomic() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            return Arguments.of(
                    Factory.makeGraphSeqVar(cp, nNodes, begin, end),
                    new AtomicReference<>(false), new AtomicReference<>(false),
                    new AtomicReference<>(false), new AtomicReference<>(false));
        });
    }

    private void assertIsBoolArrayTrueAt(boolean[] values, int... indexes) {
        Arrays.sort(indexes);
        int j = 0;
        int i = 0;
        for (; i < values.length && j < indexes.length; ++i) {
            if (i == indexes[j]) {
                assertTrue(values[i]);
                ++j;
            } else {
                assertFalse(values[i]);
            }
        }
        for (; i < values.length ; ++i) {
            assertFalse(values[i]);
        }
    }

    /**
     * Tests if the graph sequence var is constructed with the right number of nodes and insertions
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testGraphSeqVar(SeqVar seqVar) {
        int[] member = new int[] {begin, end};
        int[] possible = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] excluded = new int[] {};
        assertGraphSeqValid(seqVar, member, possible, excluded);
        // explicit call with all insertions. Should be the same assertion as above
        int[][] memberPredInsert1 = new int[][] {
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {},
                {}
        };
        int[][] possiblePredInsert1 = new int[][] {
                {   1, 2, 3, 4, 5, 6, 7, 8, 9},
                {0,    2, 3, 4, 5, 6, 7, 8, 9},
                {0, 1,    3, 4, 5, 6, 7, 8, 9},
                {0, 1, 2,    4, 5, 6, 7, 8, 9},
                {0, 1, 2, 3,    5, 6, 7, 8, 9},
                {0, 1, 2, 3, 4,    6, 7, 8, 9},
                {0, 1, 2, 3, 4, 5,    7, 8, 9},
                {0, 1, 2, 3, 4, 5, 6,    8, 9},
                {0, 1, 2, 3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3, 4, 5, 6, 7, 8,  },
                {                            },
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}
        };
        int[][] memberSuccInsert1 = new int[][] {
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {},
                {}
        };
        int[][] possibleSuccInsert1 = new int[][] {
                {   1, 2, 3, 4, 5, 6, 7, 8, 9},
                {0,    2, 3, 4, 5, 6, 7, 8, 9},
                {0, 1,    3, 4, 5, 6, 7, 8, 9},
                {0, 1, 2,    4, 5, 6, 7, 8, 9},
                {0, 1, 2, 3,    5, 6, 7, 8, 9},
                {0, 1, 2, 3, 4,    6, 7, 8, 9},
                {0, 1, 2, 3, 4, 5,    7, 8, 9},
                {0, 1, 2, 3, 4, 5, 6,    8, 9},
                {0, 1, 2, 3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3, 4, 5, 6, 7, 8,  },
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
                {                            },
        };
        assertGraphSeqValid(seqVar, member, possible, excluded,
                memberPredInsert1, possiblePredInsert1, memberSuccInsert1, possibleSuccInsert1);
    }

    /**
     * Tests for the insertions of nodes within the graph sequence
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testInsert(SeqVar seqVar) {
        StateManager sm = seqVar.getSolver().getStateManager();
        sm.saveState();
        seqVar.insert(begin, 0);
        seqVar.insert(0, 2);
        // sequence at this point: begin -> 0 -> 2 -> end
        int[] member1 = new int[] {begin, 0, 2, end};
        int[] possible1 = new int[] {1, 3, 4, 5, 6, 7, 8, 9};
        int[] excluded1 = new int[] {};
        assertGraphSeqValid(seqVar, member1, possible1, excluded1);

        sm.saveState();

        seqVar.insert(begin, 5);
        seqVar.insert(2, 7);
        // begin -> 5 -> 0 -> 2 -> 7 -> end
        int[] member2 = new int[] {begin, 5, 0, 2, 7, end};
        int[] possible2 = new int[] {1, 3, 4, 6, 8, 9};
        int[] excluded2 = new int[] {};
        assertGraphSeqValid(seqVar, member2, possible2, excluded2);

        sm.saveState();

        seqVar.insert(begin, 1);
        seqVar.insert(7, 9);
        seqVar.insert(2, 6);
        seqVar.insert(1, 8);
        seqVar.insert(8, 3);
        seqVar.insert(2, 4);
        // begin -> 1 -> 8 -> 3 -> 5 -> 0 -> 2 -> 4 -> 6 -> 7 -> 9 -> end
        int[] member3 = new int[] {begin, 1, 8, 3, 5, 0, 2, 4, 6, 7, 9, end};
        int[] possible3 = new int[] {};
        int[] excluded3 = new int[] {};
        assertGraphSeqValid(seqVar, member3, possible3, excluded3);

        sm.restoreState();
        assertGraphSeqValid(seqVar, member2, possible2, excluded2);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member1, possible1, excluded1);
    }

    /**
     * Tests for the exclusion of nodes within the sequence
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testExclude(SeqVar seqVar) {
        StateManager sm = seqVar.getSolver().getStateManager();
        seqVar.exclude(8);
        int[] member1 = new int[] {begin, end};
        int[] possible1 = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 9};
        int[] excluded1 = new int[] {8};
        assertGraphSeqValid(seqVar, member1, possible1, excluded1);

        sm.saveState();

        seqVar.exclude(2);
        seqVar.exclude(0);
        int[] member2 = new int[] {begin, end};
        int[] possible2 = new int[] {1, 3, 4, 5, 6, 7, 9};
        int[] excluded2 = new int[] {8, 2, 0};
        assertGraphSeqValid(seqVar, member2, possible2, excluded2);

        sm.saveState();

        seqVar.exclude(1);
        seqVar.exclude(9);
        seqVar.exclude(4);
        seqVar.exclude(5);
        seqVar.exclude(7);
        seqVar.exclude(6);
        seqVar.exclude(3);
        int[] member3 = new int[] {begin, end};
        int[] possible3 = new int[] {};
        int[] excluded3 = new int[] {8, 2, 0, 1, 3, 4, 5, 6, 7, 9};
        assertGraphSeqValid(seqVar, member3, possible3, excluded3);

        sm.restoreState();
        assertGraphSeqValid(seqVar, member2, possible2, excluded2);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member1, possible1, excluded1);
    }

    /**
     * Tests for the removal of individual insertions candidates
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testRemoveInsert(SeqVar seqVar) {
        StateManager sm = seqVar.getSolver().getStateManager();
        seqVar.removePredInsert(8, 2);
        seqVar.removePredInsert(5, 2);
        seqVar.removePredInsert(2, 3);
        int[] member = new int[] {begin, end};
        int[] possible = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] excluded = new int[] {};
        int[][] memberPredInsert1 = new int[][] {
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {},
                {}
        };
        int[][] possiblePredInsert1 = new int[][] {
                {   1, 2, 3, 4, 5, 6, 7, 8, 9},
                {0,    2, 3, 4, 5, 6, 7, 8, 9},
                {0, 1,    3, 4,    6, 7,    9},
                {0, 1,       4, 5, 6, 7, 8, 9},
                {0, 1, 2, 3,    5, 6, 7, 8, 9},
                {0, 1, 2, 3, 4,    6, 7, 8, 9},
                {0, 1, 2, 3, 4, 5,    7, 8, 9},
                {0, 1, 2, 3, 4, 5, 6,    8, 9},
                {0, 1, 2, 3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3, 4, 5, 6, 7, 8,  },
                {                            },
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}
        };
        int[][] memberSuccInsert1 = new int[][] {
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {},
                {}
        };
        int[][] possibleSuccInsert1 = new int[][] {
                {   1, 2, 3, 4, 5, 6, 7, 8, 9},
                {0,    2, 3, 4, 5, 6, 7, 8, 9},
                {0, 1,       4, 5, 6, 7, 8, 9},
                {0, 1, 2,    4, 5, 6, 7, 8, 9},
                {0, 1, 2, 3,    5, 6, 7, 8, 9},
                {0, 1,    3, 4,    6, 7, 8, 9},
                {0, 1, 2, 3, 4, 5,    7, 8, 9},
                {0, 1, 2, 3, 4, 5, 6,    8, 9},
                {0, 1,    3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3, 4, 5, 6, 7, 8,  },
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
                {                            }
        };
        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert1, possiblePredInsert1,
                memberSuccInsert1, possibleSuccInsert1);
        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert1, possiblePredInsert1);

        sm.saveState();

        seqVar.removePredInsert(begin, 4);
        seqVar.removePredInsert(begin, 9);
        seqVar.removePredInsert(0, end);

        int[][] memberPredInsert2 = new int[][] {
                {begin},
                {begin},
                {begin},
                {begin},
                {},
                {begin},
                {begin},
                {begin},
                {begin},
                {},
                {},
                {}
        };
        int[][] possiblePredInsert2 = new int[][] {
                {   1, 2, 3, 4, 5, 6, 7, 8, 9},
                {0,    2, 3, 4, 5, 6, 7, 8, 9},
                {0, 1,    3, 4,    6, 7,    9},
                {0, 1,       4, 5, 6, 7, 8, 9},
                {0, 1, 2, 3,    5, 6, 7, 8, 9},
                {0, 1, 2, 3, 4,    6, 7, 8, 9},
                {0, 1, 2, 3, 4, 5,    7, 8, 9},
                {0, 1, 2, 3, 4, 5, 6,    8, 9},
                {0, 1, 2, 3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3, 4, 5, 6, 7, 8,  },
                {                            },
                {   1, 2, 3, 4, 5, 6, 7, 8, 9}
        };
        int[][] memberSuccInsert2 = new int[][] {
                {},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {},
                {}
        };
        int[][] possibleSuccInsert2 = new int[][] {
                {   1, 2, 3, 4, 5, 6, 7, 8, 9},
                {0,    2, 3, 4, 5, 6, 7, 8, 9},
                {0, 1,       4, 5, 6, 7, 8, 9},
                {0, 1, 2,    4, 5, 6, 7, 8, 9},
                {0, 1, 2, 3,    5, 6, 7, 8, 9},
                {0, 1,    3, 4,    6, 7, 8, 9},
                {0, 1, 2, 3, 4, 5,    7, 8, 9},
                {0, 1, 2, 3, 4, 5, 6,    8, 9},
                {0, 1,    3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3, 4, 5, 6, 7, 8,  },
                {0, 1, 2, 3,    5, 6, 7, 8,  },
                {                            },
        };

        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert2, possiblePredInsert2,
                memberSuccInsert2, possibleSuccInsert2);
        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert2, possiblePredInsert2);

        sm.saveState();

        seqVar.removePredInsert(begin, 8);
        seqVar.removePredInsert(begin, 1);
        seqVar.removePredInsert(9, end);
        seqVar.removePredInsert(9, 4);
        seqVar.removePredInsert(2, 4);
        seqVar.removePredInsert(0, 4);
        seqVar.removePredInsert(1, 4);
        seqVar.removePredInsert(6, 3);
        seqVar.removePredInsert(7, 6);

        int[][] memberPredInsert3 = new int[][] {
                {begin},
                {},
                {begin},
                {begin},
                {},
                {begin},
                {begin},
                {begin},
                {},
                {},
                {},
                {}
        };
        int[][] possiblePredInsert3 = new int[][] {
                {   1, 2, 3, 4, 5, 6, 7, 8, 9},
                {0,    2, 3, 4, 5, 6, 7, 8, 9},
                {0, 1,    3, 4,    6, 7,    9},
                {0, 1,       4, 5,    7, 8, 9},
                {         3,    5, 6, 7, 8,  },
                {0, 1, 2, 3, 4,    6, 7, 8, 9},
                {0, 1, 2, 3, 4, 5,       8, 9},
                {0, 1, 2, 3, 4, 5, 6,    8, 9},
                {0, 1, 2, 3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3, 4, 5, 6, 7, 8,  },
                {                            },
                {   1, 2, 3, 4, 5, 6, 7, 8,  }
        };
        int[][] memberSuccInsert3 = new int[][] {
                {},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {end},
                {},
                {},
                {}
        };
        int[][] possibleSuccInsert3 = new int[][] {
                {   1, 2, 3,    5, 6, 7, 8, 9},
                {0,    2, 3,    5, 6, 7, 8, 9},
                {0, 1,          5, 6, 7, 8, 9},
                {0, 1, 2,    4, 5, 6, 7, 8, 9},
                {0, 1, 2, 3,    5, 6, 7, 8, 9},
                {0, 1,    3, 4,    6, 7, 8, 9},
                {0, 1, 2,    4, 5,    7, 8, 9},
                {0, 1, 2, 3, 4, 5,       8, 9},
                {0, 1,    3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3,    5, 6, 7, 8,  },
                {0,    2, 3,    5, 6, 7,     },
                {                            },
        };

        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert3, possiblePredInsert3,
                memberSuccInsert3, possibleSuccInsert3);
        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert3, possiblePredInsert3);

        sm.restoreState();
        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert2, possiblePredInsert2,
                memberSuccInsert2, possibleSuccInsert2);
        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert2, possiblePredInsert2);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert1, possiblePredInsert1,
                memberSuccInsert1, possibleSuccInsert1);
        assertGraphSeqValid(seqVar, member, possible, excluded, memberPredInsert1, possiblePredInsert1);

    }

    /**
     * Tests that removing all insertions of one node ends up excluding it
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testRemoveAllInsertMeansExclude(SeqVar seqVar) {
        int[] member1 = new int[] {begin, end};
        int[] possible1 = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] excluded1 = new int[] {};
        int[][] memberPredInsert1 = new int[][] {
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {begin},
                {},
                {}
        };
        int[][] possiblePredInsert1 = new int[][] {
                {   1, 2, 3, 4, 5, 6, 7, 8, 9},
                {0,    2, 3, 4, 5, 6, 7, 8, 9},
                {0, 1,    3, 4, 5, 6, 7, 8, 9},
                {0, 1, 2,    4, 5, 6, 7, 8, 9},
                {0, 1, 2, 3,    5, 6, 7, 8, 9},
                {0, 1, 2, 3, 4,    6, 7, 8, 9},
                {0, 1, 2, 3, 4, 5,    7, 8, 9},
                {0, 1, 2, 3, 4, 5, 6,    8, 9},
                {0, 1, 2, 3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3, 4, 5, 6, 7, 8,  },
                {                            },
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}
        };

        int node = 5;
        int nRemoved = 0;
        for (int i = 0 ; i < nNodes ; ++i) {
            if (i != node) {
                seqVar.removePredInsert(i, node);
                ++nRemoved;
                int finalI = i;
                if (i == begin || i == end) { // the insertion was a member node
                    memberPredInsert1[node] = Arrays.stream(memberPredInsert1[node]).filter(j -> j != finalI).toArray();
                } else { // the insertion was a possible node
                    possiblePredInsert1[node] = Arrays.stream(possiblePredInsert1[node]).filter(j -> j != finalI).toArray();
                }
            }
            if (nRemoved < nNodes - 2) { // the maximum number of insertions that can be removed (nNodes - node itself - end)
                assertGraphSeqValid(seqVar, member1, possible1, excluded1, memberPredInsert1, possiblePredInsert1);
            }
        }
        int[] member2 = new int[] {begin, end};
        int[] possible2 = new int[] {0, 1, 2, 3, 4, 6, 7, 8, 9};
        int[] excluded2 = new int[] {node};
        assertGraphSeqValid(seqVar, member2, possible2, excluded2);
    }

    /**
     * Tests for both exclusions and insertions within the sequence
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testExcludeAndInsert(SeqVar seqVar) {
        StateManager sm = seqVar.getSolver().getStateManager();
        seqVar.insert(begin, 0);
        seqVar.insert(0, 2);
        seqVar.exclude(5);
        // sequence at this point: begin -> 0 -> 2 -> end
        int[] member1 = new int[] {begin, 0, 2, end};
        int[] possible1 = new int[] {1, 3, 4, 6, 7, 8, 9};
        int[] excluded1 = new int[] {5};
        assertGraphSeqValid(seqVar, member1, possible1, excluded1);

        sm.saveState();

        seqVar.insert(0, 4);
        seqVar.insert(begin, 9);
        seqVar.exclude(7);
        // sequence at this point: begin -> 9 -> 0 -> 4 -> 2 -> end
        int[] member2 = new int[] {begin, 9, 0, 4, 2, end};
        int[] possible2 = new int[] {1, 3, 6, 8};
        int[] excluded2 = new int[] {5, 7};
        assertGraphSeqValid(seqVar, member2, possible2, excluded2);

        sm.saveState();

        seqVar.insert(4, 3);
        seqVar.exclude(6);
        seqVar.insert(2, 1);
        // sequence at this point: begin -> 9 -> 0 -> 4 -> 2 -> end
        int[] member3 = new int[] {begin, 9, 0, 4, 3, 2, 1, end};
        int[] possible3 = new int[] {8};
        int[] excluded3 = new int[] {5, 7, 6};
        assertGraphSeqValid(seqVar, member3, possible3, excluded3);

        sm.restoreState();
        assertGraphSeqValid(seqVar, member2, possible2, excluded2);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member1, possible1, excluded1);
    }

    /**
     * Tests for both exclusions and removal of insertions within the sequence
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testInsertAndRemoveInsert(SeqVar seqVar) {
        StateManager sm = seqVar.getSolver().getStateManager();
        seqVar.insert(begin, 0);
        seqVar.insert(0, 2);
        seqVar.removePredInsert(0, 5);
        seqVar.removePredInsert(begin, 7);
        seqVar.removePredInsert(8, end);
        seqVar.removePredInsert(3, 4);
        // sequence at this point: begin -> 0 -> 2 -> end
        int[] member1 = new int[] {begin, 0, 2, end};
        int[] possible1 = new int[] {1, 3, 4, 5, 6, 7, 8, 9};
        int[] excluded1 = new int[] {};

        int[][] memberPredInsert1 = new int[][] {
                {},
                {begin, 0, 2},
                {},
                {begin, 0, 2},
                {begin, 0, 2},
                {begin, 2},
                {begin, 0, 2},
                {0, 2},
                {begin, 0, 2},
                {begin, 0, 2},
                {},
                {}
        };
        int[][] possiblePredInsert1 = new int[][] {
                {   1,    3, 4, 5, 6, 7, 8, 9},
                {         3, 4, 5, 6, 7, 8, 9},
                {   1,    3, 4, 5, 6, 7, 8, 9},
                {   1,       4, 5, 6, 7, 8, 9},
                {   1,          5, 6, 7, 8, 9},
                {   1,    3, 4,    6, 7, 8, 9},
                {   1,    3, 4, 5,    7, 8, 9},
                {   1,    3, 4, 5, 6,    8, 9},
                {   1,    3, 4, 5, 6, 7,    9},
                {   1,    3, 4, 5, 6, 7, 8,  },
                {                            },
                {   1,    3, 4, 5, 6, 7,    9}
        };
        assertGraphSeqValid(seqVar, member1, possible1, excluded1, memberPredInsert1, possiblePredInsert1);

        sm.saveState();
    }

    /**
     * Test for the requirement of nodes within the sequence
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testRequire(SeqVar seqVar) {
        StateManager sm = seqVar.getSolver().getStateManager();

        int[] member = new int[] {begin, end};
        int[] possible = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] excluded = new int[] {};
        assertGraphSeqValid(seqVar, member, possible, excluded);

        sm.saveState();

        seqVar.require(7);
        seqVar.require(3);
        seqVar.require(0);
        seqVar.require(1);
        int[] member1 = new int[] {begin, end};
        int[] required1 = new int[] {begin, end, 7, 3, 0, 1};
        int[] possible1 = new int[] {2, 4, 5, 6, 8, 9};
        int[] excluded1 = new int[] {};
        assertGraphSeqValid(seqVar, member1, required1, possible1, excluded1);

        sm.saveState();

        seqVar.require(4);
        seqVar.require(5);
        seqVar.require(9);
        seqVar.require(2);
        int[] member2 = member1;
        int[] required2 = new int[] {begin, end, 7, 3, 0, 1, 4, 5, 9, 2};
        int[] possible2 = new int[] {6, 8};
        int[] excluded2 = excluded1;
        assertGraphSeqValid(seqVar, member2, required2, possible2, excluded2);

        sm.saveState();

        seqVar.require(6);
        seqVar.require(8);
        int[] member3 = member1;
        int[] required3 = new int[] {begin, end, 7, 3, 0, 1, 4, 5, 9, 2, 6, 8};
        int[] possible3 = new int[] {};
        int[] excluded3 = excluded1;
        assertGraphSeqValid(seqVar, member3, required3, possible3, excluded3);
        assertFalse(seqVar.isFixed());

        sm.restoreState();
        assertGraphSeqValid(seqVar, member2, required2, possible2, excluded2);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member1, required1, possible1, excluded1);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member, possible, excluded);
    }

    /**
     * Tests for both exclusions and removal of insertions within the sequence
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testExcludeAndRemoveInsert(SeqVar seqVar) {
        StateManager sm = seqVar.getSolver().getStateManager();
        seqVar.exclude(3);
        seqVar.exclude(7);
        seqVar.removePredInsert(5, 8);
        seqVar.removePredInsert(begin, 8);
        seqVar.removePredInsert(begin, 1);
        seqVar.removePredInsert(2, end);
        seqVar.removePredInsert(0, 8);
        seqVar.removePredInsert(8, 5);
        int[][] memberPredInsert1 = new int[][] {
                {begin},
                {},
                {begin},
                {},
                {begin},
                {begin},
                {begin},
                {},
                {},
                {begin},
                {},
                {}
        };
        int[][] possiblePredInsert1 = new int[][] {
                {   1, 2,    4, 5, 6,    8, 9},
                {0,    2,    4, 5, 6,    8, 9},
                {0, 1,       4, 5, 6,    8, 9},
                {                            },
                {0, 1, 2,       5, 6,    8, 9},
                {0, 1, 2,    4,    6,       9},
                {0, 1, 2,    4, 5,       8, 9},
                {                            },
                {   1, 2,    4,    6,       9},
                {0, 1, 2,    4, 5, 6,    8,  },
                {                            },
                {0, 1,       4, 5, 6,    8, 9}
        };
        int[] member1 = new int[] {begin, end};
        int[] possible1 = new int[] {0, 1, 2, 4, 5, 6, 8, 9};
        int[] excluded1 = new int[] {3, 7};
        assertGraphSeqValid(seqVar, member1, possible1, excluded1, memberPredInsert1, possiblePredInsert1);

        sm.saveState();

        seqVar.removePredInsert(1, 0);
        seqVar.exclude(2);
        seqVar.removePredInsert(2, 6); // 2 has already been excluded, this should do nothing
        seqVar.removePredInsert(begin, 6);
        seqVar.exclude(8);
        seqVar.removePredInsert(0, 5);
        seqVar.removePredInsert(5, 4);
        int[][] memberPredInsert2 = new int[][] {
                {begin},
                {},
                {},
                {},
                {begin},
                {begin},
                {},
                {},
                {},
                {begin},
                {},
                {}
        };
        int[][] possiblePredInsert2 = new int[][] {
                {            4, 5, 6,       9},
                {0,          4, 5, 6,       9},
                {                            },
                {                            },
                {0, 1,             6,       9},
                {   1,       4,    6,       9},
                {0, 1,       4, 5,          9},
                {                            },
                {                            },
                {0, 1,       4, 5, 6,        },
                {                            },
                {0, 1,       4, 5, 6,       9}
        };
        int[] member2 = new int[] {begin, end};
        int[] possible2 = new int[] {0, 1, 4, 5, 6, 9};
        int[] excluded2 = new int[] {3, 7, 2, 8};
        assertGraphSeqValid(seqVar, member2, possible2, excluded2, memberPredInsert2, possiblePredInsert2);

        sm.saveState();

        seqVar.removePredInsert(1, 6);
        seqVar.exclude(9);
        seqVar.removePredInsert(2, 9);
        seqVar.removePredInsert(begin, 0);
        seqVar.exclude(5);
        seqVar.exclude(4);
        seqVar.removePredInsert(0, 1);
        seqVar.removePredInsert(5, 6);
        int[][] memberPredInsert3 = new int[][] {
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {}
        };
        int[][] possiblePredInsert3 = new int[][] {
                {                  6,        },
                {                  6,        },
                {                            },
                {                            },
                {                            },
                {                            },
                {0,                          },
                {                            },
                {                            },
                {                            },
                {                            },
                {0, 1,             6,        }
        };
        int[] member3 = new int[] {begin, end};
        int[] possible3 = new int[] {0, 1, 6};
        int[] excluded3 = new int[] {3, 7, 2, 8, 9, 5, 4};
        assertGraphSeqValid(seqVar, member3, possible3, excluded3, memberPredInsert3, possiblePredInsert3);

        sm.saveState();

        // exclude node 6 through the removal of all its related insertions
        seqVar.removePredInsert(0, 6);
        seqVar.removePredInsert(1, 6);
        seqVar.exclude(0);
        seqVar.removePredInsert(6, end);
        seqVar.exclude(1);
        int[][] memberPredInsert4 = new int[][] {
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {}
        };
        int[][] possiblePredInsert4 = new int[][] {
                {                            },
                {                            },
                {                            },
                {                            },
                {                            },
                {                            },
                {                            },
                {                            },
                {                            },
                {                            },
                {                            },
                {                            }
        };
        int[] member4 = new int[] {begin, end};
        int[] possible4 = new int[] {};
        int[] excluded4 = new int[] {3, 7, 2, 8, 9, 5, 4, 0, 1, 6};
        assertGraphSeqValid(seqVar, member4, possible4, excluded4, memberPredInsert4, possiblePredInsert4);

        sm.restoreState();
        assertGraphSeqValid(seqVar, member3, possible3, excluded3, memberPredInsert3, possiblePredInsert3);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member2, possible2, excluded2, memberPredInsert2, possiblePredInsert2);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member1, possible1, excluded1, memberPredInsert1, possiblePredInsert1);

    }

    /**
     * More complicated test! It checks for a mix of several operations, as well as trying to do some invalid operations
     *
     * Tests for a mix of
     *  - Insertions
     *  - Require
     *  - Exclusions
     *  - Removal of insertions
     *
     * Also tests for
     *  - Exclusion for inserted / required node, throwing an {@link InconsistencyException}
     *  - Exclusion of node because all its insertions points have been removed
     *      - Removal of all insertions points of a required node, throwing an {@link InconsistencyException}
     *  - State recovery from {@link StateManager#restoreState()}
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testChangeStateOperations(SeqVar seqVar) {
        StateManager sm = seqVar.getSolver().getStateManager();
        seqVar.require(8);
        seqVar.exclude(9);
        seqVar.removePredInsert(3, 8);
        seqVar.insert(begin, 2);

        int[] member1 = new int[] {begin, 2, end};
        int[] required1 = new int[] {begin, end, 8, 2};
        int[] possible1 = new int[] {0, 1, 3, 4, 5, 6, 7};
        int[] excluded1 = new int[] {9};
        int[][] memberPredInsert1 = new int[][] {
                {begin, 2},
                {begin, 2},
                {},
                {begin, 2},
                {begin, 2},
                {begin, 2},
                {begin, 2},
                {begin, 2},
                {begin, 2},
                {},
                {},
                {}
        };
        int[][] possiblePredInsert1 = new int[][] {
                {   1,    3, 4, 5, 6, 7, 8   },
                {0,       3, 4, 5, 6, 7, 8   },
                {0, 1,    3, 4, 5, 6, 7, 8   },
                {0, 1,       4, 5, 6, 7, 8   },
                {0, 1,    3,    5, 6, 7, 8   },
                {0, 1,    3, 4,    6, 7, 8   },
                {0, 1,    3, 4, 5,    7, 8   },
                {0, 1,    3, 4, 5, 6,    8   },
                {0, 1,       4, 5, 6, 7,     },
                {                            },
                {                            },
                {0, 1,    3, 4, 5, 6, 7, 8   }
        };
        assertGraphSeqValid(seqVar, member1, required1, possible1, excluded1, memberPredInsert1, possiblePredInsert1);

        sm.saveState();

        seqVar.insert(2, 4);
        seqVar.exclude(0);
        seqVar.require(7);
        seqVar.require(3);
        seqVar.removePredInsert(0, 1); // should do nothing
        seqVar.removePredInsert(begin, 5);
        seqVar.removePredInsert(4, 7);
        seqVar.removePredInsert(2, 4);
        seqVar.removePredInsert(2, 7);

        int[] member2 = new int[] {begin, 2, 4, end};
        int[] required2 = new int[] {begin, end, 8, 2, 4, 7, 3};
        int[] possible2 = new int[] {1, 5, 6};
        int[] excluded2 = new int[] {9, 0};
        int[][] memberPredInsert2 = new int[][] {
                {},
                {begin, 2, 4},
                {},
                {begin, 2, 4},
                {},
                {2, 4},
                {begin, 2, 4},
                {begin},
                {begin, 2, 4},
                {},
                {},
                {}
        };
        int[][] possiblePredInsert2 = new int[][] {
                {                            },
                {         3,    5, 6, 7, 8   },
                {   1,    3,    5, 6, 7, 8   },
                {   1,          5, 6, 7, 8   },
                {   1,    3,    5, 6, 7, 8   },
                {   1,    3,       6, 7, 8   },
                {   1,    3,    5,    7, 8   },
                {   1,    3,    5, 6,    8   },
                {   1,          5, 6, 7,     },
                {                            },
                {                            },
                {   1,    3,    5, 6, 7, 8   }
        };
        assertGraphSeqValid(seqVar, member2, required2, possible2, excluded2, memberPredInsert2, possiblePredInsert2);

        sm.saveState();

        seqVar.removePredInsert(8, 2);
        seqVar.removePredInsert(8, 1);
        seqVar.insert(2, 6);
        seqVar.removePredInsert(2, 6);
        seqVar.insert(begin, 7);
        seqVar.removePredInsert(2, 6);
        seqVar.exclude(5);

        int[] member3 = new int[] {begin, 7, 2, 6, 4, end};
        int[] required3 = new int[] {begin, end, 8, 2, 4, 7, 3, 6};
        int[] possible3 = new int[] {1};
        int[] excluded3 = new int[] {9, 0, 5};
        int[][] memberPredInsert3 = new int[][] {
                {},
                {begin, 2, 4, 6, 7},
                {},
                {begin, 2, 4, 6, 7},
                {},
                {},
                {},
                {},
                {begin, 2, 4, 6, 7},
                {},
                {},
                {}
        };
        int[][] possiblePredInsert3 = new int[][] {
                {                            },
                {         3,                 },
                {   1,    3,                 },
                {   1,                   8   },
                {   1,    3,             8   },
                {                            },
                {   1,    3,             8   },
                {   1,    3,             8   },
                {   1,                       },
                {                            },
                {                            },
                {   1,    3,             8   }
        };
        assertGraphSeqValid(seqVar, member3, required3, possible3, excluded3, memberPredInsert3, possiblePredInsert3);

        sm.saveState();

        /* now the fun begins! Test for:
         *  - Exclusion for inserted / required node, throwing an {@link InconsistencyException}
         *  - Exclusion of node because all its insertions points have been removed
         *      - Removal of all insertions points of a required node, throwing an {@link InconsistencyException}
         *  - State recovery from {@link StateManager#restoreState()}
         */
        for (Procedure invalidOp : new Procedure[] {
                () -> seqVar.exclude(2), // exclude an inserted node
                () -> seqVar.exclude(3), // exclude a required node
        }) {
            try {
                assertGraphSeqValid(seqVar, member3, required3, possible3, excluded3, memberPredInsert3, possiblePredInsert3);
                sm.saveState();
                invalidOp.call();
                fail("An invalid exclusion occurred without throwing an Inconsistency");
            } catch (InconsistencyException ignored) {
                sm.restoreState(); // goes back before the invalid operation
            }
        }

        // remove all predecessors for node 1, excluding it
        for (int i : new int[] {begin, 2, 3, 4, 5, 6, 7}) { // note that the removal of node 5 should have no effect
            assertFalse(seqVar.isExcluded(1)); // the node is not excluded yet!
            seqVar.removePredInsert(i, 1);
        }

        int[] member4 = new int[] {begin, 7, 2, 6, 4, end};
        int[] required4 = new int[] {begin, end, 8, 2, 4, 7, 3, 6};
        int[] possible4 = new int[] {};
        int[] excluded4 = new int[] {1, 9, 0, 5};
        int[][] memberPredInsert4 = new int[][] {
                {},
                {},
                {},
                {begin, 2, 4, 6, 7},
                {},
                {},
                {},
                {},
                {begin, 2, 4, 6, 7},
                {},
                {},
                {}
        };
        int[][] possiblePredInsert4 = new int[][] {
                {                            },
                {                            },
                {         3,                 },
                {                        8   },
                {         3,             8   },
                {                            },
                {         3,             8   },
                {         3,             8   },
                {                            },
                {                            },
                {                            },
                {         3,             8   }
        };
        assertGraphSeqValid(seqVar, member4, required4, possible4, excluded4, memberPredInsert4, possiblePredInsert4);

        // remove all insertions for a required node, trying to exclude it. This should fail
        sm.saveState();
        for (int i : new int[] {begin, 8, 2, 4, 6}) {
            seqVar.removePredInsert(i, 3);
        }
        try {
            assertFalse(seqVar.isExcluded(3));
            seqVar.removePredInsert(7, 3);
        } catch (InconsistencyException ignored) {
            sm.restoreState();
        }
        assertGraphSeqValid(seqVar, member4, required4, possible4, excluded4, memberPredInsert4, possiblePredInsert4);

        // insert all remaining nodes that are not yet inserted
        sm.saveState();
        seqVar.insert(begin, 3);
        seqVar.insert(6, 8);
        int[] member5 = new int[] {begin, 3, 7, 2, 6, 8, 4, end};
        int[] required5 = new int[] {begin, 3, 7, 2, 6, 8, 4, end};
        int[] possible5 = new int[] {};
        int[] excluded5 = new int[] {1, 9, 0, 5};
        assertGraphSeqValid(seqVar, member5, required5, possible5, excluded5);

        sm.restoreState();
        assertGraphSeqValid(seqVar, member4, required4, possible4, excluded4, memberPredInsert4, possiblePredInsert4);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member3, required3, possible3, excluded3, memberPredInsert3, possiblePredInsert3);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member2, required2, possible2, excluded2, memberPredInsert2, possiblePredInsert2);
        sm.restoreState();
        assertGraphSeqValid(seqVar, member1, required1, possible1, excluded1, memberPredInsert1, possiblePredInsert1);
    }

    /**
     * Tests if the propagation from the {@link SeqVar} is triggered correctly
     */
    @ParameterizedTest
    @MethodSource("SeqVarPropAtomic")
    public void testPropagationSequence(SeqVar seqVar, AtomicReference<Boolean> propagateFixCalled,
                                        AtomicReference<Boolean> propagateRequireCalled,
                                        AtomicReference<Boolean> propagateInsertCalled,
                                        AtomicReference<Boolean> propagateExcludeCalled) {
        Solver cp = seqVar.getSolver();
        AtomicReference<Boolean>[] propagators = new AtomicReference[] {propagateFixCalled,
                propagateExcludeCalled,
                propagateInsertCalled,
                propagateRequireCalled};
        Constraint cons = new AbstractConstraint(cp) {
            @Override
            public void post() {
                seqVar.whenFix(() -> propagateFixCalled.set(true));
                seqVar.whenInsert(() -> propagateInsertCalled.set(true));
                seqVar.whenExclude(() -> propagateExcludeCalled.set(true));
                seqVar.whenRequire(() -> propagateRequireCalled.set(true));
            }
        };
        cp.post(cons);
        seqVar.exclude(3);
        cp.fixPoint();
        assertTrue(propagateExcludeCalled.get());
        assertFalse(propagateFixCalled.get());
        assertFalse(propagateInsertCalled.get());
        assertFalse(propagateRequireCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        seqVar.exclude(2);
        cp.fixPoint();
        assertTrue(propagateExcludeCalled.get());
        assertFalse(propagateFixCalled.get());
        assertFalse(propagateInsertCalled.get());
        assertFalse(propagateRequireCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        seqVar.insert(begin, 7);
        cp.fixPoint();
        assertFalse(propagateExcludeCalled.get());
        assertFalse(propagateFixCalled.get());
        assertTrue(propagateInsertCalled.get());
        assertTrue(propagateRequireCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        // remove all insertions of node 1 except node begin
        for (int i : new int[] {0, 4, 5, 6, 7, 8, 9}) {
            seqVar.removePredInsert(i, 1);
            cp.fixPoint();
            // propagation is not triggered by removing a predecessor
            assertFalse(propagateExcludeCalled.get());
            assertFalse(propagateFixCalled.get());
            assertFalse(propagateInsertCalled.get());
            assertFalse(propagateRequireCalled.get());
            for (AtomicReference<Boolean> b : propagators)
                b.set(false);
        }
        seqVar.removePredInsert(begin, 1);
        cp.fixPoint();
        assertTrue(propagateExcludeCalled.get());
        assertFalse(propagateFixCalled.get());
        assertFalse(propagateInsertCalled.get());
        assertFalse(propagateRequireCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        // require a node
        seqVar.require(5);
        cp.fixPoint();
        assertFalse(propagateExcludeCalled.get());
        assertFalse(propagateFixCalled.get());
        assertFalse(propagateInsertCalled.get());
        assertTrue(propagateRequireCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        // insert a required node
        seqVar.insert(begin, 5);
        cp.fixPoint();
        assertFalse(propagateExcludeCalled.get());
        assertFalse(propagateFixCalled.get());
        assertTrue(propagateInsertCalled.get());
        assertFalse(propagateRequireCalled.get()); // the node has already been required anymore
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        // insert or exclude nodes until the sequence is (almost!) fixed
        int j = 0;
        for (int i: new int[] {0, 4, 8, 9}) {
            if (j++%2==0) {
                seqVar.insert(begin, i);
                cp.fixPoint();
                assertFalse(propagateExcludeCalled.get());
                assertFalse(propagateFixCalled.get());
                assertTrue(propagateInsertCalled.get());
                assertTrue(propagateRequireCalled.get());
            } else {
                seqVar.exclude(i);
                cp.fixPoint();
                assertTrue(propagateExcludeCalled.get());
                assertFalse(propagateFixCalled.get());
                assertFalse(propagateInsertCalled.get());
                assertFalse(propagateRequireCalled.get());
            }
            for (AtomicReference<Boolean> b : propagators)
                b.set(false);
        }
        cp.getStateManager().saveState();
        seqVar.exclude(6);
        cp.fixPoint();
        assertTrue(propagateExcludeCalled.get());
        assertTrue(propagateFixCalled.get());
        assertFalse(propagateInsertCalled.get());
        assertFalse(propagateRequireCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);
        cp.getStateManager().restoreState();
        seqVar.insert(begin, 6);
        cp.fixPoint();
        assertFalse(propagateExcludeCalled.get());
        assertTrue(propagateFixCalled.get());
        assertTrue(propagateInsertCalled.get());
        assertTrue(propagateRequireCalled.get());
    }

    /**
     * Tests if the propagation is triggered correctly by the {@link InsertVar}
     */
    @ParameterizedTest
    @MethodSource("SeqVarPropArrays")
    public void testPropagationInsertion(SeqVar seqVar, boolean[] propagateInsertArrCalled,
                                         boolean[] propagatePredChangeArrCalled,
                                         boolean[] propagateExcludeArrCalled,
                                         boolean[] propagateRequireArrCalled,
                                         boolean[] propagateSuccChangeArrCalled) {
        Solver cp = seqVar.getSolver();
        
        InsertVar[] predInsertVars = new InsertVar[nNodes];
        for (int i = 0; i < nNodes; ++i) {
            predInsertVars[i] = seqVar.getInsertionVar(i);
        }

        Constraint cons = new AbstractConstraint(cp) {
            @Override
            public void post() {
                for (int i = 0; i < nNodes; ++i) {
                    int finalI = i;
                    predInsertVars[i].whenInsert(() -> propagateInsertArrCalled[finalI] = true);
                    predInsertVars[i].whenPredChange(() -> propagatePredChangeArrCalled[finalI] = true);
                    predInsertVars[i].whenExclude(() -> propagateExcludeArrCalled[finalI] = true);
                    predInsertVars[i].whenRequire(() -> propagateRequireArrCalled[finalI] = true);
                    predInsertVars[i].whenSuccChange(() -> propagateSuccChangeArrCalled[finalI] = true);
                }
            }
        };
        cp.post(cons);
        seqVar.insert(begin, 9); // sequence = begin -> 9 -> end
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled, 9);
        assertIsBoolArrayTrueAt(propagatePredChangeArrCalled, 9);
        assertIsBoolArrayTrueAt(propagateSuccChangeArrCalled, 9);
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled);
        assertIsBoolArrayTrueAt(propagateRequireArrCalled, 9);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateExcludeArrCalled, propagateRequireArrCalled,
                propagatePredChangeArrCalled, propagateSuccChangeArrCalled);

        seqVar.require(8);
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled);
        assertIsBoolArrayTrueAt(propagatePredChangeArrCalled);
        assertIsBoolArrayTrueAt(propagateSuccChangeArrCalled);
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled);
        assertIsBoolArrayTrueAt(propagateRequireArrCalled, 8);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateExcludeArrCalled, propagateRequireArrCalled,
                propagatePredChangeArrCalled, propagateSuccChangeArrCalled);

        seqVar.insert(begin, 8);  // sequence = begin -> 8 -> 9 -> end
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled, 8);
        assertIsBoolArrayTrueAt(propagatePredChangeArrCalled, 8);
        assertIsBoolArrayTrueAt(propagateSuccChangeArrCalled, 8);
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled);
        assertIsBoolArrayTrueAt(propagateRequireArrCalled); // not called as the node was already required
        resetPropagatorsArrays(propagateInsertArrCalled, propagateExcludeArrCalled, propagateRequireArrCalled,
                propagatePredChangeArrCalled, propagateSuccChangeArrCalled);

        seqVar.exclude(3);
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled);
        // all not excluded nodes have their propagation triggered
        assertIsBoolArrayTrueAt(propagatePredChangeArrCalled, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, end);
        assertIsBoolArrayTrueAt(propagateSuccChangeArrCalled, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, begin);
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled, 3);
        assertIsBoolArrayTrueAt(propagateRequireArrCalled);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateExcludeArrCalled, propagateRequireArrCalled,
                propagatePredChangeArrCalled, propagateSuccChangeArrCalled);

        seqVar.exclude(5);
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled);
        // all not excluded nodes have their propagation triggered
        assertIsBoolArrayTrueAt(propagatePredChangeArrCalled, 0, 1, 2, 4, 5, 6, 7, 8, 9, end);
        assertIsBoolArrayTrueAt(propagateSuccChangeArrCalled, 0, 1, 2, 4, 5, 6, 7, 8, 9, begin);
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled, 5);
        assertIsBoolArrayTrueAt(propagateRequireArrCalled);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateExcludeArrCalled, propagateRequireArrCalled,
                propagatePredChangeArrCalled, propagateSuccChangeArrCalled);

        // removal of an insertion where the predecessor is a possible node
        seqVar.removePredInsert(4, 2);
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled);
        assertIsBoolArrayTrueAt(propagatePredChangeArrCalled, 2);
        assertIsBoolArrayTrueAt(propagateSuccChangeArrCalled, 4);
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled);
        assertIsBoolArrayTrueAt(propagateRequireArrCalled);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateExcludeArrCalled, propagateRequireArrCalled,
                propagatePredChangeArrCalled, propagateSuccChangeArrCalled);

        // removal of an insertion where the predecessor is a member node
        seqVar.removePredInsert(begin, 2);
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled);
        // all not excluded nodes have their propagation triggered
        assertIsBoolArrayTrueAt(propagatePredChangeArrCalled, 2);
        assertIsBoolArrayTrueAt(propagateSuccChangeArrCalled, begin);
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled);
        assertIsBoolArrayTrueAt(propagateRequireArrCalled);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateExcludeArrCalled, propagateRequireArrCalled,
                propagatePredChangeArrCalled, propagateSuccChangeArrCalled);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyDoubleInsert(SeqVar seqVar) {
        seqVar.insert(begin, 4);
        seqVar.insert(begin, 8); // begin -> 8 -> 4 -> end
        assertThrowsExactly(InconsistencyException.class, () -> seqVar.insert(4, 8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwNoInconsistencyDoubleInsert(SeqVar seqVar) {
        seqVar.insert(begin, 8);
        seqVar.insert(begin, 8); // double insertions at the same point are valid
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwNoInconsistencyDoubleExclude(SeqVar seqVar) {
        seqVar.exclude(8);
        seqVar.exclude(8);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwNoInconsistencyDoubleRequire(SeqVar seqVar) {
        seqVar.require(8);
        seqVar.require(8);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwNoInconsistencyRequireInsert(SeqVar seqVar) {
        seqVar.require(8);
        seqVar.insert(begin, 8);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwNoInconsistencyInsertRequire(SeqVar seqVar) {
        seqVar.insert(begin, 8);
        seqVar.require(8);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyExcludeInsert(SeqVar seqVar) {
        seqVar.exclude(8);
        assertThrowsExactly(InconsistencyException.class, () -> seqVar.insert(begin, 8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyInsertExclude(SeqVar seqVar) {
        seqVar.insert(begin, 8);
        assertThrowsExactly(InconsistencyException.class, () -> seqVar.exclude(8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyInvalidInsert1(SeqVar seqVar) {
        // 2 is not a member of the sequence
        assertThrowsExactly(InconsistencyException.class, () -> seqVar.insert(2, 8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyInvalidInsert2(SeqVar seqVar) {
        seqVar.removePredInsert(begin, 8);
        assertThrowsExactly(InconsistencyException.class, () ->  seqVar.insert(begin, 8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyRequireExclude(SeqVar seqVar) {
        seqVar.require(8);
        assertThrowsExactly(InconsistencyException.class, () -> seqVar.exclude(8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyExcludeRequire(SeqVar seqVar) {
        seqVar.exclude(8);
        assertThrowsExactly(InconsistencyException.class, () -> seqVar.require(8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void canInsertRequiredNode(SeqVar seqVar) {
        seqVar.require(8);
        assertTrue(seqVar.canInsert(begin, 8));
        seqVar.insert(begin, 8);
    }


    /**
     * Removing the last insertion of a required node should throw an error, even if it is a chained operation
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyChainedRemove(SeqVar seqVar) {
        StateManager sm = seqVar.getSolver().getStateManager();
        // require node 0, which can only be inserted after 1, which can only be inserted after node 2
        seqVar.require(0);
        for (int i = 0; i < nNodes ; ++i) {
            if (i != 1)
                seqVar.removePredInsert(i, 0); // node 0 can only be inserted after node 1
            if (i != 2)
                seqVar.removePredInsert(i, 1); // node 1 can only be inserted after node 2
        }
        // exclude node 2. This should exclude node 1, which would trigger an error as node 0 cannot be inserted anymore
        try {
            seqVar.exclude(2);
            fail("The last insertion of a required node became excluded but an inconsistency was not thrown");
        } catch (InconsistencyException ignored) {

        }
    }


}
