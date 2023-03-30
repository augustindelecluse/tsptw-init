package minicp.engine.constraints.sequence;

import minicp.engine.core.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

public class Disjoint extends AbstractConstraint {

    private final OldSeqVar[] s;
    private final boolean mustAppear;
    int nNodes;

    /**
     * disjoint constraint. Ensures that the same node id must be scheduled once and only once across all sequences
     * a node can be excluded from all sequences
     * @param s array of SequenceVar to post the constraint on
     */
    public Disjoint(OldSeqVar... s) {
        this(true, s);
    }

    /**
     * disjoint constraint. Ensures that the same node id must be scheduled once and only once across all sequences
     * @param s array of SequenceVar to post the constraint on
     * @param mustAppear if true, each unique node must be scheduled in one sequence
     */
    public Disjoint(boolean mustAppear, OldSeqVar... s) {
        super(s[0].getSolver());
        this.s = s;
        this.mustAppear = mustAppear;
        nNodes = Arrays.stream(s).mapToInt(OldSeqVar::nNode).max().getAsInt();
    }

    @Override
    public void post() {
        for (int n = 0 ; n < nNodes ; ++n) {
            new DisjointOnOneNode(n).post();
        }
    }
    
    private class DisjointOnOneNode extends AbstractConstraint {
        
        private final int node;
        
        public DisjointOnOneNode(int node) {
            super(s[0].getSolver());
            this.node = node;
        }

        @Override
        public void post() {
            propagate();
            if (isActive()) {
                for (OldSeqVar seq : s) {
                    seq.getInsertionVar(node).propagateOnInsert(this);
                    seq.getInsertionVar(node).propagateOnExclude(this);
                }
            }
        }

        @Override
        public void propagate() {
            OldSeqVar seq = null;  // can be member in only one sequence
            boolean excludedFromAll = true;
            for (OldSeqVar candidate: s) {
                if (candidate.isMember(node)) {
                    if (seq != null)
                        throw INCONSISTENCY; // appears in two sequences
                    seq = candidate; // sequence where the node appears
                    excludedFromAll = false;
                } else {
                    excludedFromAll = excludedFromAll && candidate.isExcluded(node);
                }
            }
            if (seq != null) {
                // appears in exactly one sequence
                for (OldSeqVar candidate: s) {
                    if (candidate != seq)
                        candidate.exclude(node); // exclude from the other sequences
                }
                setActive(false); // nothing to do anymore
            } else if (excludedFromAll && mustAppear) {
                throw INCONSISTENCY; // excluded from all sequences but mut be present in one of them
            }
        }

    }
}
