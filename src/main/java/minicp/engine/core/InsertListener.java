package minicp.engine.core;

public interface InsertListener extends OldInsertListener {

    /**
     * Called whenever a successor insertion has been removed
     */
    void succChange();

    /**
     * Called whenever a node has been set as required
     * If an insertion occurs {@link SeqVar#insert(int, int)}, both
     *      {@link InsertListener#require()} and {@link OldInsertListener#insert()} are triggered
     */
    void require();

}
