package games.descent2e;

import core.AbstractGameStateWithTurnOrder;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.*;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import core.turnorders.TurnOrder;
import games.GameType;
import games.descent2e.actions.DescentAction;
import games.descent2e.components.*;
import games.descent2e.components.tokens.DToken;
import games.descent2e.actions.Triggers;
import games.descent2e.concepts.Quest;
import utilities.Vector2D;

import java.util.*;
import java.util.stream.Collectors;

import static games.GameType.Descent2e;

public class DescentGameState extends AbstractGameStateWithTurnOrder implements IPrintable {

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

    // Important
    Random rnd;

    Deck<Card> searchCards;
    GridBoard masterBoard;
    DicePool attackDicePool;
    DicePool defenceDicePool;
    DicePool attributeDicePool;
    List<Hero> heroes;
    Figure overlord;
    List<List<Monster>> monsters;
    int overlordPlayer;
    ArrayList<DToken> tokens;
    Quest currentQuest;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players for this game.
     */
    public DescentGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        tiles = new HashMap<>();
        data = new DescentGameData();
        attackDicePool = new DicePool(Collections.emptyList());
        defenceDicePool = new DicePool(Collections.emptyList());
        attributeDicePool = new DicePool(Collections.emptyList());

        heroes = new ArrayList<>();
        monsters = new ArrayList<>();
        rnd = new Random(gameParameters.getRandomSeed());
    }

    @Override
    protected TurnOrder _createTurnOrder(int nPlayers) {
        return new DescentTurnOrder(nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return Descent2e;
    }

    @Override
    protected List<Component> _getAllComponents() {
        ArrayList<Component> components = new ArrayList<>();
        if (!initData) {
            components.addAll(data.decks);
            components.addAll(data.tiles);
            components.addAll(data.heroes);
            components.addAll(data.boardConfigurations);
            for (HashMap<String, Monster> m : data.monsters.values()) {
                components.addAll(m.values());
            }
            initData = true;
        }

        // Current state
        components.add(masterBoard);
        if (tokens != null) {
            components.addAll(tokens);
        }
        components.add(searchCards);
        components.addAll(heroes);
        monsters.forEach(components::addAll);
        components.add(overlord);
        // TODO
        return components;
    }

    @Override
    protected AbstractGameStateWithTurnOrder __copy(int playerId) {
        DescentGameState copy = new DescentGameState(gameParameters, getNPlayers());
        copy.tiles = new HashMap<>(tiles);  // TODO: deep copy
        copy.masterBoard = masterBoard.copy();
        copy.overlord = overlord.copy();
        copy.heroes = new ArrayList<>();
        for (Hero f : heroes) {
            copy.heroes.add(f.copy());
        }
        copy.monsters = new ArrayList<>();
        for (List<Monster> ma : monsters) {
            List<Monster> maC = new ArrayList<>();
            for (Monster m : ma) {
                maC.add(m.copy());
            }
            copy.monsters.add(maC);
        }
        copy.tileReferences = tileReferences.clone();  // TODO deep
        copy.gridReferences = new HashMap<>(gridReferences); // TODO deep
        copy.initData = initData;
        copy.tokens = new ArrayList<>();
        for (DToken t : tokens) {
            copy.tokens.add(t.copy());
        }
        copy.searchCards = searchCards.copy();
        copy.currentQuest = currentQuest;  // TODO does this need to be deep? it (should be) read-only after data parsing
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
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DescentGameState)) return false;
        if (!super.equals(o)) return false;
        DescentGameState that = (DescentGameState) o;
        return initData == that.initData && overlordPlayer == that.overlordPlayer &&
                Objects.equals(data, that.data) && Objects.equals(tiles, that.tiles) &&
                Arrays.equals(tileReferences, that.tileReferences) &&
                Objects.equals(gridReferences, that.gridReferences) &&
                Objects.equals(searchCards, that.searchCards) &&
                Objects.equals(masterBoard, that.masterBoard) &&
                Objects.equals(attackDicePool, that.attackDicePool) &&
                Objects.equals(defenceDicePool, that.defenceDicePool) &&
                Objects.equals(attributeDicePool, that.attributeDicePool) &&
                Objects.equals(heroes, that.heroes) && Objects.equals(overlord, that.overlord) &&
                Objects.equals(monsters, that.monsters) && Objects.equals(tokens, that.tokens);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), data, tiles, gridReferences, initData, searchCards,
                masterBoard, attackDicePool, defenceDicePool, attributeDicePool, heroes, overlord, monsters, overlordPlayer, tokens);
        result = 31 * result + Arrays.deepHashCode(tileReferences);
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

    public DicePool getAttackDicePool() {
        return attackDicePool;
    }
    public DicePool getDefenceDicePool() {return defenceDicePool;}
    public DicePool getAttributeDicePool() {return attributeDicePool;}
    public void setAttackDicePool(DicePool pool) {attackDicePool = pool;}
    public void setDefenceDicePool(DicePool pool) {defenceDicePool = pool;}
    public void setAttributeDicePool(DicePool pool) {attributeDicePool = pool;}

    public List<List<Monster>> getMonsters() {
        return monsters;
    }

    public Deck<Card> getSearchCards() {
        return searchCards;
    }

    /*
    This method is fine when we are circulating through each hero and monster in turn for taking their actions
    It will not necessarily suffice when we have interrupt actions. Consider the situation in which one player has two
    heroes, and one of the heroes has an ability that allows a monster move to be interrupted.
    In this case the Action (interrupt Move ability) should encapsulate within it the Figure that is
    executing the action (if this is at all relevant).
     */
    public Figure getActingFigure() {
        // Find current monster group + monster playing
        int monsterGroupIdx = ((DescentTurnOrder) getTurnOrder()).monsterGroupActingNext;
        List<Monster> monsterGroup = getMonsters().get(monsterGroupIdx);
        int nextMonster = ((DescentTurnOrder) getTurnOrder()).monsterActingNext;

        // Find currently acting figure (hero or monster)
        Figure actingFigure;
        // TODO Ensure monsterGroup deletes itself when empty
        if (getCurrentPlayer() != overlordPlayer || monsterGroup.isEmpty()) {
            // If hero player, get corresponding hero
            actingFigure = getHeroes().get(((DescentTurnOrder)getTurnOrder()).heroFigureActingNext);
        } else {
            // Otherwise, monster is playing
            actingFigure = monsterGroup.get(nextMonster);
        }
        return actingFigure;
    }
    public int getActingPlayer() {
        return getActingFigure().getOwnerId();
    }

    public int getOverlordPlayer() {
        return overlordPlayer;
    }

    public Figure getOverlord() {
        return overlord;
    }

    public List<AbstractAction> getInterruptActionsFor(int player, Triggers trigger) {
        List<DescentAction> descentActions;
        if (player == overlordPlayer) {
            // we run through monsters
            descentActions = monsters.stream().flatMap(List::stream)
                    .flatMap(m -> m.getAbilities().stream())
                    .collect(Collectors.toList());
        } else {
            // else we just look at heroes that belong to the acting Figure
            // TODO: Add in effects from cards in the player's hand
            // We rely on canExecute() to filter out irrelevant ones
            descentActions = heroes.stream().filter(h -> h.getOwnerId() == player)
                    .flatMap(h -> h.getAbilities().stream())
                    .collect(Collectors.toList());
        }
        // Then filter to just the ones applicable to this Trigger point that are executable
        List<AbstractAction> retValue = descentActions.stream()
                .filter(a -> a.canExecute(trigger, this))
                .map(a -> (AbstractAction) a).collect(Collectors.toList());
        return retValue;
    }

    public int[][] getTileReferences() {
        return tileReferences;
    }

    public Map<String, Map<Vector2D, Vector2D>> getGridReferences() {
        return gridReferences;
    }

    public Map<Integer, GridBoard> getTiles() {
        return tiles;
    }

    public List<DToken> getTokens() {
        return tokens;
    }

    public Quest getCurrentQuest() {
        return currentQuest;
    }

    @Override
    public String toString() {
        return masterBoard.toString();
    }
}
