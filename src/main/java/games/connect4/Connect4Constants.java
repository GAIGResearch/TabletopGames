package games.connect4;
import core.components.Token;

import java.util.ArrayList;
public class Connect4Constants {
    public static final ArrayList<Token> playerMapping = new ArrayList<Token>() {{
        add(new Token("x"));
        add(new Token("o"));
    }};
    public static final String emptyCell = ".";
}
