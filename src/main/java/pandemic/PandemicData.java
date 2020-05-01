package pandemic;

import components.*;

import java.util.ArrayList;
import java.util.List;

public class PandemicData implements GameData {

    private List<Board> boards;
    private List<Deck> decks;
    private List<Token> tokens;
    private List<Counter> counters;

    @Override
    public void load(String dataPath)
    {
        boards = Board.loadBoards(dataPath + "boards.json");
        decks = Deck.loadDecks(dataPath + "decks.json");
        tokens = Token.loadTokens(dataPath + "tokens.json");
        counters = Counter.loadCounters(dataPath + "counters.json");
    }

    @Override
    public Board findBoard(String name) {
        for (Board c: boards) {
            if (name.equalsIgnoreCase(c.getNameID())) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Counter findCounter(String name) {
        for (Counter c: counters) {
            if (name.equalsIgnoreCase(c.getID())) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Token findToken(String name) {
        for (Token t: tokens) {
            if (name.equalsIgnoreCase(t.getNameID())) {
                return t;
            }
        }
        return null;
    }

    @Override
    public Deck findDeck(String name) {
        for (Deck d: decks) {
            if (name.equalsIgnoreCase(d.getID())) {
                return d;
            }
        }
        return null;
    }

    public PandemicData copy()
    {
        PandemicData pd = new PandemicData();

        pd.boards = new ArrayList<>();
        for(Board b : boards) pd.boards.add(b.copy());

        pd.decks = new ArrayList<>();
        for(Deck d : decks) pd.decks.add(d.copy());

        pd.tokens = new ArrayList<>();
        for(Token t : tokens) pd.tokens.add(t.copy());

        pd.counters = new ArrayList<>();
        for(Counter c : counters) pd.counters.add(c.copy());

        return pd;
    }

}
