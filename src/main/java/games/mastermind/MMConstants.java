package games.mastermind;

import core.components.BoardNode;
import core.components.Token;

import java.util.ArrayList;

public class MMConstants {
    public static final ArrayList<BoardNode> resultColours = new ArrayList<>() {{
        add(new BoardNode("b"));
        add(new BoardNode("w"));
        add(new BoardNode("x"));
    }};

    public static final ArrayList<BoardNode> guessColours = new ArrayList<>() {{
        add(new BoardNode("R"));
        add(new BoardNode("O"));
        add(new BoardNode("Y"));
        add(new BoardNode("G"));
        add(new BoardNode("B"));
        add(new BoardNode("V"));
    }};

    static String emptyPeg = ".";

    protected static ArrayList<BoardNode> copyGuessColours() {
        ArrayList<BoardNode> copy = new ArrayList<>();
        for (BoardNode t : guessColours) {
            copy.add(t.copy());
        }
        return copy;
    }

}
