package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.OldSeqVar;

/**
 * exclude all possible nodes of the sequence, binding it
 */
public class ExcludeAllPossible extends AbstractConstraint {

    OldSeqVar[] seqVars;

    /**
     * exclude all possible nodes from the sequence
     * @param seqVar sequence var whose possible nodes will be removed
     */
    public ExcludeAllPossible(OldSeqVar... seqVar) {
        super(seqVar[0].getSolver());
        this.seqVars = seqVar;
    }

    @Override
    public void post() {
        for (OldSeqVar seq: seqVars)
            seq.excludeAllPossible();
        setActive(false);
    }
}
