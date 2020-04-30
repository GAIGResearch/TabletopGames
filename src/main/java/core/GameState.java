package core;

import actions.Action;
import components.*;
import java.util.*;

import static pandemic.Constants.GAME_ONGOING;

/**
 * Placeholder class. Will contain all game state information.
 */
public abstract class GameState {

    protected int activePlayer;  // Player who's currently taking a turn, index from player list, N+1 is game master, -1 is game
    protected ArrayList<Integer> reactivePlayers;
    protected int nPlayers;
    public int roundStep;

    protected HashMap<Integer, Area> areas;
    protected List<Board> boards;
    protected List<IDeck> decks;
    protected List<Token> tokens;
    protected List<Counter> counters;
    protected List<Dice> dice;

    /**
     * Forward model of this game. Logic and setup.
     */
    protected ForwardModel forwardModel;

    /**
     * Set of parameters for this game.
     */
    protected GameParameters gameParameters;


    protected int gameStatus = GAME_ONGOING;

    public GameState copy() {
        GameState gsCopy = _copy(-1);
        this.copyTo(gsCopy, -1);
        return gsCopy;
    }

    protected GameState _copy(int playerId)
    {
        GameState gsCopy = this.createNewGameState();

        gsCopy.activePlayer = activePlayer;
        gsCopy.nPlayers = nPlayers;
        gsCopy.roundStep = roundStep;

        gsCopy.areas = new HashMap<>();
        for(int key : areas.keySet())
        {
            Area a = areas.get(key);
            gsCopy.areas.put(key, a.copy());
        }

        gsCopy.boards = new ArrayList<>();
        for(Board b : boards) gsCopy.boards.add(b.copy());

        gsCopy.decks = new ArrayList<>();
        for(IDeck d : decks) gsCopy.decks.add(d.copy());

        gsCopy.tokens = new ArrayList<>();
        for(Token t : tokens) gsCopy.tokens.add(t.copy());

        gsCopy.counters = new ArrayList<>();
        for(Counter c : counters) gsCopy.counters.add(c.copy());

        gsCopy.dice = new ArrayList<>();
        for(Dice d : dice) gsCopy.dice.add(d.copy());

        gsCopy.forwardModel = forwardModel.copy();
        gsCopy.gameStatus = gameStatus;
        gsCopy.gameParameters = gameParameters.copy();

        return gsCopy;
    }


    /**
     * Creates a new GameState object.
     * @return the new GameState object
     */
    public abstract GameState createNewGameState();

    /**
     * Copies the game state objects defined in the subclass of this game state to a
     * new GameState object and returns it.
     * @param dest      GameState where things need to be copied to.
     * @param playerId ID of the player for which this copy is being created (so observations can be
     *                 adapted for them). -1 indicates the game state should be copied at full.
     */
    public abstract void copyTo(GameState dest, int playerId);


    public final void init()
    {
        areas = new HashMap<>();  // Game State has areas! Initialize.
        reactivePlayers = new ArrayList<>();
    }

    public abstract void setComponents();

    public void load(String dataPath)
    {
        boards = Board.loadBoards(dataPath + "boards.json");
        decks = Deck.loadDecks(dataPath + "decks.json");
        tokens = Token.loadTokens(dataPath + "tokens.json");
        counters = Counter.loadCounters(dataPath + "counters.json");
//        dice  = Dice.loadDice(dataPath + "dice.json");  // TODO
    }


    public void next(Action action) {
        forwardModel.next(this, action);
    }

    public void addDeckToList(Deck deck){
        this.decks.add(deck);
    }

    public Board findBoard(String name) {
        for (Board c: boards) {
            if (name.equalsIgnoreCase(c.getNameID())) {
                return c;
            }
        }
        return null;
    }

    public Counter findCounter(String name) {
        for (Counter c: counters) {
            if (name.equalsIgnoreCase(c.getID())) {
                return c;
            }
        }
        return null;
    }

    public Token findToken(String name) {
        for (Token t: tokens) {
            if (name.equalsIgnoreCase(t.getNameID())) {
                return t;
            }
        }
        return null;
    }

    public IDeck findDeck(String name) {
        for (IDeck d: decks) {
            if (name.equalsIgnoreCase(d.getID())) {
                return d;
            }
        }
        return null;
    }

    public String tempDeck() {
        IDeck temp = findDeck("tempDeck");
        if (temp == null) {
            temp = new Deck("tempDeck");
            decks.add(temp);
        } else {
            temp.clear();
        }
        return "tempDeck";
    }

    public void clearTempDeck() {
        tempDeck();
    }

    //Getters & setters
    public int getGameStatus() {  return gameStatus; }
    void setForwardModel(ForwardModel fm) { this.forwardModel = fm; }
    ForwardModel getModel() {return this.forwardModel;}
    public GameParameters getGameParameters() { return this.gameParameters; }
    void setGameParameters(GameParameters gp) { this.gameParameters = gp; }
    public void setGameOver(int status){  this.gameStatus = status; }
    public int getActingPlayer() {  // Returns player taking an action (or possibly a reaction) next
        if (reactivePlayers.size() == 0)
            return activePlayer;
        else return reactivePlayers.get(0);
    }
    public int getActivePlayer() { return activePlayer; }  // Returns the player whose turn it is, might not be active player
    public ArrayList<Integer> getReactivePlayers() { return reactivePlayers; }  // Returns players queued to react
    public void addReactivePlayer(int player) { reactivePlayers.add(player); }
    public boolean removeReactivePlayer() {
        if (reactivePlayers.size() > 0) {
            reactivePlayers.remove(0);
            return true;
        }
        return false;
    }
    public HashMap<Integer, Area> getAreas() { return areas; }
    public int getNPlayers() { return nPlayers; }
    public void setNPlayers(int nPlayers) { this.nPlayers = nPlayers; }

    /* Methods to be implemented by subclass */
    public abstract int nPossibleActions();
    public abstract List<Action> possibleActions(List<Action> preDetermined);

}
