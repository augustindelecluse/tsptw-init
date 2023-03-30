package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.OldSeqVar;
import minicp.util.exception.InconsistencyException;

/**
 * exclude a node from a sequence
 */
public class Exclude extends AbstractConstraint {

    private final int[] excluded;
    private final OldSeqVar seqVar;

    /**
     * exclusion constraint. Ensure that a node / set of nodes must belong to the set of excluded nodes
     * @param seqVar: sequence var to work on
     * @param nodes : node / set of nodes to exclude
     */
    public Exclude(OldSeqVar seqVar, int... nodes) {
        super(seqVar.getSolver());
        excluded = nodes;
        this.seqVar = seqVar;
    }

    @Override
    public void post() {
        for (int node: excluded) {
            if (seqVar.isMember(node))
                throw InconsistencyException.INCONSISTENCY;
            seqVar.exclude(node);
        }
        //setActive(false);  not needed as the constraint is not registered for propagation
    }
}
