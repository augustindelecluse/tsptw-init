package minicp.engine.constraints.sequence;

import minicp.cp.Factory;
import minicp.engine.SolverTest;
import minicp.engine.core.*;
import minicp.state.StateManager;
import minicp.util.exception.InconsistencyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


public class TransitionTimesTest extends SolverTest {
    
    static int nNodes = 6;
    static int begin = 4;
    static int end = 5;
    static int[][] transitions = new int[][] {
            {0, 3, 5, 4, 4, 4},
            {3, 0, 4, 5, 5, 5},
            {5, 4, 0, 3, 9, 9},
            {4, 5, 3, 0, 8, 8},
            {4, 5, 9, 8, 0, 0},
            {4, 5, 9, 8, 0, 0},
    };
    static int[] serviceTime = new int[] {5, 5, 5, 5, 0, 0};

    private static Stream<Arguments> seqVar() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            return Arguments.of(cp, Factory.makeSequenceVar(cp, nNodes, begin, end));
        });
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testOneNodeReachable(Solver cp, OldSeqVar sequence) {
        StateManager sm = cp.getStateManager();
        IntVar[] time = new IntVar[] {
                Factory.makeIntVar(cp, 0, 10),
                Factory.makeIntVar(cp, 0, 0),
                Factory.makeIntVar(cp, 0, 0),
                Factory.makeIntVar(cp, 0, 0),
                Factory.makeIntVar(cp, 0, 0),
                Factory.makeIntVar(cp, 100, 200),
        };
        // only the node 0 is reachable with those time available
        cp.post(new TransitionTimes(sequence, time, transitions, serviceTime));

        int[][] scheduledInsertion1 = new int[][] {
                {sequence.begin()},
                {},
                {},
                {},
                {}, // begin has no insertion
                {}  // end has no insertion
        };
        int[][] possibleInsertions1 = new int[][] {
                {},
                {},
                {},
                {},
                {}, // begin has no insertion
                {}  // end has no insertion
        };
        int[] scheduled1 = new int[] {begin, end};
        int[] possible1= new int[] {0};
        int[] excluded1 = new int[] {1, 2, 3};

        OldSeqVarAssertion.isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertion1, possibleInsertions1);
        sm.saveState();
        sequence.insert(sequence.begin(), 0);
        cp.fixPoint();

        int[][] scheduledInsertion2 = new int[][] {
                {},
                {},
                {},
                {},
                {}, // begin has no insertion
                {}  // end has no insertion
        };
        int[] scheduled2 = new int[] {begin, 0, end};
        int[] possible2= new int[] {};
        assertEquals(4, time[0].min());
        assertEquals(10, time[0].max());
        assertEquals(100, time[5].min()); // end node is not affected by the changes in this case
        assertEquals(200, time[5].max());
        OldSeqVarAssertion.isSequenceValid(sequence, scheduled2, possible2, excluded1, scheduledInsertion2, possibleInsertions1);
        sm.restoreState();
        OldSeqVarAssertion.isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertion1, possibleInsertions1);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testOneNodeUnreachable(Solver cp, OldSeqVar sequence) {
        StateManager sm = cp.getStateManager();
        IntVar[] time = new IntVar[] {
                Factory.makeIntVar(cp, 0, 20),
                Factory.makeIntVar(cp, 12, 16),
                Factory.makeIntVar(cp, 0, 20),
                Factory.makeIntVar(cp, 4, 7), // node 3 unreachable from the sequence (distance from start: 8)
                Factory.makeIntVar(cp, 0, 20),
                Factory.makeIntVar(cp, 100, 200),
        };
        cp.post(new TransitionTimes(sequence, time, transitions, serviceTime));

        int[][] scheduledInsertion1 = new int[][] {
                {sequence.begin()},
                {sequence.begin()},
                {sequence.begin()},
                {},
                {}, // begin has no insertion
                {}  // end has no insertion
        };
        int[][] possibleInsertions1 = new int[][] {
                {  1, 2},
                {0,   2},
                {0,    }, // node 1 cannot be predecessor because of time window violation
                {},
                {}, // begin has no insertion
                {}  // end has no insertion
        };
        int[] scheduled1 = new int[] {begin, end};
        int[] possible1= new int[] {0, 1, 2};
        int[] excluded1 = new int[] {3};

        OldSeqVarAssertion.isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertion1, possibleInsertions1);
        sm.saveState();
        sequence.insert(sequence.begin(), 0);
        sequence.insert(0, 2);
        cp.fixPoint();
        // node 1 is now unreachable
        int[][] scheduledInsertion2 = new int[][] {
                {},
                {},
                {},
                {},
                {}, // begin has no insertion
                {}  // end has no insertion
        };
        int[][] possibleInsertions2 = new int[][] {
                {},
                {},
                {},
                {},
                {}, // begin has no insertion
                {}  // end has no insertion
        };
        int[] scheduled2 = new int[] {begin, 0, 2, end};
        int[] possible2 = new int[] {};
        int[] excluded2 = new int[] {1, 3};

        /* sequence at this point: begin -> 0 -> 2 -> 5
        initial time windows:
        begin:  0..20
        0:      0..20
        2:      0..20
        end:    100..200
         */

        // check for updates in time windows
        assertEquals(0, time[begin].min()); // min departure time remains unchanged
        assertEquals(4, time[0].min());
        assertEquals(14, time[2].min());
        assertEquals(100, time[end].min()); // end node is not affected by the changes in this case

        assertEquals(200, time[end].max());
        assertEquals(20, time[2].max());
        assertEquals(10, time[0].max()); // reduced as node 2 must be reachable from here
        assertEquals(6, time[begin].max()); // max departure time must allow reaching node 0

        // excluded nodes should not have their time window updated
        assertEquals(12, time[1].min());
        assertEquals(16, time[1].max());
        assertEquals(4, time[3].min());
        assertEquals(7, time[3].max());

        OldSeqVarAssertion.isSequenceValid(sequence, scheduled2, possible2, excluded2, scheduledInsertion2, possibleInsertions2);
        sm.restoreState();
        OldSeqVarAssertion.isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertion1, possibleInsertions1);
    }

    // assign a route that would violate the transition time
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testUnfeasibleTransitions(Solver cp, OldSeqVar sequence) {
        IntVar[] time = new IntVar[] {
                Factory.makeIntVar(cp, 0, 20),
                Factory.makeIntVar(cp, 12, 16),
                Factory.makeIntVar(cp, 0, 20),
                Factory.makeIntVar(cp, 4, 7), // node 3 unreachable from the sequence (distance from start: 8)
                Factory.makeIntVar(cp, 0, 20),
                Factory.makeIntVar(cp, 100, 200),
        };
        sequence.insert(begin, 3);
        try {
            cp.post(new TransitionTimes(sequence, time, transitions, serviceTime));
            fail("should fail");
        } catch (InconsistencyException e) {}
    }
}
