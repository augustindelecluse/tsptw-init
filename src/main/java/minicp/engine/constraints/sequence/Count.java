package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.OldSeqVar;
import minicp.state.StateTriPartition;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * count the number of specified nodes appearing within a sequence
 */
public class Count extends AbstractConstraint {

    private final OldSeqVar seq;
    private final IntVar cnt;
    private final int[] nodes;

    // nodes that are yet to be seen in the sequence
    // if in S: belong to the sequence. min(count) == nScheduled()
    // if in P: can belong to the sequence. max(count) == nScheduled() + nPossible()
    // if in E: cannot belong to the sequence
    private final StateTriPartition nodesNotSeen;
    private final int[] values;

    /**
     * count the number of specified nodes appearing within the sequence
     * @param seq sequence that needs to be inspected
     * @param cnt counter for the number of nodes that needs to appear in the sequence
     * @param nodes nodes that need to be counted
     */
    public Count(OldSeqVar seq, IntVar cnt, int... nodes) {
        super(seq.getSolver());
        this.seq = seq;
        this.cnt = cnt;
        this.nodes = nodes;
        values = new int[seq.nNode()];
        nodesNotSeen = new StateTriPartition(getSolver().getStateManager(), Arrays.stream(nodes).boxed().collect(Collectors.toSet()));
    }

    @Override
    public void post() {
        cnt.removeAbove(nodes.length); // the count is at most the number of nodes presents
        propagate();
        if (isActive()) {
            cnt.propagateOnBoundChange(this);
            for (int n: nodes) {
                seq.getInsertionVar(n).propagateOnExclude(this);
                seq.getInsertionVar(n).propagateOnInsert(this);
            }
        }
    }

    @Override
    public void propagate() {
        // update the sparse set to count the number of appearance
        int len = nodesNotSeen.fillPossible(values);
        for (int i = 0 ; i < len ; ++i) {
            int node = values[i];
            if (seq.isMember(node))
                nodesNotSeen.include(node);
            else if (seq.isExcluded(node))
                nodesNotSeen.exclude(node);
        }
        // count the number of appearance
        int minCount = nodesNotSeen.nIncluded();
        int maxCount = nodesNotSeen.nPossible() + nodesNotSeen.nIncluded();
        //TODO check

        // update the bounds
        cnt.removeAbove(maxCount);
        cnt.removeBelow(minCount);
        if (cnt.isBound()) {
            if (maxCount != minCount) {
                if (minCount == cnt.min()) { // all possible nodes in nodesNotSeen need to be excluded
                    setActive(false); // do not propagate from itself, the constraint is ended
                    len = nodesNotSeen.fillPossible(values);
                    for (int i = 0 ; i < len ; ++i) {
                        seq.exclude(values[i]);
                    }
                } else { // some possible nodes will need to be removed from the sequence and some to be required
                    ;
                }
            } else { // maxCount == minCount, no remaining node
                setActive(false);
            }
        }
    }
}
