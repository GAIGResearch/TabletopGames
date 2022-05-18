package games.tictactoe;

import core.components.BoardNode;
import core.components.Token;

import java.util.ArrayList;

public class TicTacToeConstants {
    public static final ArrayList<BoardNode> playerMapping = new ArrayList<BoardNode>() {{
        add(new BoardNode(-1,"x"));
        add(new BoardNode(-1,"o"));
    }};
    public static final String emptyCell = ".";
}
