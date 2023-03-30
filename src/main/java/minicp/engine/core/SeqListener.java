package minicp.engine.core;

public interface SeqListener extends OldSeqListener {

    /**
     * Called whenever a node has been set as required
     * If an insertion occurs {@link SeqVar#insert(int, int)}, both
     *      {@link SeqListener#require()} and {@link SeqListener#insert()} are triggered
     */
    void require();

}
