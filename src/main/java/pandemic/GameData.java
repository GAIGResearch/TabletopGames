package pandemic;

import components.Board;
import components.Counter;
import components.Deck;
import components.Token;
import core.Area;

import java.util.HashMap;

public interface GameData {
    void load(String dataPath);

    Board findBoard(String name);

    Counter findCounter(String name);

    Token findToken(String name);

    Deck findDeck(String name);
}
