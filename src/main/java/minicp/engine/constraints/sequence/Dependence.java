package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.OldSeqVar;
import minicp.util.exception.InconsistencyException;

import java.util.Arrays;

/**
 * ensure that a set of nodes are all present or all absent from a sequence
 */
public class Dependence extends AbstractConstraint {

    private OldSeqVar seqVar;
    private int[] dependent;
    private int[] insertions;

    /**
     * ensures that a series of nodes are all present in the sequence or all absent
     * @param seq SequenceVar to post the constraint on
     * @param dependent dependent nodes in the sequence
     */
    public Dependence(OldSeqVar seq, int... dependent) {
        super(seq.getSolver());
        this.seqVar = seq;
        this.dependent = dependent;
        Arrays.sort(this.dependent);
        insertions = new int[seq.nNode()];
    }

    @Override
    public void post() {
        for (int i: dependent) {
            if (seqVar.isExcluded(i))
                propagate(); // exclude all the nodes
            seqVar.getInsertionVar(i).propagateOnExclude(this);
        }
    }

    /**
     * exclude all dependent insertions in the sequence
     */
    @Override
    public void propagate() {
        for (int i: dependent) {
            if (seqVar.isPossible(i)) {
                seqVar.exclude(i);
            } else if (seqVar.isMember(i)) {
                throw new InconsistencyException(); // an insertion cannot be scheduled at this point
            }
        }
        setActive(false);
    }

}
