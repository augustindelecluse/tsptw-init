package minicp.engine.core;

import minicp.state.StateInt;
import minicp.state.StateTriPartition;
import minicp.state.StateSparseSet;
import minicp.state.StateStack;
import minicp.util.Procedure;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

/**
 * A {@link OldSeqVar} implementation
 */
public class OldSeqVarImpl implements OldSeqVar {

    private final Solver cp;
    private final int nNodes;                   // number of nodes available (omitting begin and end)
    private OldInsertVarInSequence[] insertionVars;
    private StateInt[] succ;                    // successors of the nodes
    private StateInt[] pred;                    // predecessors of the nodes
    private StateTriPartition domain;            // domain for the set of Member, Possible and Excluded variables

    // TODO constructor from a set of specified edges
    // TODO checker for clusters of possibles nodes

    // constraints registered for this sequence
    private StateStack<Constraint> onInsert;    // a node has been inserted into the sequence
    private StateStack<Constraint> onFix;       // all nodes are members or excluded: no possible node remain
    private StateStack<Constraint> onExclude;   // a node has been excluded from the sequence
    private final int begin;                      // beginning of the sequence
    private final int end;                        // end of the sequence

    private final int[] values;                   // used by fill methods

    /**
     * Creates a Sequence Variable, representing a path from begin until end in a complete insertion graph.
     * At the construction the sequence only contains the begin and end nodes.
     *
     * @param cp solver related to the variable.
     * @param nNodes >= 2 is number of nodes in the graph
     * @param begin first node of the path, must be a node of the graph, thus in the range [0..nNodes-1]
     * @param end last node of the path, must be a node of the graph, different than begin.
     */
    public OldSeqVarImpl(Solver cp, int nNodes, int begin, int end) {
        if (nNodes < 2) {
            throw new IllegalArgumentException("at least two nodes required since begin and end are included in the sequence");
        }
        if (begin < 0 || end < 0 || begin >= nNodes || end >= nNodes || begin == end) {
            throw new IllegalArgumentException("begin and end nodes must be in the range ["+0+".."+(nNodes-1)+"]"+" begin="+begin+" end="+end);
        }
        this.cp = cp;
        this.nNodes = nNodes;
        this.begin = begin;
        this.end = end;
        insertionVars = new OldInsertVarInSequence[nNodes];
        succ = new StateInt[nNodes];
        pred = new StateInt[nNodes];
        for (int i = 0; i < nNodes; ++i) {
            insertionVars[i] = new OldInsertVarInSequence(i);
            succ[i] = cp.getStateManager().makeStateInt(i);
            pred[i] = cp.getStateManager().makeStateInt(i);
        }
        succ[begin].setValue(end); // the sequence is a closed loop at the beginning
        succ[end].setValue(begin);
        pred[begin].setValue(end);
        pred[end].setValue(begin);

        domain = new StateTriPartition(cp.getStateManager(), nNodes);
        domain.include(begin);
        domain.include(end);

        // begin and end cannot be inserted
        insertionVars[begin].excludeAll();
        insertionVars[end].excludeAll();
        // no nodes can be inserted after the end
        for (int i = 0; i < nNodes; i++) {
            removePredInsert(end,i);
        }
        insertionVars[begin].nMember.setValue(0);
        insertionVars[end].nMember.setValue(0);
        insertionVars[begin].nPossible.setValue(0);
        insertionVars[end].nPossible.setValue(0);

        onInsert = new StateStack<>(cp.getStateManager());
        onFix = new StateStack<>(cp.getStateManager());
        onExclude = new StateStack<>(cp.getStateManager());
        values = new int[nNodes];
    }

    /**
     * Listener for the whole sequence.
     * For more information about the changes (i.e. what insertion has occurred?),
     * use the listener within the insertionVars
     */
    private OldSeqListener oldSeqListener = new OldSeqListener() {
        @Override
        public void fix() {
            scheduleAll(onFix);
        }

        @Override
        public void insert() {
            scheduleAll(onInsert);
        }

        @Override
        public void exclude() { scheduleAll(onExclude); }

    };

    /**
     * InsertionVar: represents a node in the sequence, its status and its predecessors
     * if it is not yet inserted / excluded in the sequence
     */
    public class OldInsertVarInSequence implements OldInsertVar {

