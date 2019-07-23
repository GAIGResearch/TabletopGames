package core;

import java.util.List;

public abstract class Board {
    protected List<BoardNode> boardNodes;

    public List<BoardNode> getBoardNodes() {
        return boardNodes;
    }

    public void setBoardNodes(List<BoardNode> boardNodes) {
        this.boardNodes = boardNodes;
    }

    public abstract void loadBoard();

    public abstract Board copy();
}
