package core;

import core.components.Board;
import core.components.Counter;
import core.components.Deck;
import core.components.Token;

public interface GameData {
    void load(String dataPath);

    Board findBoard(String name);

    Counter findCounter(String name);

    Token findToken(String name);

    Deck findDeck(String name);
}