        // the possible predecessors are partitioned into
        // 1: the ones that are included (inserted) in the sequence
        // 2: the ones that are still possible in the sequence but not yet inserted
        // 3: the excluded predecessors

        StateSparseSet insertions;
        private StateInt nPossible;  // number of possible insertions. Each value is included within the possible set of the sequence
        private StateInt nMember; // number of member insertions. Each value is included within the member set of the sequence

        private int n;
        private int id;
        // constraints registered for this sequence
        private StateStack<Constraint> onInsert;
        private StateStack<Constraint> onDomain;
        private StateStack<Constraint> onExclude;

        private OldInsertListener listener = new OldInsertListener() {
            @Override
            public void insert() {
                scheduleAll(onInsert);
            }

            @Override
            public void exclude() {
                scheduleAll(onExclude);
            }

            @Override
            public void predChange() { scheduleAll(onDomain); }

        };

        public OldInsertVarInSequence(int id) {
            // all insertions are valid at first
            // no insertion belongs to the set of member insertions at first
            this.id = id;
            n = nNodes;
            insertions = new StateSparseSet(cp.getStateManager(),n,0);
            nPossible = cp.getStateManager().makeStateInt(n); // consider all nodes as possible
            nMember = cp.getStateManager().makeStateInt(0);

            onDomain = new StateStack<>(cp.getStateManager());
            onInsert = new StateStack<>(cp.getStateManager());
            onExclude = new StateStack<>(cp.getStateManager());

            // a node cannot have itself as predecessor
            insertions.remove(id); // a node cannot have itself as predecessor
            nPossible.decrement();

            // the begin node is always a member predecessor at first
            nMember.setValue(1);
            nPossible.decrement();
            // the end node is never a predecessor
            nPossible.decrement();

            // the end node cannot be a predecessor of any other node (by definition)
            insertions.remove(end);
        }

        @Override
        public Solver getSolver() {
            return cp;
        }

        @Override
        public void removePred(int i) {
            OldSeqVarImpl.this.removePredInsert(i, id);
        }

        private void excludeAll() {
            insertions.removeAll();
        }

        @Override
        public int node() {
            return id;
        }

        @Override
        public boolean isPred(int i) {
            return insertions.contains(i);
        }

        @Override
        public int fillPred(int[] dest) {
            int s = insertions.fillArray(dest);
            return s;
        }

        @Override
        public int nPred() {
            return insertions.size();
        }

        @Override
        public void whenInsert(Procedure f) {
            onInsert.push(constraintClosure(f));
        }

        @Override
        public void propagateOnInsert(Constraint c) {
            onInsert.push(c);
        }

        @Override
        public void whenPredChange(Procedure f) {
            onDomain.push(constraintClosure(f));
        }

        @Override
        public void propagateOnPredChange(Constraint c) {
            onDomain.push(c);
        }

        @Override
        public void whenExclude(Procedure f) {
            onExclude.push(constraintClosure(f));
        }

        @Override
        public void propagateOnExclude(Constraint c) {
            onExclude.push(c);
        }

        @Override
        public void whenFixed(Procedure f) {
            onExclude.push(constraintClosure(f));
            onInsert.push(constraintClosure(f));
        }

        @Override
        public void propagateOnFix(Constraint c) {
            onInsert.push(c);
            onExclude.push(c);
        }

        public int nMember() {return nMember.value();}

        public int nPossible() {return nPossible.value();}

        @Override
        public String toString() {
            return insertions.toString();
        }
    }

    @Override
    public Solver getSolver() {
        return cp;
    }

    @Override
    public int begin() {
        return begin;
    }

    @Override
    public int end() {
        return end;
    }

    @Override
    public boolean isFixed() {
        return domain.nPossible() == 0;
    }

    @Override
    public int nextMember(int node) {
        return succ[node].value();
    }

    @Override
    public int predMember(int node) {
        return pred[node].value();
    }

    @Override
    public int fillOrder(int[] dest) {
        return fillOrder(dest, true);
    }

    @Override
    public int fillOrder(int[] dest, boolean includeBounds) {
        dest[0] = includeBounds ? begin : succ[begin].value();
        int lastElem = includeBounds ? end : pred[end].value();
        int i = 1;
        for (;dest[i-1] != lastElem; ++i)
            dest[i] = succ[dest[i-1]].value();
        return i;
    }

