package games.connect4;
import core.components.BoardNode;
import core.components.Token;

import java.util.ArrayList;
public class Connect4Constants {
    public static final ArrayList<BoardNode> playerMapping = new ArrayList<BoardNode>() {{
        add(new BoardNode(8, "x"));
        add(new BoardNode(8, "o"));
    }};
    public static final String emptyCell = ".";
}
