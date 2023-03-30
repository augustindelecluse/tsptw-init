package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.OldSeqVar;
import minicp.util.exception.InconsistencyException;

/**
 * count the number of member nodes in a sequence
 */
public class NMember extends AbstractConstraint {

    private final OldSeqVar seq;
    private final IntVar nNodes;
    private final int nNodesInt;
    private final boolean includeBounds;

    /**
     * ensure that a sequence visits nNodes
     * @param nNodes number of nodes visited by the sequence
     * @param seq sequence whose nodes will be visited
     * @param includeBounds if the beginning and ending nodes must be counted or not
     */
    public NMember(OldSeqVar seq, IntVar nNodes, boolean includeBounds) {
        this(seq, nNodes, 0, includeBounds);
    }

    /**
     * ensure that a sequence visits nNodes
     * @param nNodes number of nodes visited by the sequence
     * @param seq sequence whose nodes will be visited
     */
    public NMember(OldSeqVar seq, IntVar nNodes) {
        this(seq, nNodes, 0, true);
    }

    /**
     * ensure that a sequence visits nNodes
     * @param nNodes number of nodes visited by the sequence
     * @param seq sequence whose noes will be visited
     */
    public NMember(OldSeqVar seq, int nNodes) {
        this(seq, null, nNodes, true);
    }

    private NMember(OldSeqVar seq, IntVar nNodes, int nNodesInt, boolean includeBounds) {
        super(seq.getSolver());
        this.nNodes = nNodes;
        this.seq = seq;
        this.nNodesInt = nNodesInt;
        this.includeBounds = includeBounds;
    }

    @Override
    public void post() {
        propagate();
        if (isActive()) {
            if (nNodes != null)
                nNodes.propagateOnBoundChange(this);
            seq.propagateOnInsert(this);
            seq.propagateOnExclude(this);
        }
    }

    @Override
    public void propagate() {
        int nMember = seq.nMember(includeBounds);
        int nPossibleNode = seq.nPossible();
        if (nNodes != null) { // using an IntVar
            nNodes.removeBelow(nMember); // least number of nodes in the sequence
            nNodes.removeAbove(nMember + nPossibleNode); // highest number of nodes that can belong to the sequence
            // if nNodes is decided and corresponds to the number of scheduled nodes ...
            if (nNodes.isBound() && !seq.isFixed() && nMember == nNodes.min()) {
                seq.excludeAllPossible(); // ... exclude all possible nodes
                setActive(false);
            }
        } else { // using an integer
            if (nMember > nNodesInt) // exceeding the maximum number of nodes
                throw new InconsistencyException();
            else if (nMember == nNodesInt) { // maximum number of nodes reached
                seq.excludeAllPossible();
                setActive(false);
            }
        }
    }
}