    @Override
    public int nMember() {
        return nMember(true);
    }

    @Override
    public int nMember(boolean includeBounds) {
        if (includeBounds)
            return domain.nIncluded();
        else
            return domain.nIncluded() - 2;
    }

    @Override
    public int nPossible() {
        return domain.nPossible();
    }

    @Override
    public int nExcluded() {
        return domain.nExcluded();
    }

    @Override
    public int nNode() {
        return nNodes;
    }

    @Override
    public boolean canInsert(int pred, int node) {
        return isPossible(node) && isMember(pred) && (insertionVars[node].isPred(pred));
    }

    @Override
    public void insert(int pred, int node) {
        if (!isMember(pred)) {
            throw INCONSISTENCY;
        }
        if (!domain.include(node)) {
            // the node is either already member or excluded
            if (succ[pred].value() != node || isExcluded(node)) {
                // the insertion points asked differs from the current / the node is excluded
                throw INCONSISTENCY;
            } else {
                // trying to do the same insertion twice
                return;
            }
        }
        else if (!insertionVars[node].isPred(pred)) {
            throw INCONSISTENCY; // the insertion var did not contain the node
        }
        int succNode = succ[pred].value();
        succ[pred].setValue(node);
        succ[node].setValue(succNode);
        this.pred[node].setValue(pred);
        this.pred[succNode].setValue(node);

        insertionVars[node].excludeAll();
        insertionVars[node].nMember.setValue(0);
        insertionVars[node].nPossible.setValue(0);

        // update the counters
        int size = domain.fillPossibleWithFilter(values, i -> insertionVars[i].isPred(node));
        // can only iterate over possible nodes
        for (int i = 0; i < size; i++) {
            // the insertion point related to this node belongs now a member insertion point
            insertionVars[values[i]].nPossible.decrement();
            insertionVars[values[i]].nMember.increment();
        }
        if (isFixed()) {
            oldSeqListener.fix();
        }
        insertionVars[node].listener.insert();
        insertionVars[node].listener.predChange();
        oldSeqListener.insert();
    }

    @Override
    public void exclude(int node) {
        if (isMember(node)) {
            throw INCONSISTENCY;
        }
        if (isExcluded(node))
            return;
        int size = domain.fillPossible(values);
        for (int i = 0; i < size ; ++i) {// remove this node for all others insertions
            insertionVars[values[i]].removePred(node);
        }
        if (domain.exclude(node)) {
            insertionVars[node].nMember.setValue(0);
            insertionVars[node].nPossible.setValue(0);
            if (isFixed())
                oldSeqListener.fix();
            insertionVars[node].insertions.removeAll();
            insertionVars[node].listener.exclude();
            //insertionVars[node].listener.change();  // not called as it technically does not change its domain
            oldSeqListener.exclude();
        }
    }

    @Override
    public void excludeAllPossible() {
        int size = domain.fillPossible(values);
        domain.excludeAllPossible();
        oldSeqListener.fix(); // notify that the variable is fixed
        oldSeqListener.exclude(); // nodes have been excluded
        for (int i = 0 ; i < size; ++i) {
            insertionVars[values[i]].insertions.removeAll();
            insertionVars[values[i]].listener.exclude();
        }
    }

    @Override
    public boolean isMember(int node) {
        return domain.isIncluded(node);
    }

    @Override
    public boolean isPossible(int node) {
        return domain.isPossible(node);
    }

    @Override
    public boolean isExcluded(int node) {
        return domain.isExcluded(node);
    }

    @Override
    public int fillMember(int[] dest) {
        return domain.fillIncluded(dest);
    }

    @Override
    public int fillPossible(int[] dest) {
        return domain.fillPossible(dest);
    }

    @Override
    public int fillExcluded(int[] dest) {
        return domain.fillExcluded(dest);
    }

