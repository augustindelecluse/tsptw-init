package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.OldSeqVar;

/**
 * remove all pair of (node, predecessor) from the insertion where predecessor is a specified node
 */
public class NoEdgeFrom extends AbstractConstraint {

    private final int node;
    private final OldSeqVar seq;

    public NoEdgeFrom(OldSeqVar seq, int invalidPredecessor) {
        super(seq.getSolver());
        this.seq = seq;
        this.node = invalidPredecessor;
    }

    @Override
    public void post() {
        seq.removeAllPredInsertFrom(node);
        setActive(false);
    }
}
