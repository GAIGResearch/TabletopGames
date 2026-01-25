package games.tictactoe;

import core.components.BoardNode;
import core.components.Token;

import java.util.ArrayList;

public class TicTacToeConstants {
    public static final ArrayList<BoardNode> playerMapping = new ArrayList<>() {{
        add(new BoardNode("x"));
        add(new BoardNode("o"));
    }};
    public static final String emptyCell = ".";
}