    @Override
    public int fillMemberPredInsert(int node, int[] dest) {
        if (!isPossible(node))
            return 0;
        int j = 0; // indexing used for dest
        int s = insertionVars[node].nPred();
        if (s > domain.nIncluded()) { // quicker to iterate over the current sequence
            j =  domain.fillIncludedWithFilter(dest, v -> insertionVars[node].isPred(v));
        } else { // quicker to iterate over the remaining insertions inside the insertion var
            // filter to only keep the ones in the sequence
            j = insertionVars[node].insertions.fillArrayWithFilter(dest,i -> domain.isIncluded(i));
        }
        return j;
    }

    @Override
    public int fillPossiblePredInsert(int node, int[] dest) {
        if (!isPossible(node))
            return 0;
        int j = 0; // indexing used for dest
        int s = insertionVars[node].nPred();
        if (s > domain.nPossible()) { // quicker to iterate over the possible nodes
            j = domain.fillPossibleWithFilter(dest, v -> insertionVars[node].isPred(v));
        } else { // quicker to iterate over the remaining insertions inside the insertion var
            // filter to only keep the possible ones
            j = insertionVars[node].insertions.fillArrayWithFilter(dest,i -> domain.isPossible(i));
        }
        return j;
    }

    @Override
    public int nPossiblePredInsert(int node) {
        return insertionVars[node].nPossible.value();
    }

    @Override
    public int nMemberPredInsert(int node) {
        return insertionVars[node].nMember.value();
    }

    @Override
    public int nPredInsert(int node) {
        return insertionVars[node].nPred();
    }

    @Override
    public int fillPredInsert(int node, int[] dest) {
        return insertionVars[node].fillPred(dest);
    }

    @Override
    public boolean isPredInsert(int pred, int node) {
        return insertionVars[node].isPred(pred);
    }

    /**
     * remove an insertion point for an insertion var. The insertion trying to be removed cannot be in the excluded
     * set of the domain yet!
     * trigger the propagation
     * @param insertion
     * @param node
     */
    @Override
    public void removePredInsert(int insertion, int node) {
        if (insertionVars[node].insertions.remove(insertion)) {
            // update the counters for the number of member and possible insertions
            if (isMember(insertion)) {
                insertionVars[node].nMember.decrement();
            } else if (isPossible(insertion)) {
                insertionVars[node].nPossible.decrement();
            } if (insertionVars[node].nPred() == 0) {
                exclude(node);
            }
            insertionVars[node].listener.predChange();
        }
    }

    @Override
    public void removeAllPredInsertFrom(int node) {
        int n = fillPossible(values);
        for (int i = 0; i < n ; ++i) {
            removePredInsert(node, values[i]);
        }
    }

    @Override
    public OldInsertVar getInsertionVar(int i) {
        return insertionVars[i];
    }

    /**  =====  propagation methods  =====  */

    @Override
    public void whenFix(Procedure f) {
        onFix.push(constraintClosure(f));
    }

    @Override
    public void whenInsert(Procedure f) {
        onInsert.push(constraintClosure(f));
    }

    @Override
    public void whenExclude(Procedure f) {
        onExclude.push(constraintClosure(f));
    }

    @Override
    public void propagateOnFix(Constraint c) {
        onFix.push(c);
    }

    @Override
    public void propagateOnInsert(Constraint c) {
        onInsert.push(c);
    }

    @Override
    public void propagateOnExclude(Constraint c) {
        onExclude.push(c);
    }

    protected void scheduleAll(StateStack<Constraint> constraints) {
        for (int i = 0; i < constraints.size(); i++)
            cp.schedule(constraints.get(i));
    }

    private Constraint constraintClosure(Procedure f) {
        Constraint c = new ConstraintClosure(cp, f);
        getSolver().post(c, false);
        return c;
    }

    @Override
    public String toString() {
        return ordering(true, " -> ");
    }

    @Override
    public String ordering() {
        return ordering(false);
    }

    @Override
    public String ordering(boolean includeBounds) {
        return ordering(includeBounds, " -> ");
    }

    @Override
    public String ordering(boolean includeBounds, String join) {
        if (nMember(includeBounds) == 0)
            return "";
        int current = includeBounds ? begin : nextMember(begin);
        int last = includeBounds ? end : predMember(end);
        StringBuilder description = new StringBuilder(String.format("%d", current));
        while (current != last) {
            current = nextMember(current);
            description.append(join);
            description.append(current);
        }
        return description.toString();
    }
}
