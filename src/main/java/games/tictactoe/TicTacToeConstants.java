package games.tictactoe;

import core.components.Token;

import java.util.ArrayList;

public class TicTacToeConstants {
    public static final Token defaultToken = new Token(" ");
    public static final ArrayList<Token> playerMapping = new ArrayList<Token>() {{
        add(new Token("x"));
        add(new Token("o"));
    }};
}
