package games.conquest;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
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
import org.jetbrains.annotations.NotNull;
import utilities.Vector2D;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static gui.AbstractGUIManager.defaultItemSize;

// TODO: implement actionHeuristic?

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
    public int cmdHighlight = -1; // the highlighted command, for the current player.
    GridBoard<Cell> gridBoard;
    PartialObservableDeck[] chosenCommands;
    int[] commandPoints = new int[] {0, 0};

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public CQGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    /*======= SIMPLE GETTERS AND SETTERS =======*/
    public GridBoard<Cell> getGridBoard() {
        return gridBoard;
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
    /**
     * Gets all troops, including dead troops, for a given owner
     * @param uid Player to list troops for
     * @return A set of troops for the given owner
     */
    public HashSet<Troop> getAllTroops(int uid) {
        return troops
                .stream()
                .filter(t -> t.getOwnerId() == uid)
                .collect(Collectors.toCollection(HashSet::new));
    }
    /**
     * Gets troops owned by `uid` that are alive
     * @param uid Player to list troops for
     * @return A set of troops for the given owner
     */
    public HashSet<Troop> getTroops(int uid) {
        return troops
                .stream()
                .filter(t -> t.getOwnerId() == uid && t.isAlive())
                .collect(Collectors.toCollection(HashSet::new));
    }
    public PartialObservableDeck getCommands(int uid) {
        return chosenCommands[uid];
    }
    public Troop getSelectedTroop() {
        return (Troop) getComponentById(selectedTroop);
    }
    public int getCommandPoints() {
        return getCommandPoints(getCurrentPlayer());
    }
    public int getCommandPoints(int uid) {
        return commandPoints[uid];
    }
    public void setSelectedTroop(int selectedTroop) {
        assert gamePhase == CQGamePhase.SelectionPhase;
        this.selectedTroop = selectedTroop;
    }
    public HashMap<Vector2D, Integer> getLocationToTroopMap() {
        return locationToTroopMap;
    }
    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.Conquest;
    }

    /*======= ADVANCED GETTERS AND SETTERS =======*/
    public Troop getTroopByLocation(Vector2D vec) {
        Integer id = locationToTroopMap.get(vec);
        if (id == null) return null;
        Troop troop = (Troop) getComponentById(id);
        if (!troop.isAlive()) return null; // troop has died; is no longer relevant
        return troop;
    }
    public Troop getTroopByLocation(@NotNull Cell c) {
        return getTroopByLocation(c.position);
    }
    public Troop getTroopByLocation(int x, int y) {
        return getTroopByLocation(new Vector2D(x, y));
    }
    public Troop getTroopByRect(Rectangle r) {
        return getTroopByLocation(getLocationByRect(r));
    }
    public Vector2D getLocationByRect(@NotNull Rectangle r) {
        int x = (int) (r.getX() / defaultItemSize);
        int y = (int) (r.getY() / defaultItemSize);
        return new Vector2D(x, y);
    }
    /**
     * See your own commands on/off cooldown
     * @param uid the user checking their own inactive commands
     * @param active whether the list should show the inactive or active commands.
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
     * Returns all Components used in the game
     * and referred to by componentId from actions or rules.
     * This method is called after initialising the game state,
     * so all components will be initialised already.
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        ArrayList<Component> components = new ArrayList<>();
        for (Cell[] cell : cells) {
            components.addAll(List.of(cell));
        }
        components.addAll(troops);
        components.addAll(chosenCommands[0].getComponents());
        components.addAll(chosenCommands[1].getComponents());
        return components;
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
        if (!phase.equals(CQGamePhase.SelectionPhase))
            actions.add(new EndTurn());
        int hash = this.hashCode();
        Troop currentTroop = selectedTroop == -1 ? null : (Troop) getComponentById(selectedTroop);
        if (!getCommands(uid, true).isEmpty()) {
            for (Command c : getCommands(uid, true)) {
                int commandId = c.getComponentID();
                if (c.getCost() > getCommandPoints(uid)) continue;
                if (c.getCommandType() == CommandType.WindsOfFate) {
                    ApplyCommand cmd = new ApplyCommand(uid, commandId, (Vector2D) null, c.getCommandType(), hash);
                    if (cmd.canExecute(this)) {
                        actions.add(cmd); // only add it if it can execute
                    }
                } else {
                    for (Troop t : getTroops(c.getCommandType().enemy ? uid ^ 1 : uid)) {
                        // if Winds of Fate caused immediate cooldown of a command, you still can't apply it twice to the same troop
                        if (!t.getAppliedCommands().contains(c.getCommandType())) {
                            ApplyCommand cmd = new ApplyCommand(uid, commandId, t, c.getCommandType(), hash);
                            if (cmd.canExecute(this)) {
                                actions.add(cmd);
                            }
                        }
                    }
                }
            }
        }
        if (phase.equals(CQGamePhase.SelectionPhase)) {
            for (Troop t : getTroops(uid)) {
                SelectTroop sel = new SelectTroop(uid, t.getLocation(), hash);
                if (canPerformAction(sel, false))
                    actions.add(sel);
            }
        } else if (phase.equals(CQGamePhase.MovementPhase)) {
            assert currentTroop != null;
            Vector2D pos = currentTroop.getLocation();
            int range = currentTroop.getMovement();
            if (!currentTroop.hasMoved()) // TODO: If the player is the gui player, it's nice to let them move multiple times.
                for (int i = Math.max(0, pos.getX() - range); i <= Math.min(gridBoard.getWidth(), pos.getX() + range); i++) {
                    for (int j = Math.max(0, pos.getY() - range); j <= Math.min(gridBoard.getHeight(), pos.getY() + range); j++) {
                        MoveTroop mov = new MoveTroop(uid, new Vector2D(i,j), hash);
                        if (canPerformAction(mov, false))
                            actions.add(mov);
                    }
                }
        }
        if (phase.equals(CQGamePhase.MovementPhase) || phase.equals(CQGamePhase.CombatPhase)) {
            assert currentTroop != null;
            Cell c = getCell(currentTroop.getLocation());
            for (Troop t : getTroops(uid ^ 1)) {
                AttackTroop atk = new AttackTroop(uid, t.getLocation(), hash);
                if (canPerformAction(atk, false)) {
                    actions.add(atk);
                }
            }
        }
        return actions;
    }
    public static double getRelativeTroopCost(@NotNull HashSet<Troop> troops) {
        double cost = 0;
        for (Troop troop : troops) {
            cost += troop.getTroopType().cost * troop.getUnboostedHealth() / (double) troop.getTroopType().health;
        }
        return cost;
    }
    public static int getTotalTroopCost(@NotNull HashSet<Troop> troops) {
        int cost = 0;
        for (Troop troop : troops) {
            cost += troop.getTroopType().cost;
        }
        return cost;
    }

    /**
     * Calculate a figure describing how many of a player's troops are alive, and if so, with how much health.
     * If a troop is dead, it counts as negative points scaled to their cost; if a troop is alive, it's positive points,
     * scaled to their cost and their current relative health.
     * @param playerId The player to check troop health for
     * @return the metric for alive-ness of troops
     */
    public double getTroopHealthMetric(int playerId) {
        HashSet<Troop> allTroops = getAllTroops(playerId);
        HashSet<Troop> livingTroops = getTroops(playerId);
        int totalTroopCost = getTotalTroopCost(allTroops);
        int livingTroopCost = getTotalTroopCost(livingTroops);
        // dead troops is always worse than living troops; also, 2 champ at 100hp is better than 1 champ at 200hp.
        double relativeTroopCost = getRelativeTroopCost(livingTroops) / totalTroopCost;
        double deadTroopCost = (totalTroopCost - livingTroopCost) / (double) totalTroopCost;
        // dead troops count as negative score; alive troop count as positive score, based on their health.
        // all troops being dead counts as -1; all troops being full health counts as 1.
        return -deadTroopCost + relativeTroopCost;
    }

    /*======= METHODS THAT EXECUTE SOME ACTION =======*/
    public boolean useCommand(int uid, @NotNull Command cmd) {
        int idx = getCommands(uid).getComponents().indexOf(cmd);
        cmd.use();
        if (idx == -1) return false;
        getCommands(uid).setVisibilityOfComponent(idx, uid ^ 1, true);
        return true;
    }
    public void gainCommandPoints(int uid, int points) {
        assert points >= 0;
        spendCommandPoints(uid, -points);
        if (getTroops(uid ^ 1).isEmpty()) {
            // Game is over.
            setGameStatus(CoreConstants.GameResult.WIN_GAME);
        }
    }
    public boolean spendCommandPoints(int uid, int points) {
        if (getCommandPoints(uid) >= points) {
            commandPoints[uid] -= points;
            return true;
        } else {
            return false;
        }
    }
    public boolean moveTroop(@NotNull Troop troop, Vector2D to) {
        if (locationToTroopMap.get(troop.getLocation()) == null)
            return false;
        locationToTroopMap.remove(troop.getLocation());
        locationToTroopMap.put(to, troop.getComponentID());
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
    public void addTroop(@NotNull Troop troop, Vector2D position) {
        troop.setLocation(position);
        locationToTroopMap.put(position, troop.getComponentID());
    }

    /*======= CHECKING / COMPUTING FUNCTIONS =======*/
    /**
     * Check if an action is allowed to be performed on the target. This verifies the different conditions for different actions.
     * @param action Which action to check
     * @param requireHighlight whether or not to require the target cell to be taken from the highlight, or the action
     * @return can the action be performed or not
     */
    public boolean canPerformAction(AbstractAction action, boolean requireHighlight) {
        if (action instanceof EndTurn) return gamePhase != CQGamePhase.SelectionPhase;
        CQAction cqAction = (CQAction) action;
        if (requireHighlight && !cqAction.compareHighlight(highlight, (Command) getComponentById(cmdHighlight))) return false;
        return ((CQAction) action).canExecute(this);
    }
    public int getDistance(Cell from, Cell to) {
        return aStar(from, to);
    }
    public int getDistance(Vector2D from, Vector2D to) {
        return getDistance(getCell(from), getCell(to));
    }
    /**
     * Calculate the distance between two points, given some game state.
     * @param from The cell from which to calculate
     * @param to The cell to which to calculate
     * @return Distance between the two cells; returns 9999 (~inf) when unreachable
     */
    public int aStar(@NotNull Cell from, Cell to) {
        if (from.equals(to)) return 0; // only cell with a troop we can walk on is to our own cell.
        if (!to.isWalkable(this))
            return 9999; // if target is not walkable, it's not reachable; avoid calculations
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(node -> node.f));
        Map<Cell, Integer> gScores = new HashMap<>();
        Set<Cell> closedSet = new HashSet<>();

        Node startNode = new Node(from, 0, from.getChebyshev(to));
        openSet.add(startNode);
        gScores.put(from, 0);
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            Cell currentCell = current.cell;
            if (currentCell.equals(to)) // If we reach the destination, return the distance
                return current.g;
            closedSet.add(currentCell);

            for (Cell neighbor : currentCell.getNeighbors(cells)) {
                if (closedSet.contains(neighbor) || !neighbor.isWalkable(this)) {
                    continue;
                }
                int heuristicG = current.g + 1; // Cost from start to neighbor
                if (heuristicG < gScores.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    gScores.put(neighbor, heuristicG);
                    int fScore = heuristicG + neighbor.getChebyshev(to);
                    openSet.add(new Node(neighbor, heuristicG, fScore));
                }
            }
        }
        // If no path was found
        return 9999;
    }
    private static class Node {
        Cell cell;
        int g;
        int f;
        Node(Cell cell, int g, int f) {
            this.cell = cell;
            this.g = g;
            this.f = f;
        }
    }
    /**
     * Flood fill to generate distances to the whole board. (currently only used in GUI)
     * Used in case where all distances need to be known, due to being cheaper than performing A* on all cells.
     * @param source Source cell from which to calculate distances
     * @param maxDistance maximum distance to perform floodFill on, or 0 if no maximum is set.
     * @return 2d integer array containing distances to the target square, or 9999 if unreachable.
     */
    public int[][] floodFill (@NotNull Cell source, int maxDistance) {
        int w = cells.length, h = cells[0].length;
        List<Cell> openSet = source.getNeighbors(cells); // initial set of neighbors
        Set<Cell> closedSet = new HashSet<>();
        int[][] board = new int[w][h];
        for (int[] row : board) // fill all with 'unreachable' initially
            Arrays.fill(row, 9999);
        int distance = 0;
        while (!openSet.isEmpty() && (maxDistance == 0 || distance <= maxDistance)) {
            distance++; // first iteration is distance 1, etc
            for (Cell c : openSet) {
                // First go through all cells currently listed and add their distance
                if (!c.isWalkable(this)) // can't visit; set value to infty
                    board[c.position.getX()][c.position.getY()] = 9999;
                else // can visit; set
                    board[c.position.getX()][c.position.getY()] = distance;
                closedSet.add(c);
            }
            Set<Cell> newSet = new HashSet<>();
            for (Cell c : openSet) {
                // After going through all neighbors once, replace them by their neighbor set
                if (c.isWalkable(this)) // ignore if this cell wasn't reachable anyway
                    newSet.addAll(c.getNeighbors(cells));
            }
            openSet.clear(); // all items have been checked for their neighbors, so make place
            openSet.addAll(newSet); // copy new set of all neighbours to open set
            openSet.removeAll(closedSet); // remove cells that have been visited before
        }
        board[source.position.getX()][source.position.getY()] = 0;
        return board;
    }

    /*======= BOILERPLATE METHODS =======*/
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
        copy.cells = cells; // Cells only have final properties so can be copied like this.
        copy.troops = new HashSet<Troop>();
        for (Troop t : troops) {
            copy.troops.add(t.copy());
        }
        copy.locationToTroopMap = (HashMap<Vector2D, Integer>) locationToTroopMap.clone();
        copy.selectedTroop = selectedTroop;
        copy.highlight = highlight;
        copy.cmdHighlight = cmdHighlight;
        copy.gridBoard = gridBoard.copy();
        copy.chosenCommands = new PartialObservableDeck[2];
        copy.chosenCommands[0] = chosenCommands[0].copy();
        copy.chosenCommands[1] = chosenCommands[1].copy();
        copy.commandPoints = commandPoints.clone();
        return copy;
    }

    /**
     * Heuristic used: Compute a metric that compares living troops to dead troops, and scales living troops to their hp
     * Then take the difference between these two metrics, and add a metric for command points and command cooldowns.
     * A player with a lot of command points and available commands, and lots of troops living, with an enemy with few troops living,
     * will receive a score of 1. The opposite will result in a negative score. Since the command point metric can't be negative,
     * the score will be divided by 3 for positive scores, or by 2 for negative scores;
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing, between -1 and 1
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        // compare living troop scores for both players;
        double runningScore = getTroopHealthMetric(playerId) - getTroopHealthMetric(playerId ^ 1);
        // Commands are useful when not on cooldown; punish having commands on cooldown, but by less than the reward of a dead troop.
        List<Command> allCommands = getCommands(playerId).getVisibleComponents(playerId);
        int totalCooldown = 0;
        int currentCooldown = 0;
        for (Command cmd : allCommands) {
            currentCooldown += cmd.getCooldown();
            totalCooldown += cmd.getCommandType().cooldown;
        }
        double cooldownFraction = currentCooldown / (double) totalCooldown;
        // Low points is bad, some points is good, more points is slightly better
        int points = getCommandPoints(playerId);
        double expPoints = 1 - Math.exp(-points / 25.0);
        // to the final score, add points, subtract cooldowns
        runningScore += (expPoints - cooldownFraction);
        return runningScore > 0 ? runningScore / 3 : runningScore / 2;
    }

    @Override
    public double getGameScore(int playerId) {
        return _getHeuristicScore(playerId) * 1000;
//        return getTotalTroopCost(getTroops(playerId));
    }
    @Override
    protected boolean _equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CQGameState)) return false;
        CQGameState cqgs = (CQGameState) o;
        if (cqgs.getGamePhase() != getGamePhase()) return false;
        // Compare simple integer values:
        if (cqgs.commandPoints[0] != commandPoints[0] ||
            cqgs.commandPoints[1] != commandPoints[1])
            return false;
        // Compare hashmaps and other subobjects:
        if (!(
                cqgs.highlight.equals(highlight) &&
                Arrays.deepEquals(cqgs.cells, cells) &&
                cqgs.troops.equals(troops) &&
                cqgs.locationToTroopMap.equals(locationToTroopMap) &&
                cqgs.cmdHighlight == cmdHighlight &&
                cqgs.gridBoard.equals(gridBoard) &&
                Arrays.equals(cqgs.chosenCommands, chosenCommands)
        ))
            return false;
        return true;
    }
    @Override
    public int hashCode() {
        return Objects.hash(
                getGamePhase(), Arrays.deepHashCode(cells), troops, locationToTroopMap, selectedTroop, getCurrentPlayer(),
                gridBoard, Arrays.hashCode(chosenCommands), Arrays.hashCode(commandPoints)
        );
    }
}
