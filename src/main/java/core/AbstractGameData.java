package core;

import core.components.*;

public abstract class AbstractGameData<T extends Component> {
    public abstract void load(String dataPath);
    public GraphBoard findBoard(String name) { return null; }
    public Counter findCounter(String name) { return null; }
    public Token findToken(String name) { return null; }
    public Deck<T> findDeck(String name) { return null; }
}
