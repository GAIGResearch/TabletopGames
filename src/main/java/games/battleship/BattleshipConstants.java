package games.battleship;
import core.components.Token;
import java.util.ArrayList;

public class BattleshipConstants {
    public static final ArrayList<Token> shipMapping = new ArrayList<Token>() {{
        add(new Token("X")); // Hit
        add(new Token("O")); // Miss
        add(new Token("S")); // Ship
    }};
    public static final String water = ".";
}
