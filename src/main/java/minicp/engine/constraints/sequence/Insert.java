package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.OldSeqVar;

public class Insert extends AbstractConstraint {

    private OldSeqVar seqVar;
    private int node;
    private int predecessor;

    public Insert(OldSeqVar seqVar, int pred, int node) {
        super(seqVar.getSolver());
        this.seqVar = seqVar;
        this.node = node;
        this.predecessor = pred;
    }

    @Override
    public void post() {
        seqVar.insert(predecessor, node);
        // setActive(false); not needed as the constraint is not registered for propagation
    }
}
