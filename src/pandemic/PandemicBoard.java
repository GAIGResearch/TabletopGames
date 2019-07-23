package pandemic;

import core.Board;
import java.util.ArrayList;

public class PandemicBoard extends Board {
    String dataPath = "data/pandemicBoard.json";

    public PandemicBoard() {
        boardNodes = new ArrayList<>();
    }

    @Override
    public void loadBoard() {
        // 1. Read data from file to create list of boardNodes

        // 2. Assign neighbours when all boardNodes created.
    }

    @Override
    public Board copy() {
        Board copy = new PandemicBoard();
        copy.setBoardNodes(new ArrayList<>(boardNodes));
        return null;
    }
}
