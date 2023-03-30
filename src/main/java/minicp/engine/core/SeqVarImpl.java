package minicp.engine.core;

import minicp.state.StateInt;
import minicp.state.StateTriPartition;
import minicp.state.StateSparseSet;
import minicp.state.StateStack;
import minicp.util.Procedure;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

/**
 * A {@link SeqVar} implementation
 */
public class SeqVarImpl implements SeqVar {

    private final Solver cp;
    private final int nNodes;                   // number of nodes available (omitting begin and end)
    private InsertVarInSeq[] insertionVars;
    private StateInt[] succ;                    // successors of the nodes
    private StateInt[] pred;                    // predecessors of the nodes
    private StateTriPartition domain;           // domain for the set of Required, Possible and Excluded variables
    private StateInt member;                    // number of member nodes amongst the Required nodes

    // constraints registered for this sequence
    private StateStack<Constraint> onInsert;    // a node has been inserted into the sequence
    private StateStack<Constraint> onFix;       // all nodes are members or excluded: no possible node remain
    private StateStack<Constraint> onExclude;   // a node has been excluded from the sequence
    private StateStack<Constraint> onRequire;   // a node has been required in the sequence
    private final int begin;                    // beginning of the sequence
    private final int end;                      // end of the sequence

    private final int[] values;                 // used by fill methods

    /**
     * Creates a Sequence Variable, representing a path from begin until end in a complete insertion graph.
     * At the construction the sequence only contains the begin and end nodes.
     *
     * @param cp solver related to the variable.
     * @param nNodes >= 2 is number of nodes in the graph
     * @param begin first node of the path, must be a node of the graph, thus in the range [0..nNodes-1]
     * @param end last node of the path, must be a node of the graph, different than begin.
     */
    public SeqVarImpl(Solver cp, int nNodes, int begin, int end) {
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
        insertionVars = new InsertVarInSeq[nNodes];
        succ = new StateInt[nNodes];
        pred = new StateInt[nNodes];
        for (int i = 0; i < nNodes; ++i) {
            insertionVars[i] = new InsertVarInSeq(i);
            succ[i] = cp.getStateManager().makeStateInt(i);
            pred[i] = cp.getStateManager().makeStateInt(i);
        }
        member = cp.getStateManager().makeStateInt(2); // the beginning and ending nodes are member
        succ[begin].setValue(end); // the sequence is a closed loop at the beginning
        succ[end].setValue(begin);
        pred[begin].setValue(end);
        pred[end].setValue(begin);

        domain = new StateTriPartition(cp.getStateManager(), nNodes);
        domain.include(begin);
        domain.include(end);

        // begin and end cannot be inserted
        insertionVars[begin].excludeAllPred();
        insertionVars[begin].succInsertions.remove(end);
        insertionVars[begin].succNMember.setValue(0);
        insertionVars[begin].succNPossible.setValue(nNodes-2);
        insertionVars[end].excludeAllSucc();
        insertionVars[end].predNMember.setValue(0);
        insertionVars[end].predInsertions.remove(begin);
        insertionVars[end].predNPossible.setValue(nNodes-2);

        onInsert = new StateStack<>(cp.getStateManager());
        onFix = new StateStack<>(cp.getStateManager());
        onExclude = new StateStack<>(cp.getStateManager());
        onRequire = new StateStack<>(cp.getStateManager());
        values = new int[nNodes];
    }

    /**
     * Listener for the whole sequence.
     * For more information about the changes (i.e. what insertion has occurred?),
     * use the listener within the insertionVars
     */
    private SeqListener listener = new SeqListener() {
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

        @Override
        public void require() { scheduleAll(onRequire); }
    };

    /**
     * InsertionVar: represents a node in the sequence, its status and its predecessors
     * if it is not yet inserted / excluded in the sequence
     */
    public class InsertVarInSeq implements InsertVar {

        // the possible predecessors are partitioned into
        // 1: the ones that are included (inserted) in the sequence
        // 2: the ones that are still possible in the sequence but not yet inserted
        // 3: the excluded predecessors

