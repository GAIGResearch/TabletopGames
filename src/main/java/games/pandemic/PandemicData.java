package games.pandemic;

import core.AbstractGameData;
import core.components.*;

import java.util.ArrayList;
import java.util.List;

public class PandemicData extends AbstractGameData<Card> {

    private List<GraphBoard> graphBoards;
    private List<Deck<Card>> decks;
    private List<Token> tokens;
    private List<Counter> counters;

    @Override
    public void load(String dataPath)
    {
        graphBoards = GraphBoard.loadBoards(dataPath + "boards.json");
        decks = Deck.loadDecksOfCards(dataPath + "decks.json");
        tokens = Token.loadTokens(dataPath + "tokens.json");
        counters = Counter.loadCounters(dataPath + "counters.json");
    }

    @Override
    public GraphBoard findBoard(String name) {
        for (GraphBoard c: graphBoards) {
            if (name.equalsIgnoreCase(c.getComponentName())) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Counter findCounter(String name) {
        for (Counter c: counters) {
            if (name.equalsIgnoreCase(c.getComponentName())) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Deck<Card> findDeck(String name) {
        for (Deck<Card> d: decks) {
            if (name.equalsIgnoreCase(d.getComponentName())) {
                return d;
            }
        }
        return null;
    }

    public PandemicData copy()
    {
        PandemicData pd = new PandemicData();

        pd.graphBoards = new ArrayList<>();
        for(GraphBoard b : graphBoards) pd.graphBoards.add(b.copy());

        pd.decks = new ArrayList<>();
        for(Deck<Card> d : decks) pd.decks.add(d.copy());

        pd.tokens = new ArrayList<>();
        for(Token t : tokens) pd.tokens.add(t.copy());

        pd.counters = new ArrayList<>();
        for(Counter c : counters) pd.counters.add(c.copy());

        return pd;
    }

}
