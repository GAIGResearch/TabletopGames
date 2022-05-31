package games.descent2e;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Component;
import core.components.GridBoard;
import core.components.Token;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import games.GameType;
import games.descent2e.components.*;
import games.descent2e.components.tokens.DToken;
import games.descent2e.actions.Triggers;
import utilities.Vector2D;

import java.util.*;

public class DescentGameState extends AbstractGameState implements IPrintable {

    public enum DescentPhase implements IGamePhase {
        ForceMove  // Used when a figure started a (possibly valid move action) and is currently overlapping a friendly figure
    }
    DescentGameData data;

    // For reference only

    // Mapping from board node ID in board configuration to tile configuration
    Map<Integer, GridBoard> tiles;
    // int corresponds to component ID of tile at that location in master board
    int[][] tileReferences;
    // Mapping from tile name to list of coordinates in master board for each cell (and corresponding coordinates on original tile)
    Map<String, Map<Vector2D, Vector2D>> gridReferences;
    boolean initData;
    Random rnd;


    GridBoard masterBoard;
    List<DescentDice> dice;
    DicePool dicePool;
    List<Hero> heroes;
    Figure overlord;
    List<List<Monster>> monsters;
    int overlordPlayer;
    ArrayList<DToken> tokens;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players for this game.
     */
    public DescentGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new DescentTurnOrder(nPlayers), GameType.Descent2e);
        tiles = new HashMap<>();
        data = new DescentGameData();
        dice = new ArrayList<>();
        dicePool = new DicePool(Collections.emptyList());

        heroes = new ArrayList<>();
        monsters = new ArrayList<>();
        rnd = new Random(gameParameters.getRandomSeed());
    }

    @Override
    protected List<Component> _getAllComponents() {
        ArrayList<Component> components = new ArrayList<>();
        if (!initData) {
            // Data, only add once at the start to have ready for fm compute
            components.addAll(data.decks);
            components.addAll(data.tiles);
            components.addAll(data.heroes);
            components.addAll(data.boardConfigurations);
            components.addAll(data.dice);
            for (HashMap<String, Token> m : data.monsters.values()) {
                components.addAll(m.values());
            }
            initData = true;
        }
        // Current state
        components.add(masterBoard);
        if (tokens != null) {
            components.addAll(tokens);
        }
        // TODO
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        DescentGameState copy = new DescentGameState(gameParameters, getNPlayers());
        copy.tiles = new HashMap<>(tiles);  // TODO: deep copy
        copy.masterBoard = masterBoard.copy();
        copy.overlord = overlord.copy();
        copy.heroes = new ArrayList<>();
        for (Hero f: heroes) {
            copy.heroes.add(f.copy());
        }
        copy.monsters = new ArrayList<>();
        for (List<Monster> ma: monsters) {
            List<Monster> maC = new ArrayList<>();
            for (Monster m: ma) {
                maC.add(m.copy());
            }
            copy.monsters.add(maC);
        }
        copy.tileReferences = tileReferences.clone();  // TODO deep
        copy.gridReferences = new HashMap<>(gridReferences); // TODO deep
        copy.initData = initData;
        copy.tokens = new ArrayList<>();
        for (DToken t: tokens) {
            copy.tokens.add(t.copy());
        }
        // TODO: Why copy Dice - aren't these immutable?
        for (DescentDice d : dice) {
            copy.dice.add(d.copy());
        }

        copy.rnd = new Random(rnd.nextLong());
        // TODO
        return copy;
    }

    public Random getRandom() {
        return rnd;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        // TODO
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        // TODO
        return 0;
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        // TODO
        return null;
    }

    @Override
    protected void _reset() {
        // TODO
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DescentGameState)) return false;
        if (!super.equals(o)) return false;
        DescentGameState that = (DescentGameState) o;
        return overlordPlayer == that.overlordPlayer &&
                Objects.equals(tiles, that.tiles) &&
                Arrays.equals(tileReferences, that.tileReferences) &&
                Objects.equals(gridReferences, that.gridReferences) &&
                Objects.equals(masterBoard, that.masterBoard) &&
                Objects.equals(heroes, that.heroes) &&
                Objects.equals(overlord, that.overlord) &&
                Objects.equals(monsters, that.monsters) &&
                Objects.equals(dice, that.dice);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), tiles, gridReferences, masterBoard, heroes, overlord, dice, monsters, overlordPlayer);
        result = 31 * result + Arrays.hashCode(tileReferences);
        return result;
    }

    DescentGameData getData() {
        return data;
    }

    public GridBoard getMasterBoard() {
        return masterBoard;
    }

    public List<Hero> getHeroes() {
        return heroes;
    }

    public List<DescentDice> getDice(){return dice;}

    public DicePool getDicePool(){
        return dicePool;
    }

    public void setDicePool(DicePool pool){
        dicePool = pool;
    }

    public List<List<Monster>> getMonsters() {
        return monsters;
    }

    public Figure getActingFigure() {
        // Find current monster group + monster playing
        int monsterGroupIdx = ((DescentTurnOrder) getTurnOrder()).monsterGroupActingNext;
        List<Monster> monsterGroup = getMonsters().get(monsterGroupIdx);
        int nextMonster = ((DescentTurnOrder) getTurnOrder()).monsterActingNext;

        // Find currently acting figure (hero or monster)
        Figure actingFigure;
        if (getCurrentPlayer() != 0) {
            // If hero player, get corresponding hero
            actingFigure = getHeroes().get(getCurrentPlayer() - 1);
        } else {
            // Otherwise, monster is playing
            actingFigure = monsterGroup.get(nextMonster);
        }
        return actingFigure;
    }

    public boolean playerHasAvailableInterrupt(int player, Triggers trigger) {
        // TODO: implement with look through Abilities/Items/Actions which fit
        return false;
    }
    public List<AbstractAction> getInterruptActionsFor(int player, Triggers trigger) {
        List<AbstractAction> retValue = new ArrayList<>();
        // TODO: Run through the inventory or items/cards/abilities to see which have
        // an action that can be used at this trigger
        retValue.add(new DoNothing());
        return retValue;
    }

    public int[][] getTileReferences() {
        return tileReferences;
    }

    public Map<String, Map<Vector2D, Vector2D>> getGridReferences() {
        return gridReferences;
    }

    public List<DToken> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        return masterBoard.toString();
    }
}
