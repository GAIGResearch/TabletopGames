package games.conquest;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.GridBoard;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.conquest.actions.*;
import games.conquest.components.Cell;
import games.conquest.components.Command;
import games.conquest.components.CommandType;
import games.conquest.components.Troop;
import utilities.Vector2D;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static gui.AbstractGUIManager.defaultItemSize;

/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class CQGameState extends AbstractGameState {
    public enum CQGamePhase implements IGamePhase {
        SelectionPhase,
        MovementPhase,
        CombatPhase,
        RallyPhase
    }
    Cell[][] cells;
    HashSet<Troop> troops;
    HashMap<Vector2D, Integer> locationToTroopMap;
    private int selectedTroop = -1; // The troop that is allowed to move and attack
    public Vector2D highlight = null; // the highlighted cell, whether or not it contains anything
    public Command cmdHighlight = null; // the highlighted command, for the current player.
    GridBoard<Cell> gridBoard;
    PartialObservableDeck[] chosenCommands;
    int[] commandPoints = new int[] {0, 0};
    Random random;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public CQGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        random = new Random(gameParameters.getRandomSeed());
    }

    public GridBoard<Cell> getGridBoard() {
        return gridBoard;
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.Conquest;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        // TODO: board squares
        ArrayList<Component> components = new ArrayList<>();
        // components.add(cells); // TODO: cells are not components.
        components.addAll(troops);
        return components;
    }

    public Cell getCell(int x, int y) {
        CQParameters cqp = (CQParameters) getGameParameters();
        if (x < 0 || y < 0 || x >= cqp.gridWidth || y >= cqp.gridHeight) {
            return null;
        }
        return cells[x][y];
    }
    public Cell getCell(Vector2D pos) {
        return getCell(pos.getX(), pos.getY());
    }

    public HashSet<Troop> getTroops(int uid) {
        return troops.stream().filter(t -> t.getOwnerId() == uid).collect(Collectors.toCollection(HashSet::new));
    }
    public PartialObservableDeck getCommands(int uid) {
        return chosenCommands[uid];
    }
    /**
     * See your own commands on/off cooldown
     * @param uid the user checking their own inactive commands
     * @param active whether the list should show the inactive or active commands
     * @return set of commands that are on cooldown
     */
    public HashSet<Command> getCommands(int uid, boolean active) {
        HashSet<Command> commands = new HashSet<>();
        if (chosenCommands == null) return commands;
        List<Command> list = chosenCommands[uid].getVisibleComponents(uid);
        for (Command cmd : list) {
            if ((active && cmd.getCooldown() == 0) || (!active && cmd.getCooldown() > 0))
                commands.add(cmd);
        }
        return commands;
    }

    /**
     * List actions that are available in the current game phase.
     * This will be called from the different extended action sequences, which would
     * otherwise have to duplicate code computing these available actions.
     * @return the full list of actions available at the current point in the game.
     */
    public List<AbstractAction> getAvailableActions() {
        int uid = getCurrentPlayer();
        CQGamePhase phase = (CQGamePhase) getGamePhase();
        List<AbstractAction> actions = new ArrayList<>();
        actions.add(new EndTurn());
        if (!getCommands(uid, true).isEmpty()) {
            for (Command c : getCommands(uid, true)) {
                actions.add(new ApplyCommand(uid, c));
            }
        }
        Troop currentTroop = selectedTroop == -1 ? null : (Troop) getComponentById(selectedTroop);
        if (phase.equals(CQGamePhase.SelectionPhase)) {
            for (Troop t : getTroops(uid)) {
                actions.add(new SelectTroop(uid, t.getLocation()));
            }
        } else if (phase.equals(CQGamePhase.MovementPhase)) {
            assert currentTroop != null;
            Vector2D pos = currentTroop.getLocation();
            int range = currentTroop.getMovement();
            for (int i = pos.getX() - range; i < pos.getX() + range; i++) {
                for (int j = pos.getY() - range; j < pos.getY() + range; j++) {
                    if (getTroopByLocation(pos) == null) continue; // can't move where another troop is located
                    if (getDistance(pos, new Vector2D(i,j)) > range) continue; // path is longer than crow's flight due to obstacle
                    actions.add(new MoveTroop(uid, new Vector2D(i,j)));
                }
            }
        } else if (phase.equals(CQGamePhase.CombatPhase)) {
            assert currentTroop != null;
            Cell c = getCell(currentTroop.getLocation());
            for (Troop t : getTroops(uid ^ 1)) {
                if (c.getCrowDistance(t.getLocation()) <= currentTroop.getRange()) {
                    actions.add(new AttackTroop(uid, c.position));
                }
            }
        }
        return actions;
    }
    public List<AbstractAction> getGenericActions() {
        CQGamePhase phase = (CQGamePhase) getGamePhase();
        List<AbstractAction> actions = new ArrayList<>();
        int uid = getCurrentPlayer();
        actions.add(new EndTurn());
        if (!getCommands(uid, true).isEmpty()) {
            actions.add(new ApplyCommand(uid));
        }
        if (phase.equals(CQGamePhase.SelectionPhase)) {
            actions.add(new SelectTroop(uid));
        } else if (!phase.equals(CQGamePhase.RallyPhase)) {
            actions.add(new AttackTroop(uid));
            if (phase.equals(CQGamePhase.MovementPhase)) {
                actions.add(new MoveTroop(uid));
            }
        }
        return actions;
    }

    public boolean useCommand(int uid, Command cmd) {
        cmd.use();
        int idx = getCommands(uid).getComponents().indexOf(cmd);
        if (idx == -1) return false;
        getCommands(uid).setVisibilityOfComponent(idx, uid ^ 1, true);
        return true;
    }
    public Troop getSelectedTroop() {
        return (Troop) getComponentById(selectedTroop);
    }
    public int getCommandPoints() {
        return getCommandPoints(getCurrentPlayer());
    }
    public int getCommandPoints(int uid) {
        assert uid == 0 || uid == 1;
        return commandPoints[uid];
    }
    public void gainCommandPoints(int uid, int points) {
        assert points > 0;
        spendCommandPoints(uid, -points);
    }
    public boolean spendCommandPoints(int uid, int points) {
        assert points > 0;
        if (getCommandPoints(uid) >= points) {
            commandPoints[uid] -= points;
            return true;
        } else {
            return false;
        }
    }

    public void setSelectedTroop(int selectedTroop) {
        assert gamePhase == CQGamePhase.SelectionPhase;
        this.selectedTroop = selectedTroop;
    }

    public int getDistance(Cell from, Cell to) {
        return getDistance(from.position, to.position);
    }
    public int getDistance(Vector2D from, Vector2D to) {
        // TODO: use locationToTroopMap with A*
        return getDistance(from, to, locationToTroopMap);
    }
    public static int getDistance(Vector2D from, Vector2D to, HashMap<Vector2D, Integer> map) {
        return Math.max(Math.abs(from.getX() - to.getX()), Math.abs(from.getY() - to.getY()));
    }

    public Troop getTroopByLocation(Vector2D vec) {
        Integer id = locationToTroopMap.get(vec);
        if (id == null) return null;
        Troop troop = (Troop) getComponentById(id);
        if (troop.getHealth() <= 0) return null; // troop has died; is no longer relevant
        return troop;
    }
    public Troop getTroopByLocation(Cell c) {
        return getTroopByLocation(c.position);
    }
    public Troop getTroopByLocation(int x, int y) {
        return getTroopByLocation(new Vector2D(x, y));
    }
    public Troop getTroopByRect(Rectangle r) {
        return getTroopByLocation(getLocationByRect(r));
    }
    public Vector2D getLocationByRect(Rectangle r) {
        int x = (int) (r.getX() / defaultItemSize);
        int y = (int) (r.getY() / defaultItemSize);
        return new Vector2D(x, y);
    }

    public HashMap<Vector2D, Integer> getLocationToTroopMap() {
        return locationToTroopMap;
    }

    /**
     * Check if an action is allowed to be performed on the target. This verifies the different conditions for different actions.
     * @param action Which action to check
     * @return can the action be performed or not
     */
    public boolean canPerformAction(AbstractAction action) {
        Troop target = getTroopByLocation(highlight);
        Troop selected = getSelectedTroop();
        IGamePhase phase = getGamePhase();
        if (action instanceof EndTurn) {
            return true;
        } else if (action instanceof ApplyCommand) {
            if (cmdHighlight == null) return false;
            if (cmdHighlight.getCooldown() > 0) return false; // no activating an inactive command
            if (cmdHighlight.getCost() > getCommandPoints()) return false; // no activating expensive commands
            CommandType cmdType = cmdHighlight.getCommandType();
            if (cmdType == CommandType.WindsOfFate) {
                return !getCommands(getCurrentPlayer(), false).isEmpty();
            } else if (target == null) return false; // all other commands need to be applied to a target
            if ((target.getOwnerId() == getCurrentPlayer()) ^ !cmdType.enemy) return false; // apply on self XOR use enemy-targeting command
            if (cmdType == CommandType.Charge && (
                    getGamePhase() == CQGamePhase.CombatPhase || getGamePhase() == CQGamePhase.RallyPhase
            )) {
                // no use in applying Charge on a troop after it has already moved; prevent this from happening to aid MCTS
                return false;
            }
            return true;
        } else if (target == null && action instanceof MoveTroop) {
            if (phase != CQGamePhase.MovementPhase || selected == null) return false; // Other phases don't ever allow movement
            // Check if movement is allowed: calculate distance
            int dist = getDistance(selected.getLocation(), highlight);
            return dist <= selected.getMovement(); // allowed if within movement range, otherwise not
        } else if (target != null) {
            if (action instanceof SelectTroop) {
                if (phase != CQGamePhase.SelectionPhase) return false;
                return target.getOwnerId() == getCurrentPlayer(); // allow if the selected troop is owned by current player
            } else if (action instanceof AttackTroop) {
                if (phase == CQGamePhase.RallyPhase || selected == null) return false; // you can attack in MovementPhase or CombatPhase
                if (target.getOwnerId() == getCurrentPlayer()) return false; // don't attack your own troops
                Cell targetCell = getCell(target.getLocation());
                return targetCell.getCrowDistance(selected.getLocation()) <= selected.getRange(); // only allowed if within attack range
            }
        }
        // no conditions found to allow this action => disallow
        return false;
    }

    public boolean moveTroop(int troop, Vector2D from, Vector2D to) {
        if (locationToTroopMap.get(from) == null) return false;
        locationToTroopMap.remove(from);
        locationToTroopMap.put(to, troop);
        return true;
    }

    public void endTurn() {
        setGamePhase(CQGamePhase.SelectionPhase);
        selectedTroop = -1;
        for (Troop troop : troops) {
            troop.step(getCurrentPlayer());
        }
        int nextPlayer = getCurrentPlayer() ^ 1;
        commandPoints[nextPlayer] += 25;
        List<Command> list = chosenCommands[nextPlayer].getComponents();
        for (Command cmd : list) {
            cmd.step();
        }
    }
    /**
     * <p>Create a deep copy of the game state containing only those components the given player can observe.</p>
     * <p>If the playerID is NOT -1 and If any components are not visible to the given player (e.g. cards in the hands
     * of other players or a face-down deck), then these components should instead be randomized (in the previous examples,
     * the cards in other players' hands would be combined with the face-down deck, shuffled together, and then new cards drawn
     * for the other players). This process is also called 'redeterminisation'.</p>
     * <p>There are some utilities to assist with this in utilities.DeterminisationUtilities. One firm is guideline is
     * that the standard random number generator from getRnd() should not be used in this method. A separate Random is provided
     * for this purpose - redeterminisationRnd.
     *  This is to avoid this RNG stream being distorted by the number of player actions taken (where those actions are not themselves inherently random)</p>
     * <p>If the playerID passed is -1, then full observability is assumed and the state should be faithfully deep-copied.</p>
     *
     * <p>Make sure the return type matches the class type, and is not AbstractGameState.</p>
     *
     * @param playerId - player observing this game state.
     */
    @Override
    protected CQGameState _copy(int playerId) {
        CQGameState copy = new CQGameState(getGameParameters(), getNPlayers());
        // TODO: make deep copies of troops and commands?
        copy.cells = cells;
        copy.troops = (HashSet<Troop>) troops.clone();
        copy.locationToTroopMap = (HashMap<Vector2D, Integer>) locationToTroopMap.clone();
        copy.selectedTroop = selectedTroop;
        copy.highlight = highlight;
        copy.cmdHighlight = cmdHighlight;
        copy.gridBoard = gridBoard.copy();
        copy.chosenCommands = chosenCommands.clone();
        copy.commandPoints = commandPoints.clone();
        return copy;
    }

    /**
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            // TODO: Calculate the amount of troops killed on each side
            // TODO: Calculate amount of health lost on each side
            // TODO: Calculate amount of command points on each side
            // TODO: adjust score based on cooldown of commands
            return 0;
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected boolean _equals(Object o) {
        // TODO: compare all variables in the state
        return o instanceof CQGameState;
    }

    @Override
    public int hashCode() {
        // TODO: include the hash code of all variables
        return super.hashCode();
    }
    // TODO: Review the methods below...these are all supported by the default implementation in AbstractGameState
    // TODO: So you do not (and generally should not) implement your own versions - take advantage of the framework!
    public Random getRnd() {
        return random;
    }
    public int getFirstPlayer() {
        // TODO: Doesn't correctly select the first player
        return getRnd().nextInt(2);
    }

    // This method can be used to log a game event (e.g. for something game-specific that you want to include in the metrics)
    // public void logEvent(IGameEvent...)
}