        StateSparseSet predInsertions;
        StateSparseSet succInsertions;
        private StateInt predNPossible;  // number of possible insertions. Each value is included within the possible set of the sequence
        private StateInt predNMember; // number of member insertions. Each value is included within the member set of the sequence
        private StateInt succNPossible;
        private StateInt succNMember;

        private int n;
        private int id;
        // constraints registered for this sequence
        private StateStack<Constraint> onInsert;
        private StateStack<Constraint> onPredDomain;
        private StateStack<Constraint> onExclude;
        private StateStack<Constraint> onSuccDomain;
        private StateStack<Constraint> onRequire;

        private InsertListener listener = new InsertListener() {
            @Override
            public void insert() {
                scheduleAll(onInsert);
            }

            @Override
            public void exclude() {
                scheduleAll(onExclude);
            }

            @Override
            public void predChange() {
                scheduleAll(onPredDomain);
            }

            @Override
            public void succChange() {
                scheduleAll(onSuccDomain);
            }

            @Override
            public void require() {
                scheduleAll(onRequire);
            }

        };

        public InsertVarInSeq(int id) {
            // all insertions are valid at first
            // no insertion belongs to the set of member insertions at first
            this.id = id;
            n = nNodes;
            predInsertions = new StateSparseSet(cp.getStateManager(),n,0);
            // all nodes - itself - end - begin (it is a member node)
            predNPossible = cp.getStateManager().makeStateInt(n-3);
            predNMember = cp.getStateManager().makeStateInt(1);

            succInsertions = new StateSparseSet(cp.getStateManager(),n,0);
            // all nodes - itself - begin - end (it is a member node)
            succNPossible = cp.getStateManager().makeStateInt(n-3);
            succNMember = cp.getStateManager().makeStateInt(1);

            onPredDomain = new StateStack<>(cp.getStateManager());
            onInsert = new StateStack<>(cp.getStateManager());
            onExclude = new StateStack<>(cp.getStateManager());
            onSuccDomain = new StateStack<>(cp.getStateManager());
            onRequire = new StateStack<>(cp.getStateManager());

            // a node cannot have itself as predecessor
            predInsertions.remove(id);
            succInsertions.remove(id);
            // the end node cannot be a predecessor of any other node (by definition)
            predInsertions.remove(end);
            // the begin node cannot be a successor of any other node (by definition)
            succInsertions.remove(begin);
        }

        @Override
        public Solver getSolver() {
            return cp;
        }

        @Override
        public void removePred(int i) {
            SeqVarImpl.this.removePredInsert(i, id);
        }

        private void excludeAllPred() {
            predInsertions.removeAll();
            predNMember.setValue(0);
            predNPossible.setValue(0);
        }

        private void excludeAllSucc() {
            succInsertions.removeAll();
            succNMember.setValue(0);
            succNPossible.setValue(0);
        }

        /**
         * remove all predecessor that are member nodes within the insertions
         */
        private void removeAllPredMembers() {
            if (member.value() < predInsertions.size()) { // quicker to iterate from the member
                int n = begin;
                while (n != end) {
                    predInsertions.remove(n);
                    n = succ[n].value(); // goes to the next node
                }
            } else { // quicker to iterate from the stored insertions
                predInsertions.removeAllWithFilter(SeqVarImpl.this::isMember);
            }
            predNMember.setValue(0);
        }

        /**
         * remove all successors that are member nodes within the insertions
         */
        private void removeAllSuccMembers() {
            if (member.value() < predInsertions.size()) {
                int n = end;
                while (n != begin) {
                    succInsertions.remove(n);
                    n = pred[n].value();
                }
            } else {
                succInsertions.removeAllWithFilter(SeqVarImpl.this::isMember);
            }
            succNMember.setValue(0);
        }

        @Override
        public int node() {
            return id;
        }

        @Override
        public boolean isPred(int i) {
            return predInsertions.contains(i);
        }

        @Override
        public int fillPred(int[] dest) {
            int s = predInsertions.fillArray(dest);
            return s;
        }

