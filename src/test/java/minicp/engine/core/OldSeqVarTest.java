package minicp.engine.core;

import minicp.cp.Factory;
import minicp.engine.SolverTest;
import minicp.state.StateManager;
import minicp.util.exception.InconsistencyException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static minicp.engine.core.OldSeqVarAssertion.isSequenceValid;
import static org.junit.jupiter.api.Assertions.*;

public class OldSeqVarTest extends SolverTest {

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
                    Factory.makeSequenceVar(cp, nNodes, begin, end),
                    new int[nNodes-2]);
        });
    }

    private static Stream<Arguments> seqVar() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            return Arguments.of(Factory.makeSequenceVar(cp, nNodes, begin, end));
        });
    }

    private static Stream<Arguments> SeqVarPropArrays() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            return Arguments.of(
                    Factory.makeSequenceVar(cp, nNodes, begin, end),
                    new boolean[nNodes], new boolean[nNodes], new boolean[nNodes]);
        });
    }

    private static Stream<Arguments> SeqVarPropAtomic() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            return Arguments.of(
                    Factory.makeSequenceVar(cp, nNodes, begin, end),
                    new AtomicReference<>(false), new AtomicReference<>(false), new AtomicReference<>(false));
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
     * test if the sequence is constructed with the right number of nodes and insertions
     */
    @ParameterizedTest
    @MethodSource("seqWithInserts")
    public void testSequenceVar(OldSeqVar sequence, int[] insertions) {
        assertEquals(nNodes, sequence.nNode());
        assertEquals(begin, sequence.begin());
        assertEquals(end, sequence.end());
        assertEquals(sequence.begin(), sequence.nextMember(sequence.end()));
        assertEquals(sequence.end(), sequence.nextMember(sequence.begin()));
        assertEquals(sequence.begin(), sequence.predMember(sequence.end()));
        assertEquals(sequence.end(), sequence.predMember(sequence.begin()));
        assertEquals(2, sequence.nMember());
        assertEquals(10, sequence.nPossible());
        assertEquals(0, sequence.nExcluded());
        for (int i = 0; i < 10 ; ++i) {
            assertEquals(10, sequence.fillPredInsert(i, insertions));
            assertEquals(sequence.nPredInsert(i), sequence.nPossiblePredInsert(i) + sequence.nMemberPredInsert(i),
                    String.format("Node %d has %d insertions but %d of them are possible and %d member",
                            i, sequence.nPredInsert(i), sequence.nPossiblePredInsert(i), sequence.nMemberPredInsert(i)));
            boolean beginFound = false; // true if the begin node is considered as a predecessor
            for (int val: insertions) {
                assertNotEquals(val, i); // a node cannot have itself as predecessor
                beginFound = beginFound || val == 10;
            }
            assertTrue(beginFound);
        }
    }

    /**
     * test for the scheduling of insertions within the sequence
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testSchedule(OldSeqVar sequence) {
        StateManager sm = sequence.getSolver().getStateManager();
        sm.saveState();

        sequence.insert(sequence.begin(), 0);
        sequence.insert(0, 2);
        // sequence at this point: begin -> 0 -> 2 -> end
        int[] scheduled1 = new int[] {begin, 0, 2, end};
        int[] possible1 = new int[] {1, 3, 4, 5, 6, 7, 8, 9};
        int[] excluded1 = new int[] {};
        isSequenceValid(sequence, scheduled1, possible1, excluded1);

        sm.saveState();

        sequence.insert(sequence.begin(), 8);  // begin -> 8 -> 0 -> 2 -> end
        sequence.insert(2, 5);           // begin -> 8 -> 0 -> 2 -> 5 -> end
        int[] scheduled2 = new int[] {begin, 8, 0, 2, 5, end};
        int[] possible2 = new int[] {1, 3, 4, 6, 7, 9};
        int[] excluded2 = new int[] {};
        isSequenceValid(sequence, scheduled2, possible2, excluded2);

        sm.saveState();

        sequence.insert(8, 3);  // begin -> 8 -> 3 -> 0 -> 2 -> end
        sequence.insert(2, 7);  // begin -> 8 -> 3 -> 0 -> 2 -> 7 -> 5 -> end
        int[] scheduled3 = new int[] {begin, 8, 3, 0, 2, 7, 5, end};
        int[] possible3 = new int[] {1, 4, 6, 9};
        int[] excluded3 = new int[] {};
        isSequenceValid(sequence, scheduled3, possible3, excluded3);

        sm.saveState();

        sequence.insert(0, 4);  // begin -> 8 -> 3 -> 0 -> 4 -> 2 -> 7 -> 5 -> end
        sequence.insert(0, 9);  // begin -> 8 -> 3 -> 0 -> 9 -> 4 -> 2 -> 7 -> 5 -> end
        int[] scheduled4 = new int[] {begin, 8, 3, 0, 9, 4, 2, 7, 5, end};
        int[] possible4 = new int[] {1, 6};
        int[] excluded4 = new int[] {};
        isSequenceValid(sequence, scheduled4, possible4, excluded4);

        sm.saveState();

        sequence.insert(3, 1);  // begin -> 8 -> 3 -> 1 -> 0 -> 4 -> 2 -> 7 -> 5 -> end
        sequence.insert(5, 6);  // begin -> 8 -> 3 -> 0 -> 9 -> 4 -> 2 -> 7 -> 5 -> 6 -> end
        int[] scheduled5 = new int[] {begin, 8, 3, 1, 0, 9, 4, 2, 7, 5, 6, end};
        int[] possible5 = new int[] {};
        int[] excluded5 = new int[] {};
        isSequenceValid(sequence, scheduled5, possible5, excluded5);

        sm.restoreState();
        isSequenceValid(sequence, scheduled4, possible4, excluded4);
        sm.restoreState();
        isSequenceValid(sequence, scheduled3, possible3, excluded3);
        sm.restoreState();
        isSequenceValid(sequence, scheduled2, possible2, excluded2);
        sm.restoreState();
        isSequenceValid(sequence, scheduled1, possible1, excluded1);
        sm.restoreState();

        int[] scheduledInit = new int[] {begin, end};
        int[] possibleInit = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] excludedInit = new int[] {};
        isSequenceValid(sequence, scheduledInit, possibleInit, excludedInit);
    }

    /**
     * test for exclusion of nodes within the sequence
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testExclude(OldSeqVar sequence) {
        StateManager sm = sequence.getSolver().getStateManager();
        sm.saveState();

        sequence.exclude(0);
        sequence.exclude(2);
        int[] scheduled1 = new int[] {begin, end};
        int[] possible1 = new int[] {1, 3, 4, 5, 6, 7, 8, 9};
        int[] excluded1 = new int[] {0, 2};
        isSequenceValid(sequence, scheduled1, possible1, excluded1);

        sm.saveState();

        sequence.exclude(5);
        sequence.exclude(7);
        sequence.exclude(9);
        sequence.exclude(3);
        int[] scheduled2 = new int[] {begin, end};
        int[] possible2 = new int[] {1, 4, 6, 8};
        int[] excluded2 = new int[] {0, 2, 5, 7, 9, 3};
        isSequenceValid(sequence, scheduled2, possible2, excluded2);

        sm.saveState();

        sequence.exclude(4);
        sequence.exclude(6);
        sequence.exclude(1);
        sequence.exclude(8);
        int[] scheduled3 = new int[] {begin, end};
        int[] possible3 = new int[] {};
        int[] excluded3 = new int[] {0, 2, 5, 7, 9, 3, 5, 6, 1, 8};
        isSequenceValid(sequence, scheduled3, possible3, excluded3);

        sm.restoreState();
        isSequenceValid(sequence, scheduled2, possible2, excluded2);
        sm.restoreState();
        isSequenceValid(sequence, scheduled1, possible1, excluded1);
        sm.restoreState();

        int[] scheduledInit = new int[] {begin, end};
        int[] possibleInit = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] excludedInit = new int[] {};
        isSequenceValid(sequence, scheduledInit, possibleInit, excludedInit);
    }

    /**
     * test for both exclusion and scheduling of insertions within the sequence
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testExcludeAndSchedule(OldSeqVar sequence) {
        StateManager sm = sequence.getSolver().getStateManager();
        sm.saveState();
        sequence.insert(sequence.begin(), 4);
        sequence.exclude(5);
        sequence.exclude(6);

        int[] scheduled1 = new int[] {begin, 4, end};
        int[] possible1 = new int[] {0, 1, 2, 3, 7, 8, 9};
        int[] excluded1 = new int[] {5, 6};
        isSequenceValid(sequence, scheduled1, possible1, excluded1);
        sm.saveState();

        sequence.insert(4, 9);
        sequence.exclude(2);
        int[] scheduled2 = new int[] {begin, 4, 9, end};
        int[] possible2 = new int[] {0, 1, 3, 7, 8};
        int[] excluded2 = new int[] {2, 5, 6};
        isSequenceValid(sequence, scheduled2, possible2, excluded2);

        sm.saveState();

        sequence.exclude(1);
        sequence.insert(sequence.begin(), 7);
        int[] scheduled3 = new int[] {begin, 7, 4, 9, end};
        int[] possible3 = new int[] {0, 3, 8};
        int[] excluded3 = new int[] {1, 2, 5, 6};
        isSequenceValid(sequence, scheduled3, possible3, excluded3);

        sm.restoreState();
        isSequenceValid(sequence, scheduled2, possible2, excluded2);
        sm.restoreState();
        isSequenceValid(sequence, scheduled1, possible1, excluded1);
        sm.restoreState();
        int[] scheduledInit = new int[] {begin, end};
        int[] possibleInit = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] excludedInit = new int[] {};
        isSequenceValid(sequence, scheduledInit, possibleInit, excludedInit);
    }

    /**
     * test for removal of individual insertion candidates in an InsertionVar
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testIndividualInsertionRemoval(OldSeqVar sequence) {
        StateManager sm = sequence.getSolver().getStateManager();
        sm.saveState();

        OldInsertVar[] predInsertVars = new OldInsertVar[nNodes];
        for (int i=0; i< nNodes; ++i) {
            predInsertVars[i] = sequence.getInsertionVar(i);
        }

        assertEquals(0, sequence.nMemberPredInsert(begin));
        predInsertVars[0].removePred(4);
        predInsertVars[0].removePred(9);
        predInsertVars[1].removePred(0);
        predInsertVars[1].removePred(8);
        predInsertVars[1].removePred(2);
        predInsertVars[6].removePred(0);
        predInsertVars[6].removePred(3);
        assertEquals(0, sequence.nMemberPredInsert(begin));
        predInsertVars[7].removePred(sequence.begin());
        assertEquals(0, sequence.nMemberPredInsert(begin));
        predInsertVars[7].removePred(5);
        predInsertVars[7].removePred(6);
        int[] scheduled1 = new int[] {begin, end};
        int[] possible1 = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] excluded1 = new int[] {};
        int[][] scheduledInsertions1 = new int[][] {
                {sequence.begin()},
                {sequence.begin()},
                {sequence.begin()},
                {sequence.begin()},
                {sequence.begin()},
                {sequence.begin()},
                {sequence.begin()},
                {},
                {sequence.begin()},
                {sequence.begin()},
                {},
                {}
        };
        int[][] possibleInsertions1 = new int[][] {
                {   1, 2, 3,    5, 6, 7, 8,  },
                {         3, 4, 5, 6, 7,    9},
                {0, 1,    3, 4, 5, 6, 7, 8, 9},
                {0, 1, 2,    4, 5, 6, 7, 8, 9},
                {0, 1, 2, 3,    5, 6, 7, 8, 9},
                {0, 1, 2, 3, 4,    6, 7, 8, 9},
                {   1, 2,    4, 5,    7, 8, 9},
                {0, 1, 2, 3, 4,          8, 9},
                {0, 1, 2, 3, 4, 5, 6, 7,    9},
                {0, 1, 2, 3, 4, 5, 6, 7, 8,  },
                {},
                {}
        };
        isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertions1, possibleInsertions1);
        sm.saveState();

        assertNotEquals(0, sequence.nMemberPredInsert(4));
        sequence.insert(sequence.begin(), 4);
        assertEquals(0, sequence.nMemberPredInsert(4));
        predInsertVars[2].removePred(3); // possible insert
        predInsertVars[2].removePred(4); // scheduled insert
        predInsertVars[2].removePred(6); // possible insert
        predInsertVars[2].removePred(sequence.begin()); // scheduled insert
        predInsertVars[8].removePred(4); // scheduled insert
        sequence.exclude(5);
        int[][] scheduledInsertions2 = new int[][] {
                {sequence.begin()},
                {sequence.begin(), 4},
                {},
                {sequence.begin(), 4},
                {sequence.begin(), 4},
                {sequence.begin(), 4},
                {sequence.begin(), 4},
                {4},
                {sequence.begin()},
                {sequence.begin(), 4},
                {},
                {}
        };
        int[][] possibleInsertions2 = new int[][] {
                {   1, 2, 3,       6, 7, 8,  },
                {         3,       6, 7,    9},
                {0, 1,                7, 8, 9},
                {0, 1, 2,          6, 7, 8, 9},
                {}, // sequenced node
                {0, 1, 2, 3,       6, 7, 8, 9},
                {   1, 2,             7, 8, 9},
                {0, 1, 2, 3,             8, 9},
                {0, 1, 2, 3,       6, 7,    9},
                {0, 1, 2, 3,       6, 7, 8,  },
                {},
                {}
        };
        int[] scheduled2 = new int[] {begin, 4, end};
        int[] possible2= new int[] {0, 1, 2, 3, 6, 7, 8, 9};
        int[] excluded2 = new int[] {5};
        isSequenceValid(sequence, scheduled2, possible2, excluded2, scheduledInsertions2, possibleInsertions2);

        sm.restoreState();
        isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertions1, possibleInsertions1);
    }

    /**
     * test for calls to propagation within the InsertionVars contained in the sequence
     */
    @ParameterizedTest
    @MethodSource("SeqVarPropArrays")
    public void testPropagationInsertion(OldSeqVar sequence, boolean[] propagateInsertArrCalled,
                                         boolean[] propagateChangeArrCalled, boolean[] propagateExcludeArrCalled) {
        Solver cp = sequence.getSolver();

        OldInsertVar[] predInsertVars = new OldInsertVar[nNodes];
        for (int i = 0; i < nNodes; ++i) {
            predInsertVars[i] = sequence.getInsertionVar(i);
        }

        Constraint cons = new AbstractConstraint(cp) {
            @Override
            public void post() {
                for (int i = 0; i < nNodes; ++i) {
                    int finalI = i;
                    predInsertVars[i].whenInsert(() -> propagateInsertArrCalled[finalI] = true);
                    predInsertVars[i].whenPredChange(() -> propagateChangeArrCalled[finalI] = true);
                    predInsertVars[i].whenExclude(() -> propagateExcludeArrCalled[finalI] = true);
                }
            }
        };
        cp.post(cons);
        sequence.insert(sequence.begin(), 9); // sequence= begin -> 9 -> end
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled, 9);
        assertIsBoolArrayTrueAt(propagateChangeArrCalled, 9);
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateChangeArrCalled, propagateExcludeArrCalled);

        sequence.exclude(5);
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled);
        assertIsBoolArrayTrueAt(propagateChangeArrCalled, 0, 1, 2, 3, 4, 6, 7, 8); // node 5 and 9 don't have a change in their domain
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled, 5);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateChangeArrCalled, propagateExcludeArrCalled);

        sequence.insert(sequence.begin(), 2); // sequence= begin -> 2 -> 9 -> end
        sequence.insert(sequence.begin(), 8); // sequence= begin -> 8 -> 2 -> 9 -> end
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled, 2, 8);
        assertIsBoolArrayTrueAt(propagateChangeArrCalled, 2, 8);
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateChangeArrCalled, propagateExcludeArrCalled);

        sequence.exclude(3);
        cp.fixPoint();
        assertIsBoolArrayTrueAt(propagateInsertArrCalled);
        assertIsBoolArrayTrueAt(propagateChangeArrCalled, 0, 1, 4, 6, 7); // node 8, 2, 9, 5, 3 don't have a change in their domain
        assertIsBoolArrayTrueAt(propagateExcludeArrCalled, 3);
        resetPropagatorsArrays(propagateInsertArrCalled, propagateChangeArrCalled, propagateExcludeArrCalled);
    }

    /**
     * test for calls to propagation within the sequence
     */
    @ParameterizedTest
    @MethodSource("SeqVarPropAtomic")
    public void testPropagationSequence(OldSeqVar sequence, AtomicReference<Boolean> propagateBindCalled,
                                        AtomicReference<Boolean> propagateInsertCalled, AtomicReference<Boolean> propagateExcludeCalled) {
        Solver cp = sequence.getSolver();

        Constraint cons = new AbstractConstraint(cp) {
            @Override
            public void post() {
                sequence.whenFix(() -> propagateBindCalled.set(true));
                sequence.whenInsert(() -> propagateInsertCalled.set(true));
                sequence.whenExclude(() -> propagateExcludeCalled.set(true));
            }
        };
        AtomicReference<Boolean>[] propagators = new AtomicReference[] {propagateBindCalled, propagateExcludeCalled, propagateInsertCalled};

        cp.post(cons);
        sequence.exclude(3);
        cp.fixPoint();
        assertTrue(propagateExcludeCalled.get());
        assertFalse(propagateBindCalled.get());
        assertFalse(propagateInsertCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        sequence.exclude(2);
        cp.fixPoint();
        assertTrue(propagateExcludeCalled.get());
        assertFalse(propagateBindCalled.get());
        assertFalse(propagateInsertCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        sequence.insert(sequence.begin(), 8); // sequence: begin -> 8 -> end
        cp.fixPoint();
        assertFalse(propagateExcludeCalled.get());
        assertFalse(propagateBindCalled.get());
        assertTrue(propagateInsertCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        sequence.insert(8, 1); // sequence: begin -> 8 -> 1 -> end
        cp.fixPoint();
        assertFalse(propagateExcludeCalled.get());
        assertFalse(propagateBindCalled.get());
        assertTrue(propagateInsertCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        sequence.exclude(0);
        sequence.exclude(4);
        sequence.exclude(5);
        sequence.exclude(7);
        sequence.exclude(9);
        cp.fixPoint();
        assertTrue(propagateExcludeCalled.get());
        assertFalse(propagateBindCalled.get());
        assertFalse(propagateInsertCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        // only node 6 is unassigned at the moment
        cp.getStateManager().saveState();
        sequence.exclude(6);
        cp.fixPoint();
        assertTrue(propagateExcludeCalled.get());
        assertTrue(propagateBindCalled.get());  // no possible node remain
        assertFalse(propagateInsertCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);

        cp.getStateManager().restoreState();
        sequence.insert(sequence.begin(), 6); // sequence: begin -> 6 -> 8 -> 1 -> end
        cp.fixPoint();
        assertFalse(propagateExcludeCalled.get());
        assertTrue(propagateBindCalled.get());  // no possible node remain
        assertTrue(propagateInsertCalled.get());
        for (AtomicReference<Boolean> b : propagators)
            b.set(false);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyDoubleInsert(OldSeqVar sequence) {
        sequence.insert(sequence.begin(), 4);
        sequence.insert(sequence.begin(), 8); // sequence at this point: begin -> 8 -> 4 -> end
        assertThrowsExactly(InconsistencyException.class, () -> sequence.insert(4, 8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwNoInconsistencyDoubleInsert(OldSeqVar sequence) {
        sequence.insert(sequence.begin(), 8);
        sequence.insert(sequence.begin(), 8); // double insertions at the same point are valid
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwNoInconsistencyDoubleExclude(OldSeqVar sequence) {
        sequence.exclude(8);
        sequence.exclude(8);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyExcludeSchedule(OldSeqVar sequence) {
        sequence.exclude(8);
        assertThrowsExactly(InconsistencyException.class, () -> sequence.insert(sequence.begin(), 8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwInconsistencyScheduleExclude(OldSeqVar sequence) {
        sequence.insert(sequence.begin(), 8);
        assertThrowsExactly(InconsistencyException.class, () -> sequence.exclude(8));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void throwAssertionSchedule(OldSeqVar sequence) {
        assertThrowsExactly(InconsistencyException.class, () -> sequence.insert(2, 8));
    }

}
