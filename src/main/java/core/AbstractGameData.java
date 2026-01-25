package core;

import core.components.*;
import core.components.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AbstractGameData {

    private List<GraphBoard> graphBoards = new ArrayList<>();
    private List<GridBoard> gridBoards = new ArrayList<>();
    private List<Deck<Card>> decks = new ArrayList<>();
    private List<Counter> counters = new ArrayList<>();
    private List<Token> tokens = new ArrayList<>();

    public void load(String dataPath) {
        File dir = new File(dataPath);
        if (dir.isDirectory()) {
            List<String> files = Arrays.asList(Objects.requireNonNull(dir.list()));
            if (files.contains("boards.json"))
                graphBoards = GraphBoard.loadBoards(dataPath + "/boards.json");
            if (files.contains("gridboards.json"))
                gridBoards = GridBoard.loadBoards(dataPath + "/gridboards.json");
            if (files.contains("decks.json"))
                decks = Deck.loadDecksOfCards(dataPath + "/decks.json");
            if (files.contains("counters.json"))
                counters = Counter.loadCounters(dataPath + "/counters.json");
            if (files.contains("tokens.json"))
                tokens = Token.loadTokens(dataPath + "/tokens.json");
        } else {
            throw new IllegalArgumentException(dataPath + " is not a directory");
        }
    }

    public GridBoard findGridBoard(String name) {
        for (GridBoard c : gridBoards) {
            if (name.equalsIgnoreCase(c.getComponentName())) {
                return c.copy();
            }
        }
        return null;
    }

    public GraphBoard findGraphBoard(String name) {
        for (GraphBoard c : graphBoards) {
            if (name.equalsIgnoreCase(c.getComponentName())) {
                return c.copy();
            }
        }
        return null;
    }

    public Counter findCounter(String name) {
        for (Counter c : counters) {
            if (name.equalsIgnoreCase(c.getComponentName())) {
                return c.copy();
            }
        }
        return null;
    }

    public Token findToken(String name) {
        for (Token t : tokens) {
            if (name.equalsIgnoreCase(t.getComponentName())) {
                return t.copy();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> Deck<T> findDeck(String name) {
        for (Deck<?> d : decks) {
            if (name.equalsIgnoreCase(d.getComponentName())) {
                return (Deck<T>) d.copy();
            }
        }
        return null;
    }

    public AbstractGameData copy() {
        AbstractGameData retValue = new AbstractGameData();

        retValue.graphBoards = new ArrayList<>();
        for (GraphBoard b : graphBoards) retValue.graphBoards.add(b.copy());

        retValue.gridBoards = new ArrayList<>();
        for (GridBoard b : gridBoards) retValue.gridBoards.add(b.copy());

        retValue.decks = new ArrayList<>();
        for (Deck<Card> d : decks) retValue.decks.add(d.copy());

        retValue.counters = new ArrayList<>();
        for (Counter c : counters) retValue.counters.add(c.copy());

        retValue.tokens = new ArrayList<>();
        for (Token t : tokens) retValue.tokens.add(t.copy());

        return retValue;
    }
}
