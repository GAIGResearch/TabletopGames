package core;

import core.components.*;

public abstract class AbstractGameData {
    public abstract void load(String dataPath);
    public GraphBoard findBoard(String name) { return null; }
    public Counter findCounter(String name) { return null; }
    public Token findToken(String name) { return null; }
    public <T extends Component> Deck<T> findDeck(String name) { return null; }
}
