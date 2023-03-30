package minicp.engine.constraints.sequence;

import minicp.cp.Factory;
import minicp.engine.SolverTest;
import minicp.engine.core.OldSeqVar;
import minicp.engine.core.Solver;
import minicp.state.StateManager;
import minicp.util.exception.InconsistencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CumulativeTest extends SolverTest {

    static int nNodes = 10;
    static int begin = 8;
    static int end = 9;

    private static Stream<Arguments> seqVar() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            return Arguments.of(cp, Factory.makeSequenceVar(cp, nNodes, begin, end));
        });
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testInitCumulative(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {1, 1, -1, -1, 0, 0, 0, 0};
        try {
            cp.post(new Cumulative(sequence, new int[] {0, 1}, new int[] {2, 3}, 2, capacity));
        } catch (InconsistencyException e) {
            fail("should not fail");
        }
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testFeasibleSequence(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Insert(sequence, sequence.begin(), 2));
        cp.post(new Insert(sequence, 2, 4));
        cp.post(new Insert(sequence, 4, 3));
        cp.post(new Insert(sequence, 3, 1));
        cp.post(new Insert(sequence, 1, 7));
        cp.post(new Insert(sequence, 7, 5));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testNoInsertForStart(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 3, capacity));
        cp.post(new Insert(sequence, sequence.begin(), p[0]));
        cp.post(new Insert(sequence, p[0], p[2]));
        cp.post(new Insert(sequence, p[2], d[2]));
        cp.post(new Insert(sequence, d[2], d[1]));
        // sequence at this point: begin -> p0   -> p2   -> d2   -> d1   -> end
        // capacity:                0, 0    0, 2    0, 1    1, 0    2, 0    0, 0
        assertTrue(sequence.isPredInsert(p[0], p[1]));
        assertTrue(sequence.isPredInsert(p[0], p[3]));
        assertTrue(sequence.isPredInsert(p[0], d[3]));
    }

    /**
     * test if an activity with no inserted part has some insertions points removed
     */
    @ParameterizedTest
    @MethodSource("seqVar")
    public void testNotInsertedActivity(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Insert(sequence, sequence.begin(), 2));
        cp.post(new Insert(sequence, 2, 0));
        cp.post(new Insert(sequence, 0, 4));
        cp.post(new Insert(sequence, 4, 6)); // sequence: begin -> 2 -> 0 -> 4 -> 6 -> end
        int start = 1;
        int end = 5;
        assertTrue(sequence.isPredInsert(sequence.begin(), start));
        assertTrue(sequence.isPredInsert(2, start));
        assertFalse(sequence.isPredInsert(0, start));
        assertTrue(sequence.isPredInsert(4, start));
        assertTrue(sequence.isPredInsert(6, start));

        assertTrue(sequence.isPredInsert(sequence.begin(), end));
        assertTrue(sequence.isPredInsert(2, end));
        assertFalse(sequence.isPredInsert(0, end));
        assertTrue(sequence.isPredInsert(4, end));
        assertTrue(sequence.isPredInsert(6, end));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testUpdateEnd(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Insert(sequence, sequence.begin(), 1));
        cp.post(new Insert(sequence, 1, 2));
        cp.post(new Insert(sequence, 2, 0));
        cp.post(new Insert(sequence, 0, 4));
        cp.post(new Insert(sequence, 4, 6)); // sequence: begin -> 1 -> 2 -> 0 -> 4 -> 6 -> end

        int end = 5; // end for start == 1
        assertFalse(sequence.isPredInsert(sequence.begin(), end)); // cannot schedule end before start
        assertTrue(sequence.isPredInsert(1, end));
        assertTrue(sequence.isPredInsert(2, end));
        assertFalse(sequence.isPredInsert(0, end)); // from this point, the end exceeds the max capacity
        assertFalse(sequence.isPredInsert(4, end));
        assertFalse(sequence.isPredInsert(6, end));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testRemoveIntermediate(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Insert(sequence, sequence.begin(), 2));
        cp.post(new Insert(sequence, 2, 0));
        cp.post(new Insert(sequence, 0, 4));
        cp.post(new Insert(sequence, 4, 3));
        cp.post(new Insert(sequence, 3, 7));
        cp.post(new Insert(sequence, 7, 6)); // sequence: begin -> 2 -> 0 -> 4 -> 3 -> 7 -> 6 -> end

        int start = 1;
        int end = 5;
        assertTrue(sequence.isPredInsert(sequence.begin(), start));
        assertTrue(sequence.isPredInsert(2, start));  // capacity: 1
        assertFalse(sequence.isPredInsert(0, start)); // capacity: 3
        assertTrue(sequence.isPredInsert(4, start));  // capacity: 1
        assertFalse(sequence.isPredInsert(3, start)); // capacity: 2
        assertTrue(sequence.isPredInsert(7, start));  // capacity: 1
        assertTrue(sequence.isPredInsert(6, start));  // capacity: 0

        assertTrue(sequence.isPredInsert(sequence.begin(), end));
        assertTrue(sequence.isPredInsert(2, end));    // capacity: 1
        assertFalse(sequence.isPredInsert(0, end));   // capacity: 3
        assertTrue(sequence.isPredInsert(4, end));    // capacity: 1
        assertFalse(sequence.isPredInsert(3, end));   // capacity: 2
        assertTrue(sequence.isPredInsert(7, end));    // capacity: 1
        assertTrue(sequence.isPredInsert(6, end));    // capacity: 0

        try {
            cp.post(new Insert(sequence, 3, 1));
            fail();
        } catch (InconsistencyException e) {
            ;
        }
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testPartiallyInsert(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Insert(sequence, sequence.begin(), 2));
        cp.post(new Insert(sequence, 2, 3));
        cp.post(new Insert(sequence, 2, 4));
        cp.post(new Insert(sequence, 2, 5));
        cp.post(new Insert(sequence, 2, 6));
        cp.post(new Insert(sequence, 3, 7)); // sequence: begin -> 2 -> 6 -> 5 -> 4 -> 3 -> 7 -> end
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void removeDropInsertion(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {1, 1, 1, 1, -1, -1, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        Cumulative Cumulative = new Cumulative(sequence, p, d, 2, capacity);
        cp.post(Cumulative);
        cp.post(new Insert(sequence, sequence.begin(), p[0]));
        cp.post(new Insert(sequence, p[0], p[1]));
        cp.post(new Insert(sequence, p[1], p[2]));
        cp.post(new Insert(sequence, p[2], d[0]));
        // sequence:   begin -> 0 (p0) -> 1 (p1) -> 2 (p2) -> 4 (d0) -> end
        // capacity:   0, 0     0, 1      1, 2      1, 2      1, 0
        // partially inserted
        assertFalse(sequence.isPredInsert(p[2], d[1]));
        assertTrue(sequence.isPredInsert(p[1], d[1]));

        // not inserted: pickup
        assertTrue(sequence.isPredInsert(sequence.begin(), p[3]));
        assertTrue(sequence.isPredInsert(p[0], p[3]));
        assertTrue(sequence.isPredInsert(p[1], p[3]));
        assertTrue(sequence.isPredInsert(p[2], p[3]));
        assertTrue(sequence.isPredInsert(d[0], p[3]));

        // not inserted: drop
        assertTrue(sequence.isPredInsert(sequence.begin(), d[3]));
        assertTrue(sequence.isPredInsert(p[0], d[3]));
        assertTrue(sequence.isPredInsert(p[1], d[3]));
        assertTrue(sequence.isPredInsert(p[2], d[3]));
        assertTrue(sequence.isPredInsert(d[0], d[3]));
    }


    @ParameterizedTest
    @MethodSource("seqVar")
    public void removeNotInserted(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        Cumulative Cumulative = new Cumulative(sequence, p, d, 3, capacity);
        cp.post(Cumulative);
        cp.post(new Insert(sequence, sequence.begin(), p[0]));
        cp.post(new Insert(sequence, p[0], p[2]));
        cp.post(new Insert(sequence, p[2], d[2]));
        cp.post(new Insert(sequence, d[2], d[0]));
        // sequence:   begin -> (p0) -> (p2) -> 2 (d2) -> 4 (d0) -> end
        // sequence: capacity:   2       3         2         0
        // not inserted: drop
        assertTrue(sequence.isPredInsert(sequence.begin(), d[1]));
        assertFalse(sequence.isPredInsert(p[0], d[1]));
        assertFalse(sequence.isPredInsert(p[2], d[1]));
        assertFalse(sequence.isPredInsert(d[2], d[1]));
        assertTrue(sequence.isPredInsert(d[0], d[1]));
        // not inserted: pickup
        assertTrue(sequence.isPredInsert(sequence.begin(), p[1]));
        assertFalse(sequence.isPredInsert(p[0], p[1]));
        assertFalse(sequence.isPredInsert(p[2], p[1]));
        assertFalse(sequence.isPredInsert(d[2], p[1]));
        assertTrue(sequence.isPredInsert(d[0], p[1]));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void SeveralPartiallyInserted(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 3, capacity));
        cp.post(new Insert(sequence, sequence.begin(), p[2]));
        cp.post(new Insert(sequence, p[2], p[3]));
        cp.post(new Insert(sequence, p[2], d[0]));
        cp.post(new Insert(sequence, p[3], d[1]));
        cp.post(new Insert(sequence, d[1], d[2])); // begin -> p2 -> d0 -> p3 -> d1 -> d2 -> end
        cp.post(new Insert(sequence, p[2], p[0])); // begin -> p2 -> p0 -> d0 -> p3 -> d1 -> d2 -> end
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void MultipleDropPartiallyInserted(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {1, 1, 1, 1, -1, -1, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 1, capacity));
        cp.post(new Insert(sequence, sequence.begin(), d[0]));
        cp.post(new Insert(sequence, d[0], d[1]));
        cp.post(new Insert(sequence, d[1], d[2]));
        cp.post(new Insert(sequence, d[2], d[3])); // begin -> d0 -> d1 -> d2 -> d3 -> end
        assertFalse(sequence.isPredInsert(sequence.begin(), p[1]));
        assertFalse(sequence.isPredInsert(sequence.begin(), p[2]));
        assertFalse(sequence.isPredInsert(d[0], p[2]));
        assertFalse(sequence.isPredInsert(sequence.begin(), p[3]));
        assertFalse(sequence.isPredInsert(d[0], p[3]));
        assertFalse(sequence.isPredInsert(d[1], p[3]));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void MultiplePickupPartiallyInserted(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {1, 1, 1, 1, -1, -1, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 1, capacity));
        cp.post(new Insert(sequence, sequence.begin(), p[0]));
        cp.post(new Insert(sequence, p[0], p[1]));
        cp.post(new Insert(sequence, p[1], p[2]));
        cp.post(new Insert(sequence, p[2], p[3])); // begin -> p0 -> p1 -> p2 -> p3 -> end
        assertFalse(sequence.isPredInsert(p[1], d[0]));
        assertFalse(sequence.isPredInsert(p[2], d[1]));
        assertFalse(sequence.isPredInsert(p[3], d[2]));
        assertTrue(sequence.isPredInsert(p[0], d[0]));
        assertTrue(sequence.isPredInsert(p[1], d[1]));
        assertTrue(sequence.isPredInsert(p[2], d[2]));
        assertTrue(sequence.isPredInsert(p[3], d[3]));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void MultiplePartiallyInserted(Solver cp, OldSeqVar sequence) {
        int[] capacity = new int[] {1, 1, 1, 1, -1, -1, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 1, capacity));
        cp.post(new Insert(sequence, sequence.begin(), d[0]));
        cp.post(new Insert(sequence, d[0], p[1]));
        cp.post(new Insert(sequence, p[1], d[2]));
        cp.post(new Insert(sequence, d[2], p[3])); // begin -> d0 -> p1 -> d2 -> p3 -> end

        // insertions for p0
        assertTrue(sequence.isPredInsert(sequence.begin(), p[0]));
        assertFalse(sequence.isPredInsert(d[0], p[0]));
        assertFalse(sequence.isPredInsert(p[1], p[0]));
        assertFalse(sequence.isPredInsert(d[2], p[0]));
        assertFalse(sequence.isPredInsert(p[3], p[0]));

        // insertions for d1
        assertFalse(sequence.isPredInsert(sequence.begin(), d[1]));
        assertFalse(sequence.isPredInsert(d[0], d[1]));
        assertTrue(sequence.isPredInsert(p[1], d[1]));
        assertFalse(sequence.isPredInsert(d[2], d[1]));
        assertFalse(sequence.isPredInsert(p[3], d[1]));

        // insertions for p2
        assertFalse(sequence.isPredInsert(sequence.begin(), p[2]));
        assertFalse(sequence.isPredInsert(d[0], p[2]));
        assertTrue(sequence.isPredInsert(p[1], p[2]));
        assertFalse(sequence.isPredInsert(d[2], p[2]));
        assertFalse(sequence.isPredInsert(p[3], p[2]));

        // insertions for d3
        assertFalse(sequence.isPredInsert(sequence.begin(), d[3]));
        assertFalse(sequence.isPredInsert(d[0], d[3]));
        assertFalse(sequence.isPredInsert(p[1], d[3]));
        assertFalse(sequence.isPredInsert(d[2], d[3]));
        assertTrue(sequence.isPredInsert(p[3], d[3]));
    }

}