        @Override
        public int nPred() {
            return predInsertions.size();
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
            onPredDomain.push(constraintClosure(f));
        }

        @Override
        public void propagateOnPredChange(Constraint c) {
            onPredDomain.push(c);
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

        public int nMember() {return predNMember.value();}

        public int nPossible() {return predNPossible.value();}

        @Override
        public String toString() {
            return "[pred = " + predInsertions.toString() + ", succ = " + succInsertions.toString() + ']';
        }

        @Override
        public boolean isSucc(int i) {
            return succInsertions.contains(i);
        }

        @Override
        public void removeSucc(int i) {
            SeqVarImpl.this.removePredInsert(id, i);
        }

        @Override
        public int fillSucc(int[] dest) {
            return succInsertions.fillArray(dest);
        }

        @Override
        public int nSucc() {
            return succInsertions.size();
        }

        @Override
        public void whenSuccChange(Procedure f) {
            onSuccDomain.push(constraintClosure(f));
        }

        @Override
        public void propagateOnSuccChange(Constraint c) {
            onSuccDomain.push(c);
        }

        @Override
        public void whenRequire(Procedure f) {
            onRequire.push(constraintClosure(f));
        }

        @Override
        public void propagateOnRequire(Constraint c) {
            onRequire.push(c);
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
        return nMember(true) + nExcluded() == nNodes;
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
            return member.value();
        else
            return member.value() - 2;
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
        return (isPossible(node) || isRequired(node) && !isMember(node)) &&
                isMember(pred) &&
                insertionVars[node].isPred(pred) &&
                insertionVars[pred].isSucc(node);
    }

    /**
     * Insert a node after a given predecessor
     * The insertion is only valid if
     *  - the node is possible
     *  - the predecessor is a member and a predecessor insertion for the node
     *  - {@code nextMember(pred)} is a valid successor for the node
     *
     * @param pred predecessor for the node
     * @param node node to schedule
     */
    @Override
    public void insert(int pred, int node) {
        if (!isMember(pred)) {
            throw INCONSISTENCY;
        }
        int succNode = succ[pred].value();
        // include within the domain
        boolean wasRequired = false;
        if (!domain.include(node)) {
            // the node is either already required or excluded
            boolean isMember = isMember(node);
            if ((isMember && predMember(node) != pred) || isExcluded(node)) {
                // the insertion points asked differs from the current / the node is excluded
                throw INCONSISTENCY;
            } else if (isMember) {
                // trying to do the same insertion twice
                return;
            } else {
                wasRequired = true;
            }
        }
        if (!insertionVars[node].isPred(pred) || !insertionVars[node].isSucc(succNode)) {
            throw INCONSISTENCY; // the insertion var did not contain the node
        }
        // extend the path, to create the link pred -> node -> succNode
        succ[pred].setValue(node);
        succ[node].setValue(succNode);
        this.pred[node].setValue(pred);
        this.pred[succNode].setValue(node);
        member.increment();

        // update the counters for the member successors: one of their predecessor has been removed
        int size = insertionVars[node].fillSucc(values);
        for (int i = 0; i < size; i++) {
            int succ = values[i];
            if (isMember(succ)) {
                // the member nodes that were successors do not have this point as an insertion now
                insertionVars[succ].predInsertions.remove(node);
                insertionVars[succ].predNPossible.decrement();
            } else {
                // the insertion point related to this node is now a member insertion point
                insertionVars[succ].predNPossible.decrement();
                insertionVars[succ].predNMember.increment();
            }
        }
        // update the counters for the predecessors of the nodes: the insertion point is now a member node
        size = insertionVars[node].fillPred(values);
        for (int i = 0; i < size; i++) {
            int predecessor = values[i];
            if (isMember(predecessor)) {
                // the member nodes that were predecessors do not have this point as an insertion now
                insertionVars[predecessor].succInsertions.remove(node);
                insertionVars[predecessor].succNPossible.decrement();
            } else {
                // the insertion point related to this node is now a member insertion point
                insertionVars[predecessor].succNPossible.decrement();
                insertionVars[predecessor].succNMember.increment();
            }
        }

        // no member can be within the predecessors / successors for the node

        insertionVars[node].listener.predChange();
        insertionVars[node].removeAllPredMembers();

        insertionVars[node].listener.succChange();
        insertionVars[node].removeAllSuccMembers();

        if (isFixed())
            listener.fix();
        if (!wasRequired) {
            insertionVars[node].listener.require();
            listener.require();
        }
        insertionVars[node].listener.insert();
        listener.insert();
    }

    @Override
    public void exclude(int node) {
        if (isRequired(node)) {
            throw INCONSISTENCY;
        }
        if (isExcluded(node))
            return;
        // removes this value for all the successors related to it
        int size = insertionVars[node].fillSucc(values);
        for (int i = 0 ; i < size ; ++i) {
            insertionVars[values[i]].removePred(node);
        }
        // removes this value for all the predecessors related to it
        size = insertionVars[node].fillPred(values);
        for (int i = 0 ; i < size ; ++i) {
            insertionVars[values[i]].removeSucc(node);
        }
        // exclude from the domain tri partition
        domain.exclude(node);
        if (isFixed())
            listener.fix();
        insertionVars[node].predInsertions.removeAll();
        insertionVars[node].succInsertions.removeAll();
        insertionVars[node].listener.exclude();
        //insertionVars[node].listener.predChange();  // not called as it technically does not change its domain
        listener.exclude();
    }

    @Override
    public void excludeAllPossible() {
        int size = domain.fillPossible(values);
        domain.excludeAllPossible();
        listener.fix(); // notify that the variable is fixed
        listener.exclude(); // nodes have been excluded
        for (int i = 0 ; i < size; ++i) {
            insertionVars[values[i]].predInsertions.removeAll();
            insertionVars[values[i]].succInsertions.removeAll();
            insertionVars[values[i]].listener.exclude();
        }
    }

    @Override
    public boolean isMember(int node) {
        return domain.isIncluded(node) && succ[node].value() != node;
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
        return fillOrder(dest, true);
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
        if (isMember(node) || isExcluded(node))
            return 0;  // excluded and member both don't have member successors
        int j = 0; // indexing used for dest
        int s = insertionVars[node].nPred();
        if (s > domain.nIncluded()) { // quicker to iterate over the current sequence
            j =  domain.fillIncludedWithFilter(dest, v -> insertionVars[node].isPred(v) && isMember(v));
        } else { // quicker to iterate over the remaining insertions inside the insertion var
            // filter to only keep the ones in the sequence
            j = insertionVars[node].predInsertions.fillArrayWithFilter(dest, this::isMember);
        }
        return j;
    }

    @Override
    public int fillPossiblePredInsert(int node, int[] dest) {
        if (isExcluded(node))
            return 0;
        int j = 0; // indexing used for dest
        int s = insertionVars[node].nPred();
        if (s > nNotMemberNotExcluded()) { // quicker to iterate over the possible nodes
            j = domain.fillIncludedAndPossibleWithFilter(dest, v -> insertionVars[node].isPred(v) && !isMember(v));
        } else { // quicker to iterate over the remaining insertions inside the insertion var
            // filter to only keep the possible ones
            j = insertionVars[node].predInsertions.fillArrayWithFilter(dest, i -> !isMember(i));
        }
        return j;
    }

    @Override
    public int nPossiblePredInsert(int node) {
        return insertionVars[node].predNPossible.value();
    }

    @Override
    public int nMemberPredInsert(int node) {
        return insertionVars[node].predNMember.value();
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
     * Removes an insertion point for an insertion var. The insertion trying to be removed cannot be in the excluded
     * set of the domain yet!
     * Triggers the propagation
     *
     * @param pred predecessor of a node
     * @param node node
     */
    @Override
    public void removePredInsert(int pred, int node) {
        if (insertionVars[node].predInsertions.remove(pred)) {
            insertionVars[node].listener.predChange();
            // update the counters for the number of member and possible insertions
            boolean isPossiblePred = !isMember(pred);
            boolean isPossibleNode = !isMember(node);
            if (isMember(pred)) {
                insertionVars[node].predNMember.decrement();
            } else if (isPossiblePred) {
                insertionVars[node].predNPossible.decrement();
            }
            insertionVars[pred].succInsertions.remove(node);
            insertionVars[pred].listener.succChange();
            // update the counters for the successors of pred
            if (isMember(node)) {
                insertionVars[pred].succNMember.decrement();
            } else if (isPossibleNode) {
                insertionVars[pred].succNPossible.decrement();
            }
            if (insertionVars[node].nPred() == 0 && isPossibleNode) {
                exclude(node);
            }
            if (insertionVars[pred].nSucc() == 0 && isPossiblePred) {
                exclude(pred);
            }
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
    public int fillMemberSuccInsert(int node, int[] dest) {
        if (isMember(node) || isExcluded(node))
            return 0; // excluded and member both don't have member successors
        int j; // indexing used for dest
        int s = insertionVars[node].nSucc();
        if (s > domain.nIncluded()) { // quicker to iterate over the current sequence
            j =  domain.fillIncludedWithFilter(dest, v -> insertionVars[node].isSucc(v) && isMember(v));
        } else { // quicker to iterate over the remaining insertions inside the insertion var
            // filter to only keep the ones in the sequence
            j = insertionVars[node].succInsertions.fillArrayWithFilter(dest, this::isMember);
        }
        return j;
    }

    @Override
    public int fillPossibleSuccInsert(int node, int[] dest) {
        if (isExcluded(node))
            return 0;
        int j = 0; // indexing used for dest
        int s = insertionVars[node].nSucc();
        if (s > nNotMemberNotExcluded()) { // quicker to iterate over the possible nodes
            j = domain.fillIncludedAndPossibleWithFilter(dest, v -> insertionVars[node].isSucc(v) && !isMember(v));
        } else { // quicker to iterate over the remaining insertions inside the insertion var
            // filter to only keep the possible ones
            j = insertionVars[node].succInsertions.fillArrayWithFilter(dest, i -> !isMember(i));
        }
        return j;
    }

    @Override
    public int fillSuccInsert(int node, int[] dest) {
        return insertionVars[node].fillSucc(dest);
    }

    @Override
    public int nMemberSuccInsert(int node) {
        return insertionVars[node].succNMember.value();
    }

    @Override
    public int nPossibleSuccInsert(int node) {
        return insertionVars[node].succNPossible.value();
    }

    @Override
    public int nSuccInsert(int node) {
        return insertionVars[node].succInsertions.size();
    }

    @Override
    public void require(int node) {
        if (isRequired(node))
            return;
        if (!domain.include(node))
            throw INCONSISTENCY;
        listener.require();
        insertionVars[node].listener.require();
    }

    @Override
    public int fillRequired(int[] dest) {
        return fillRequired(dest, true);
    }

    @Override
    public int nRequired() {
        return nRequired(true);
    }

    @Override
    public int nRequired(boolean includeMember) {
        return includeMember ? domain.nIncluded() : domain.nIncluded() - member.value();
    }

    @Override
    public int fillRequired(int[] dest, boolean includeMember) {
        if (includeMember)
            return domain.fillIncluded(dest);
        else
            return domain.fillIncludedWithFilter(dest, i -> !isMember(i));
    }

    /**
     * Gives the number of nodes that are both possible or required but not yet inserted
     *
     * @return number of nodes that are both possible or required but not yet inserted
     */
    private int nNotMemberNotExcluded() {
        return domain.nPossible() + domain.nIncluded() - member.value();
    }

    @Override
    public boolean isRequired(int node) {
        return domain.isIncluded(node);
    }

    @Override
    public void requireAllPossible() {
        domain.excludeAll();
    }

    @Override
    public boolean areAllPossibleRequired() {
        return domain.nPossible() == 0;
    }

    @Override
    public InsertVar getInsertionVar(int i) {
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
    public void whenRequire(Procedure f) {
        onRequire.push(constraintClosure(f));
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

    @Override
    public void propagateOnRequire(Constraint c) {
        onRequire.push(c);
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
