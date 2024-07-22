package games.mastermind;

import core.components.Token;

import java.util.ArrayList;

public class MMConstants {
    public static final ArrayList<Token> resultColours = new ArrayList<Token>() {{
            add(new Token("w"));
            add(new Token("b"));
            add(new Token("x"));
    }};

    public static final ArrayList<Token> guessColours = new ArrayList<Token>() {{
        add(new Token("R"));
        add(new Token("O"));
        add(new Token("Y"));
        add(new Token("G"));
        add(new Token("B"));
        add(new Token("V"));
    }};

    static String emptyPeg = ".";

    protected static ArrayList<Token> copyGuessColours() {
        ArrayList<Token> copy = new ArrayList<>();
        for (Token t : guessColours) {
            copy.add(t.copy());
        }
        return copy;
    }

}
