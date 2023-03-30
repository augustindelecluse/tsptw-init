package minicp.engine.constraints.sequence;

import minicp.cp.Factory;
import minicp.engine.SolverTest;
import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.OldSeqVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.state.StateManager;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static minicp.cp.BranchingScheme.EMPTY;
import static minicp.cp.Factory.makeDfs;
import static minicp.util.exception.InconsistencyException.INCONSISTENCY;
import static org.junit.jupiter.api.Assertions.*;

public class DisjointTest extends SolverTest {

    static int nSequences = 5;
    static int nNodes = 10;

    private static Stream<Arguments> seqVar() {
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            OldSeqVar[] SeqVars = new OldSeqVar[nSequences];
            for (int i = 0; i < nSequences ; ++i) {
                SeqVars[i] = Factory.makeSequenceVar(cp, nNodes+2*nSequences, nNodes+i, nNodes + nSequences + i);
            }
            return Arguments.of(cp, SeqVars);
        });
    }

    private static Stream<Arguments> aFewSeqVars() {
        int nSequences = 2;
        int nNodes = 6;
        return solver().map(s -> {
            Solver cp = (Solver) s.get()[0];
            OldSeqVar[] SeqVars = new OldSeqVar[nSequences];
            for (int i = 0; i < nSequences ; ++i) {
                SeqVars[i] = Factory.makeSequenceVar(cp, nNodes+2*nSequences, nNodes+i, nNodes + nSequences + i);
            }
            return Arguments.of(cp, SeqVars);
        });
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testDisjoint1(Solver cp, OldSeqVar[] SeqVars) {
        for (int i = 0; i < nSequences ; ++i) {
            SeqVars[i] = Factory.makeSequenceVar(cp, nNodes+2*nSequences, nNodes+i, nNodes + nSequences + i);
        }
        cp.post(new Disjoint(SeqVars));
        // no modifications should have occurred at the moment

        SeqVars[0].insert(SeqVars[0].begin(), 5);
        cp.fixPoint();
        // node 5 should be excluded from all others sequences
        for (int i = 1; i < nSequences ; ++i) {
            assertTrue(SeqVars[i].isExcluded(5));
        }

        SeqVars[1].insert(SeqVars[1].begin(), 2);
        cp.fixPoint();
        // node 2 should be excluded from all others sequences
        for (int i = 0; i < nSequences ; ++i) {
            if (i != 1)
                assertTrue(SeqVars[i].isExcluded(2));
        }

        SeqVars[4].insert(SeqVars[4].begin(), 8);
        cp.fixPoint();
        // node 2 should be excluded from all others sequences
        for (int i = 0; i < nSequences ; ++i) {
            if (i != 4)
                assertTrue(SeqVars[i].isExcluded(8));
        }

    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testDisjoint2(Solver cp, OldSeqVar[] SeqVars) {
        StateManager sm = cp.getStateManager();
        // a node cannot be excluded from all sequences
        sm.saveState();
        cp.post(new Disjoint(true, SeqVars));
        OldSeqVar chosen = SeqVars[new Random().nextInt(SeqVars.length)];
        for (OldSeqVar s: SeqVars) {
            if (s != chosen) {
                s.exclude(3);
                try {
                    cp.fixPoint();
                } catch (InconsistencyException e) {
                    fail("failed to exclude a node from a sequence when it was still possible in another sequence");
                }
            }
        }
        cp.getStateManager().saveState();
        try {
            chosen.exclude(3);
            cp.fixPoint();
            fail("a node was excluded from all sequences without raising an inconsistency");
        } catch (InconsistencyException e) {

        }
        cp.getStateManager().restoreState();
        try {
            chosen.insert(chosen.begin(), 3);
        } catch (InconsistencyException e) {
            fail("failed to schedule a node when it was possible");
        }
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testDisjoint3(Solver cp, OldSeqVar[] SeqVars) {
        // cannot schedule a node in more than 2 sequences
        cp.post(new Disjoint(SeqVars));
        if (SeqVars.length <= 1)
            return;
        Random random = new Random();
        int i = random.nextInt(SeqVars.length);
        int j = random.nextInt(SeqVars.length);
        while (j == i) {
            j = random.nextInt(SeqVars.length);
        }
        int node = random.nextInt(nNodes);
        SeqVars[i].insert(SeqVars[i].begin(), node);
        SeqVars[j].insert(SeqVars[j].begin(), node);
        assertThrowsExactly(InconsistencyException.class, cp::fixPoint);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testDisjointOneSequence1(Solver cp, OldSeqVar[] SeqVars) {
        SeqVars[0].exclude(2);
        assertThrows(InconsistencyException.class, () -> cp.post(new Disjoint(SeqVars[0])));
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testDisjointOneSequence2(Solver cp, OldSeqVar[] SeqVars) {
        try {
            cp.post(new Disjoint(SeqVars[0]));
        } catch (InconsistencyException e) {
            fail("inconsistency should not be thrown when no node is excluded");
        }
        SeqVars[0].exclude(2);
        assertThrowsExactly(InconsistencyException.class, cp::fixPoint);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testExcludeInMultipleSequence(Solver cp, OldSeqVar[] SeqVars) {
        try {
            cp.post(new Disjoint(SeqVars));
        } catch (InconsistencyException e) {

            fail("inconsistency should not be thrown when no node is excluded");
        }
        for (OldSeqVar seq : SeqVars) {
            seq.exclude(2);
        }
        assertThrowsExactly(InconsistencyException.class, cp::fixPoint);
    }

    @ParameterizedTest
    @MethodSource("seqVar")
    public void testDisjointAfterExclusion(Solver cp, OldSeqVar[] SeqVars) {
        for (OldSeqVar seq : SeqVars) {
            seq.exclude(2);
        }
        assertThrowsExactly(InconsistencyException.class, () -> cp.post(new Disjoint(SeqVars)));
    }

    @ParameterizedTest
    @MethodSource("aFewSeqVars")
    public void testSameSolAsLazyConstraint(Solver cp, OldSeqVar[] SeqVars) {
        int nNodes = 0;
        for (OldSeqVar s: SeqVars) {
            nNodes = Math.max(s.end(), Math.max(s.begin(), s.nNode()));
        }
        cp.getStateManager().saveState(); // post temporarily the constraint
        cp.post(new Disjoint(SeqVars));
        DFSearch search1 = makeDfs(cp, branchOnNode(SeqVars, nNodes));
        SearchStatistics statsDisjoint = search1.solve();
        cp.getStateManager().saveState(); // removes the constraint

        cp.post(new LazyDisjoint(nNodes, SeqVars));
        DFSearch search2 = makeDfs(cp, branchOnNode(SeqVars, nNodes));
        SearchStatistics statsLazy = search2.solve();
        assertEquals(statsDisjoint.numberOfSolutions(), statsLazy.numberOfSolutions());
    }

    /**
     * A default search for problems with multiple sequences
     */
    private Supplier<Procedure[]> branchOnNode(OldSeqVar[] route, int nNodes) {
        int maxNodes = Arrays.stream(route).mapToInt(OldSeqVar::nNode).max().getAsInt();
        int[] pred = new int[maxNodes];
        return () -> {
            boolean oneUnfixed = Arrays.stream(route).map(r -> !r.isFixed()).findAny().get();
            if (!oneUnfixed)
                return EMPTY;
            // get the non-inserted node with the least number of insertions
            int bestNode = -1;
            int minInsert = Integer.MAX_VALUE;
            for (int node = 0 ; node < nNodes ; ++node) {
                int nInsert = 0;
                for (OldSeqVar r : route) {
                    nInsert += r.nMemberPredInsert(node);
                }
                if (nInsert != 0 && nInsert < minInsert) {
                    bestNode = node;
                    minInsert = nInsert;
                }
            }
            if (bestNode == -1) {
                // failed to insert a node
                throw new InconsistencyException();
            }
            // insert the node at every position in every route
            int branchingNode = bestNode;
            Procedure[] branching = new Procedure[minInsert];
            int i = 0 ;
            for (OldSeqVar r : route) {
                int nPred = r.fillMemberPredInsert(bestNode, pred);
                for (int p = 0 ; p < nPred ; ++p) {
                    int predecessor = pred[p];
                    branching[i++] = () -> {
                        r.getSolver().post(new Insert(r, predecessor, branchingNode));
                    };
                }
            }
            assertEquals(minInsert, i);
            assertNotEquals(0, branching.length);
            return branching;
        };
    }

    /**
     * Disjoint constraint only evaluated when all Sequences are fixed
     */
    private class LazyDisjoint extends AbstractConstraint {

        OldSeqVar[] seqVars;
        Set<Integer> visited = new HashSet<>();
        int[] nodes;
        int nVars;

        public LazyDisjoint(int nVars, OldSeqVar... seqs) {
            super(seqs[0].getSolver());
            this.seqVars = seqs;
            nodes = new int[seqs[0].nNode()];
            this.nVars = nVars;
        }

        @Override
        public void post() {
            for (OldSeqVar s: seqVars)
                s.propagateOnFix(this);
        }

        @Override
        public void propagate() {
            for (OldSeqVar s: seqVars)
                if (!s.isFixed())
                    return;
            visited.clear();
            for (OldSeqVar s: seqVars) {
                int n = s.fillMember(nodes);
                int oldSize = visited.size();
                for (int i = 0 ; i < n ; ++i) {
                    visited.add(nodes[i]);
                }
                if (visited.size() != oldSize + n)
                    throw INCONSISTENCY; // node covered by 2 sequences
            }
            if (visited.size() != nVars) // not all nodes have been covered
                throw INCONSISTENCY;
        }
    }

}
