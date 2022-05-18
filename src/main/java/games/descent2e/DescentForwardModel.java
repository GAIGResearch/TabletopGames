package games.descent2e;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.*;
import core.properties.*;
import games.GameType;
import games.descent2e.actions.Move;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import games.descent2e.concepts.Quest;
import utilities.Pair;
import utilities.Vector2D;

import java.awt.*;
import java.util.*;
import java.util.List;

import games.descent2e.DescentTypes.*;

import static core.CoreConstants.*;
import static games.descent2e.DescentConstants.*;
import static utilities.Utils.getNeighbourhood;

public class DescentForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DescentGameState dgs = (DescentGameState) firstState;
        DescentGameData _data = dgs.getData();

        // TODO: epic play options (pg 19)

        // Get campaign from game parameters, load all the necessary information
        Campaign campaign = ((DescentParameters)dgs.getGameParameters()).campaign;
        campaign.load(_data);
        // TODO: Separate shop items (+shuffle), monster and lieutenent cards into 2 acts.

        Quest firstQuest = campaign.getQuests()[0];
        String firstBoard = firstQuest.getBoards().get(0);

        // Set up first board of first quest
        setupBoard(dgs, _data, firstBoard);

        // Overlord setup
        dgs.overlordPlayer = 0;  // First player is always the overlord
        // Overlord will also have a figure, but not on the board (to store xp and skill info)
        dgs.overlord = new Figure("Overlord");
        dgs.overlord.setTokenType("Overlord");
        // TODO: Shuffle overlord deck and give overlord nPlayers cards.

        // TODO: is this quest phase or campaign phase?

        // TODO: Let players choose these, for now randomly assigned
        // TODO: 2 player games, with 2 heroes for one, and the other the overlord.
        // 5. Player setup phase interrupts, after which setup continues:
        // Player chooses hero & class

        ArrayList<Vector2D> playerStartingLocations = firstQuest.getStartingLocations().get(firstBoard);

        ArrayList<Integer> archetypes = new ArrayList<>();
        for (int i = 0; i < DescentConstants.archetypes.length; i++) {
            archetypes.add(i);
        }
        Random rnd = new Random(firstState.getGameParameters().getRandomSeed());
        dgs.heroes = new ArrayList<>();
        for (int i = 1; i < dgs.getNPlayers(); i++) {
            // Choose random archetype from those remaining
            int choice = archetypes.get(rnd.nextInt(archetypes.size()));
            archetypes.remove(Integer.valueOf(choice));
            String archetype = DescentConstants.archetypes[choice];

            // Choose random hero from that archetype
            List<Hero> heroes = _data.findHeroes(archetype);
            Hero figure = heroes.get(rnd.nextInt(heroes.size()));

            // Choose random class from that archetype
            choice = rnd.nextInt(DescentConstants.archetypeClassMap.get(archetype).length);
            String heroClass = DescentConstants.archetypeClassMap.get(archetype)[choice];

            // Inform figure of chosen class
            figure.setProperty(new PropertyString("class", heroClass));

            // Assign starting skills and equipment from chosen class
            Deck<Card> classDeck = _data.findDeck(heroClass);
            for (Card c: classDeck.getComponents()) {
                if (((PropertyInt)c.getProperty(xpHash)).value <= figure.getXP()) {
                    figure.equip(c);
                }
            }

            // Place player in random starting location
            choice = rnd.nextInt(playerStartingLocations.size());
            Vector2D location = playerStartingLocations.get(choice);
            figure.setLocation(location);
            PropertyInt prop = new PropertyInt("players", figure.getComponentID());
            dgs.masterBoard.getElement(location.getX(), location.getY()).setProperty(prop);
            playerStartingLocations.remove(choice);

            // Inform game of this player's token
            dgs.heroes.add(figure);
        }

        // Overlord chooses monster groups // TODO, for now randomly selected
        // Create and place monsters
        createMonsters(dgs, firstQuest, _data, rnd);

        // Set up dice?

        // Shuffle search cards deck

        // Ready to start playing!
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
        if (checkEndOfGame()) return;

        int currentPlayer = currentState.getCurrentPlayer();
        int nActionsPerPlayer = ((DescentParameters)currentState.getGameParameters()).nActionsPerPlayer;
        if (currentPlayer == 0 && ((DescentGameState)currentState).overlord.getNActionsExecuted() == nActionsPerPlayer
            || currentPlayer != 0 &&
                ((DescentGameState)currentState).getHeroes().get(currentPlayer-1).getNActionsExecuted() == nActionsPerPlayer) {
            currentState.getTurnOrder().endPlayerTurn(currentState);
        }

        /*
        Hero turn:
        1. Start of turn:
            - start of turn abilities
            - refresh cards
            - test attributes for conditions
        2. Take 2 actions

        Overlord turn: TODO: current turn order alternates 1 monster group, 1 hero player etc.
        1. Start of turn:
            - Start of turn abilities
            - Draw 1 Overlord card
            - Refresh cards
        2. Activate monsters:
            - Choose monster group
            - On-activation group effects
            - Choose unactivated monster in group
            - On-activation effects
            - Perform 2 actions with the monster
            - End of monster activation effects
            - End of monster group activation effects
            - Repeat steps for each remaining monster group
        3. End of turn abilities
         */

        // Any figure that ends its turn in a lava space is immediately defeated.
        // Heroes that are defeated in this way place their hero token in the nearest empty space
        // (from where they were defeated) that does not contain lava. A large monster is immediately defeated only
        // if all spaces it occupies are lava spaces.

        // TODO

        // Quest finished -> Campaign phase
        // Set up campaign phase
        // receive gold from search cards and return cards to the deck.
        // recover all damage and fatigue, discard conditions and effects
        // receive quest reward (1XP per hero + bonus from quest)
        // shopping (if right after interlude, can buy any act 1 cards, then remove these from game)
        // spend XP points for skills
        // choose next quest (winner chooses)
        // setup next quest
        // Campaign phase -> quest phase

        // choosing interlude: the heroes pick if they won >= 2 act 1 quests, overlord picks if they won >=2 quests

        // TODO: in 2-hero games, free regular attack action each turn or recover 2 damage.
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DescentGameState dgs = (DescentGameState)gameState;
        int currentPlayer = gameState.getCurrentPlayer();
        int nActions = ((DescentParameters) dgs.getGameParameters()).nActionsPerPlayer;

        // Find current monster group + monster playing
        int monsterGroupIdx = ((DescentTurnOrder) dgs.getTurnOrder()).monsterGroupActingNext;
        ArrayList<Monster> monsterGroup = dgs.getMonsters().get(monsterGroupIdx);
        int nextMonster = ((DescentTurnOrder) dgs.getTurnOrder()).monsterActingNext;

        // Find currently acting figure (hero or monster)
        Figure actingFigure;
        if (currentPlayer != 0) {
            // If hero player, get corresponding hero
            actingFigure = dgs.getHeroes().get(currentPlayer - 1);
        } else {
            // Otherwise, monster is playing
            actingFigure = monsterGroup.get(nextMonster);
        }

        // Init action list
        ArrayList<AbstractAction> actions = new ArrayList<>();

        if (!(dgs.getGamePhase() == DescentGameState.DescentPhase.ForceMove)) {
            // Can do actions other than move

            // Do nothing // TODO: remove this option, replace with EndAction action.
            actions.add(new DoNothing());

            // Can we do a move action? Can't if already done max actions & not currently executing a move, or immobilized
            boolean canMove = !actingFigure.hasCondition(DescentCondition.Immobilize) &&
                    (actingFigure.getNActionsExecuted() != nActions || actingFigure.getMovePoints() > 0);
            if (canMove) {
                // Is this a new move action? It is if player can move, but all move points spent in first move action
                if (actingFigure.getMovePoints() == 0) {
                    // TODO: This is a second move action, reset move points for calculation + if agent actually chooses it
                }
                actions.addAll(moveActions(dgs, actingFigure));
            }

            // TODO other actions
            // - Attack with 1 equipped weapon [ + monsters, the rest are just heroes]
            // - Rest
            // - Perform "action" ability/skill  // TODO: add these to list of figure's actions, have some mapping to functions
            // - Open/close a door
            // - Revive hero
            // - Search
            // - Stand up
            // - Special (specified by quest) TODO: add these to list of figure's actions

        } else {
            actions.addAll(moveActions(dgs, actingFigure));
        }

        // TODO: stamina move, not an "action", but same rules for move apply
        // TODO: exhaust a card for an action/modifier/effect "free" action

        if (actingFigure.getNActionsExecuted() == nActions || actions.size() == 1) {
            if (currentPlayer == 0) {
                // This monster is finished, move to next monster
                // TODO: barghest minions never move, find out why
                ((DescentTurnOrder) dgs.getTurnOrder()).nextMonster(monsterGroup.size());
                if (nextMonster == monsterGroup.size() - 1) {
                    // Overlord is finished with this monster group
                    dgs.overlord.setNActionsExecuted(nActions);
                }
            } else {
                actingFigure.setNActionsExecuted(actingFigure.getNActionsExecuted()+1);
            }
        }

        return actions;
    }

    private List<AbstractAction> moveActions(DescentGameState dgs, Figure f) {
        List<AbstractAction> actions = new ArrayList<>();

        Vector2D currentLocation = f.getLocation();
        BoardNode currentTile = dgs.masterBoard.getElement(currentLocation.getX(), currentLocation.getY());

        // Check if figure can still move
        PropertyInt moveSpeed = (PropertyInt)f.getProperty(movementHash);
        if (currentTile.getComponentName().equals("pit") || f.getMovePoints() > 0) {

            // Find valid neighbours in master graph, can move there
            for (BoardNode neighbour : currentTile.getNeighbours()) {
                Vector2D loc = ((PropertyVector2D) neighbour.getProperty(coordinateHash)).values;
                // TODO: size of figure moving, take into account large monster "expansion" rule, location saved on figure is always top-left corner

                // Find terrain type
                BoardNode tile = dgs.getMasterBoard().getElement(loc.getX(), loc.getY());
                if ((currentTile.getComponentName().equals("pit") && !tile.getComponentName().equals("pit") // Moving from pit
                        || !tile.getComponentName().equals("water")  // Normal move
                        || f.getMovePoints() > ((DescentParameters)dgs.getGameParameters()).waterMoveCost) // Difficult terrain
                        && ((PropertyInt)tile.getProperty(playersHash)).value == -1) {  // Empty space?
                    // TODO: allow move in non-empty space if figure has move points left that allow it to finish the move action afterwards in an empty space
                    // TODO: if moving to non-empty space, change game phase to ForceMove; otherwise, change game phase to main phase (if in force move).
                    actions.add(new Move(loc.copy()));
                }
            }
        }
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new DescentForwardModel();
    }

    private boolean checkEndOfGame() {
        // TODO
        return false;
    }

    private void setupBoard(DescentGameState dgs, DescentGameData _data, String bConfig) {

        // 1. Read the graph board configuration for the master grid board
        GraphBoard config = _data.findGraphBoard(bConfig);

        // 2. Read all necessary tiles, which are all grid boards. Keep in a list.
        dgs.tiles = new HashMap<>();
        dgs.gridReferences = new HashMap<>();
        for (BoardNode bn : config.getBoardNodes()) {
            String name = bn.getComponentName();
            String tileName = name;
            if (name.contains("-")) {  // There may be multiples of one tile in the board, which follow format "tilename-#"
                tileName = tileName.split("-")[0];
            }
            GridBoard tile = _data.findGridBoard(tileName);
            if (tile != null) {
                dgs.tiles.put(bn.getComponentID(), tile);
                dgs.gridReferences.put(name, new HashSet<>());
            }
        }

        // 3. Put together the master grid board
        // Find maximum board width and height, if all were put together side by side
        int width = 0;
        int height = 0;
        for (BoardNode bn : config.getBoardNodes()) {
            // Find width of this tile, according to orientation
            GridBoard tile = dgs.tiles.get(bn.getComponentID());
            if (tile != null) {
                int orientation = ((PropertyInt) bn.getProperty(orientationHash)).value;
                if (orientation % 2 == 0) {
                    width += tile.getWidth();
                    height += tile.getHeight();
                } else {
                    width += tile.getHeight();
                    height += tile.getWidth();
                }
            }
        }

        // First tile will be in the center, board could expand in more directions
        width *= 2;
        height *= 2;

        // Create big board
        BoardNode[][] board = new BoardNode[height][width];
        dgs.tileReferences = new int[height][width];
        HashSet<BoardNode> drawn = new HashSet<>();
        ArrayList<Pair<Vector2D, Vector2D>> neighbours = new ArrayList<>();  // Holds neighbouring cells information

        BoardNode bn0 = null;
        for (BoardNode b : config.getBoardNodes()) {
            if (b != null) {
                GridBoard tile = dgs.tiles.get(b.getComponentID());
                if (tile != null) {
                    bn0 = b;
                    break;
                }
            }
        }
        if (bn0 != null) {
            GridBoard tile = dgs.tiles.get(bn0.getComponentID());
            int orientation = ((PropertyInt) bn0.getProperty(orientationHash)).value;
            BoardNode[][] rotated = tile.rotate(orientation);
            int startX = width / 2 - rotated[0].length / 2;
            int startY = height / 2 - rotated.length / 2;
            Rectangle bounds = new Rectangle(startX, startY, rotated[0].length, rotated.length);
            addTilesToBoard(bn0, startX, startY, board, null, dgs.tiles, dgs.tileReferences, dgs.gridReferences, drawn, neighbours, bounds);

            // Trim the resulting board and tile references to remove excess border of nulls according to 'bounds' rectangle
            bounds.x -= 1;
            bounds.y -= 1;
            bounds.width += 2;
            bounds.height += 2;
            BoardNode[][] trimBoard = new BoardNode[bounds.height][bounds.width];
            int[][] trimTileRef = new int[bounds.height][bounds.width];
            for (int i = 0; i < bounds.height; i++) {
                if (bounds.width >= 0) System.arraycopy(board[i + bounds.y], bounds.x, trimBoard[i], 0, bounds.width);
                if (bounds.width >= 0)
                    System.arraycopy(dgs.tileReferences[i + bounds.y], bounds.x, trimTileRef[i], 0, bounds.width);
            }
            dgs.tileReferences = trimTileRef;
            // Also trim neighbour records
            for (Pair<Vector2D, Vector2D> p : neighbours) {
                p.a.subtract(bounds.x, bounds.y);
                p.b.subtract(bounds.x, bounds.y);
            }
            // And grid references
            for (Map.Entry<String, HashSet<Vector2D>> e: dgs.gridReferences.entrySet()) {
                for (Vector2D v: e.getValue()) {
                    v.subtract(bounds.x, bounds.y);
                }
            }

            // This is the master board!
            dgs.masterBoard = new GridBoard(trimBoard);
            dgs.masterBoard.setNeighbours(neighbours);
        } else {
            System.out.println("Tiles for the map not found");
        }
    }

    /**
     * Recursively adds tiles to the board, iterating through all neighbours and updating references from grid
     * to tiles, list of valid neighbours for movement graph according to Descent movement rules, and bounds for
     * the resulting grid of the master board (with all tiles put together).
     * @param bn - board node representing tile to add to board
     * @param x - top-left x-coordinate for this tile's location in the master board
     * @param y - top-left y-coordinate for this tile's location in the master board
     * @param board - the master grid board representation
     * @param tileGrid - grid representation of tile to add to board (possibly a trimmed version from its corresponding
     *                 object in the "tiles" map to fit together with existing board)
     * @param tiles - mapping from board node component ID, to GridBoard object representing the tile
     * @param tileReferences - references from each cell in the grid to the component ID of the GridBoard representing
     *                       the tile at that location
     * @param drawn - a list of board nodes already drawn, to avoid drawing twice during recursive calls.
     * @param neighbours - a list of pairs of neighbouring cells
     * @param bounds - bounds of contents of the master grid board
     */
    // TODO: set property "players" (int) to -1 for all board nodes
    // TODO: set neighbours directly in the board nodes
    private void addTilesToBoard(BoardNode bn, int x, int y, BoardNode[][] board,
                                 BoardNode[][] tileGrid,
                                 HashMap<Integer, GridBoard> tiles,
                                 int[][] tileReferences,  HashMap<String, HashSet<Vector2D>> gridReferences,
                                 HashSet<BoardNode> drawn,
                                 ArrayList<Pair<Vector2D, Vector2D>> neighbours,
                                 Rectangle bounds) {
        if (!drawn.contains(bn)) {
            // Draw this tile in the big board at x, y location
            GridBoard tile = tiles.get(bn.getComponentID());
            if (tileGrid == null) {
                tileGrid = tile.rotate(((PropertyInt) bn.getProperty(orientationHash)).value);
            }
            int height = tileGrid.length;
            int width = tileGrid[0].length;

            // Connect the new tile with current board. Tile overlapping here has 8-way connectivity,
            // as do its neighbours on new tile
            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; j++) {
                    if (board[i][j] != null && board[i][j].getComponentName().equalsIgnoreCase("open")) {
                        Vector2D point = new Vector2D(j, i);

                        // Add connections from this point to points on the master board
                        List<Vector2D> boardNs = getNeighbourhood(j, i, board[0].length, board.length, true);
                        if (TerrainType.isWalkable(tileGrid[point.getY() - y][point.getX() - x].getComponentName())) {
                            // Connect each cell that can connect to the master board with all possible connections
                            for (Vector2D n2 : boardNs) {
                                if (TerrainType.isWalkable(board[n2.getY()][n2.getX()].getComponentName()) &&
                                        !n2.equals(point)) {
                                    if (Math.abs(point.getY() - n2.getY()) <= 1 && Math.abs(point.getX() - n2.getX()) <= 1) {
                                        neighbours.add(new Pair<>(point.copy(), n2.copy()));
                                    }
                                }
                            }
                        }

                        // Add connections from connecting point on the master board to this tile, the orthogonal neighbour that's an inside piece
                        List<Vector2D> possible = getNeighbourhood(j, i, board[0].length, board.length, false);
                        Vector2D other = null;
                        for (Vector2D p: possible) {
                            if (TerrainType.isInsideTile(board[p.getY()][p.getX()].getComponentName())) {
                                other = p;
                                break;
                            }
                        }

                        if (other != null) {
                            List<Vector2D> tileNs = getNeighbourhood(j - x, i - y, width, height, true);
                            // Connect each cell that can connect to the master board with all possible connections
                            for (Vector2D n2 : tileNs) {
                                Vector2D pointInBoard = new Vector2D(n2.getX() + x, n2.getY() + y);
                                if (TerrainType.isWalkable(tileGrid[n2.getY()][n2.getX()].getComponentName()) &&
                                        !pointInBoard.equals(other)) {
                                    if (Math.abs(other.getY() - pointInBoard.getY()) <= 1 && Math.abs(other.getX() - pointInBoard.getX()) <= 1) {
                                        neighbours.add(new Pair<>(other.copy(), pointInBoard.copy()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add connections between all tiles just placed, unless blocked (no blocked tiles are connected)
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (TerrainType.isWalkable(tileGrid[i][j].getComponentName())) {
                        List<Vector2D> ns = getNeighbourhood(j, i, width, height, true);
                        for (Vector2D nn : ns) {
                            if (TerrainType.isWalkable(tileGrid[nn.getY()][nn.getX()].getComponentName()) &&
                                    !(nn.getX() == j && nn.getY() == i)) {
                                neighbours.add(new Pair<>(new Vector2D(j + x, i + y), new Vector2D(nn.getX() + x, nn.getY() + y)));
                            }
                        }
                    }
                }
            }

            // Add cells from new tile to the master board
            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; j++) {
                    if (board[i][j] != null && (tileGrid[i-y][j-x] == null || tileGrid[i-y][j-x].equals("edge")
                            || tileGrid[i-y][j-x].equals("null") || tileGrid[i-y][j-x].equals("open"))) continue;
                    // Avoid removing already set tiles
                    board[i][j] = tileGrid[i-y][j-x];

                    if (board[i][j].equals("edge") || board[i][j].equals("null") || board[i][j].equals("open")) continue;
                    // Don't keep references for edge tiles
                    tileReferences[i][j] = tile.getComponentID();
                    for (String s: gridReferences.keySet()) {
                        gridReferences.get(s).remove(new Vector2D(j, i));
                    }
                    gridReferences.get(tile.getComponentName()).add(new Vector2D(j, i));
                }
            }

            // This tile was drawn
            drawn.add(bn);

            // Draw neighbours
            for (BoardNode neighbour: bn.getNeighbours()) {

                // Find location to start drawing neighbour
                Pair<String, Vector2D> connectionToNeighbour = findConnection(bn, neighbour, findOpenings(tileGrid));

                if (connectionToNeighbour != null) {
                    connectionToNeighbour.b.add(x, y);
                    // Find orientation and opening connection from neighbour, generate top-left corner of neighbour from that
                    GridBoard tileN = tiles.get(neighbour.getComponentID());
                    if (tileN != null) {
                        BoardNode[][] tileGridN = tileN.rotate(((PropertyInt) neighbour.getProperty(orientationHash)).value);

                        // Find location to start drawing neighbour
                        Pair<String, Vector2D> conn2 = findConnection(neighbour, bn, findOpenings(tileGridN));

                        int w = tileGridN[0].length;
                        int h = tileGridN.length;

                        if (conn2 != null) {
                            String side = conn2.a;
                            Vector2D connectionFromNeighbour = conn2.b;
                            if (side.equalsIgnoreCase("W")) {
                                // Remove first column
                                BoardNode[][] tileGridNTrim = new BoardNode[h][w - 1];
                                for (int i = 0; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 1, tileGridNTrim[i], 0, w - 1);
                                }
                                tileGridN = tileGridNTrim;
                            } else if (side.equalsIgnoreCase("E")) {
                                connectionFromNeighbour.subtract(1, 0);
                                // Remove last column
                                BoardNode[][] tileGridNTrim = new BoardNode[h][w - 1];
                                for (int i = 0; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w - 1);
                                }
                                tileGridN = tileGridNTrim;
                            } else if (side.equalsIgnoreCase("N")) {
                                // Remove first row
                                BoardNode[][] tileGridNTrim = new BoardNode[h - 1][w];
                                for (int i = 1; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i - 1], 0, w);
                                }
                                tileGridN = tileGridNTrim;
                            } else {
                                connectionFromNeighbour.subtract(0, 1);
                                // Remove last row
                                BoardNode[][] tileGridNTrim = new BoardNode[h - 1][w];
                                for (int i = 0; i < h - 1; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w);
                                }
                                tileGridN = tileGridNTrim;
                            }
                            Vector2D topLeftCorner = new Vector2D(connectionToNeighbour.b.getX() - connectionFromNeighbour.getX(),
                                    connectionToNeighbour.b.getY() - connectionFromNeighbour.getY());

                            // Update area bounds
                            if (topLeftCorner.getX() < bounds.x) bounds.x = topLeftCorner.getX();
                            if (topLeftCorner.getY() < bounds.y) bounds.y = topLeftCorner.getY();
                            int deltaMaxX = (int) (topLeftCorner.getX() + tileGridN[0].length - bounds.getMaxX());
                            if (deltaMaxX > 0) bounds.width += deltaMaxX;
                            int deltaMaxY = (int) (topLeftCorner.getY() + tileGridN.length - bounds.getMaxY());
                            if (deltaMaxY > 0) bounds.height += deltaMaxY;

                            // Draw neighbour recursively
                            addTilesToBoard(neighbour, topLeftCorner.getX(), topLeftCorner.getY(), board, tileGridN,
                                    tiles, tileReferences, gridReferences, drawn, neighbours, bounds);
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds a connection between two boardnodes representing tiles in the game (i.e. where the 2 tiles should be
     * connecting according to board configuration)
     * @param from - origin board node to find connection from
     * @param to - board node to find connection to
     * @param openings - list of openings for the origin board node
     * @return - a pair of side, and location (in tile space) of openings that would connect to the given tile as required
     */
    private Pair<String, Vector2D> findConnection(BoardNode from, BoardNode to, HashMap<String, ArrayList<Vector2D>> openings) {
        String[] neighbours = ((PropertyStringArray) from.getProperty(neighbourHash)).getValues();
        String[] connections = ((PropertyStringArray) from.getProperty(connectionHash)).getValues();

        for (int i = 0; i < neighbours.length; i++) {
            if (neighbours[i].equalsIgnoreCase(to.getComponentName())) {
                String conn = connections[i];

                String side = conn.split("-")[0];
                int countFromTop = Integer.parseInt(conn.split("-")[1]);
                if (openings.containsKey(side)) {
                    if (countFromTop >= 0 && countFromTop < openings.get(side).size()) {
                        return new Pair<>(side, openings.get(side).get(countFromTop));
                    }
                }
                break;
            }
        }
        return null;
    }

    /**
     * Finds coordinates (in tile space) for where openings on all sides (top-left locations).
     * // TODO: assumes all openings 2-tile wide + no openings are next to each other.
     * @param tileGrid - grid to look for openings in
     * @return - Mapping from side (N, S, W, E) to a list of openings on that particular side.
     */
    private HashMap<String, ArrayList<Vector2D>> findOpenings(BoardNode[][] tileGrid) {
        int height = tileGrid.length;
        int width = tileGrid[0].length;

        HashMap<String, ArrayList<Vector2D>> openings = new HashMap<>();
        // TOP, check each column, stop at the first encounter in each column.
        for (int j = 0; j < width; j++) {
            for (int i = 0; i < height; i++) {
                if (tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile above
                    if (i == 0 || tileGrid[i-1][j].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i-1][j].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" to the left (already included, all openings 2-wide)
                        // But another "open" to the right
                        if ((j == 0 || !tileGrid[i][j-1].getComponentName().equalsIgnoreCase("open")) &&
                                (j < width-1 && tileGrid[i][j+1].getComponentName().equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("N")) {
                                openings.put("N", new ArrayList<>());
                            }
                            openings.get("N").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        // BOTTOM, check each column, stop at the first encounter in each column (read from bottom to top).
        for (int j = 0; j < width; j++) {
            for (int i = height-1; i >= 0; i--) {
                if (tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile below
                    if (i == height-1 || tileGrid[i+1][j].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i+1][j].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" to the left (already included, all openings 2-wide)
                        // But another "open" to the right
                        if ((j == 0 || !tileGrid[i][j-1].getComponentName().equalsIgnoreCase("open")) &&
                                (j < width-1 && tileGrid[i][j+1].getComponentName().equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("S")) {
                                openings.put("S", new ArrayList<>());
                            }
                            openings.get("S").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        // LEFT, check each row, stop at the first encounter in each row.
        for (int i = 0; i < height; i++){
            for (int j = 0; j < width; j++) {
                if (tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile to the left
                    if (j == 0 || tileGrid[i][j-1].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i][j-1].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" above (already included, all openings 2-wide)
                        // But another "open" below
                        if ((i == 0 || !tileGrid[i-1][j].getComponentName().equalsIgnoreCase("open")) &&
                                (i < height-1 && tileGrid[i+1][j].getComponentName().equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("W")) {
                                openings.put("W", new ArrayList<>());
                            }
                            openings.get("W").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        // RIGHT, check each row, stop at the first encounter in each row (read from right to left).
        for (int i = 0; i < height; i++){
            for (int j = width-1; j >= 0; j--) {
                if (tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile to the right
                    if (j == width-1 || tileGrid[i][j+1].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i][j+1].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" above (already included, all openings 2-wide)
                        // But another "open" below
                        if ((i == 0 || !tileGrid[i-1][j].getComponentName().equalsIgnoreCase("open")) &&
                                (i < height-1 && tileGrid[i+1][j].getComponentName().equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("E")) {
                                openings.put("E", new ArrayList<>());
                            }
                            openings.get("E").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        return openings;
    }

    /**
     * Creates all the monsters according to given quest information and places them randomly in the map on requested tile.
     * @param dgs - game state
     * @param quest - quest defining monsters
     * @param _data - all game data
     * @param rnd - random generator
     */
    private void createMonsters(DescentGameState dgs, Quest quest, DescentGameData _data, Random rnd) {
        dgs.monsters = new ArrayList<>();
        ArrayList<String[]> monsters = quest.getMonsters();
        for (String[] mDef: monsters) {
            ArrayList<Monster> monsterGroup = new ArrayList<>();

            String nameDef = mDef[0];
            String name = nameDef.split(":")[0];
            String tile = mDef[1];
            HashSet<Vector2D> tileCoords = dgs.gridReferences.get(tile);

            // Check property modifiers
            int hpModifierMaster = 0;
            int hpModifierMinion = 0;
            if (mDef.length > 2) {
                String mod = mDef[2];
                String[] modifiers = mod.split(";");
                for (String modifier: modifiers) {
                    String who = modifier.split(":")[0];
                    String property = modifier.split(":")[1];
                    String sign = modifier.split(":")[2];
                    int amount = Integer.parseInt(modifier.split(":")[3]);
                    if (sign.equals("-")) amount = -amount;

                    if (property.equals("HP")) {
                        // HP modifier
                        if (who.equals("all")) {
                            hpModifierMaster += amount;
                            hpModifierMinion += amount;
                        } else if (who.equals("master")) {
                            hpModifierMaster += amount;
                        } else {
                            hpModifierMinion += amount;
                        }
                    } else {
                        int a = 0;
                        // TODO: other properties modified
                        // TODO: this could be adding/removing abilities too
                    }
                }
            }

            int act = quest.getAct();
            HashMap<String, Token> monsterDef = _data.findMonster(name);
            Token superDef = monsterDef.get("super");
            int[] monsterSetup = ((PropertyIntArray)superDef.getProperty(setupHash)).getValues();

            // Always 1 master
            Monster master = new Monster(name + " master", monsterDef.get(act + "-master").getProperties());
            placeMonster(dgs, master, new ArrayList<>(tileCoords), rnd, hpModifierMaster, superDef);
            monsterGroup.add(master);

            // How many minions?
            int nMinions;
            if (nameDef.contains("group")) {
                if (nameDef.contains("ignore")) {
                    // Ignore group limits, max number
                    nMinions = monsterSetup[monsterSetup.length-1];
                } else {
                    // Respect group limits
                    nMinions = monsterSetup[dgs.getNPlayers()- GameType.Descent2e.getMinPlayers()];
                }
            } else {
                // Format name:#minions
                nMinions = Integer.parseInt(nameDef.split(":")[1]);
            }

            // Place minions
            for (int i = 0; i < nMinions; i++) {
                Monster minion = new Monster(name + " minion " + i, monsterDef.get(act + "-minion").getProperties());
                placeMonster(dgs, minion, new ArrayList<>(tileCoords), rnd, hpModifierMinion, superDef);
                monsterGroup.add(minion);
            }

            dgs.monsters.add(monsterGroup);
        }
    }

    /**
     * Places a monster in the board, randomly choosing one valid tile from given list.
     * @param dgs - current game state
     * @param monster - monster to place
     * @param tileCoords - coordinate options for the monster
     * @param rnd - random generator
     */
    private void placeMonster(DescentGameState dgs, Monster monster, ArrayList<Vector2D> tileCoords, Random rnd,
                              int hpModifier, Token superDef) {
        // Finish setup of monster
        monster.setProperties(superDef.getProperties());
        if (hpModifier > 0) {
            int oldMasterHP = ((PropertyInt)monster.getProperty(healthHash)).value;
            monster.setProperty(new PropertyInt("hp", oldMasterHP + hpModifier));
        }
        // Place monster
        boolean placed = false;

        // TODO: maybe change orientation if monster doesn't fit vertically
        String size = ((PropertyString)monster.getProperty(sizeHash)).value;
        int w = Integer.parseInt(size.split("x")[0]);
        int h = Integer.parseInt(size.split("x")[1]);
        monster.setSize(w, h);

        while (tileCoords.size() > 0) {
            Vector2D option = tileCoords.get(rnd.nextInt(tileCoords.size()));
            tileCoords.remove(option);
            BoardNode position = dgs.masterBoard.getElement(option.getX(), option.getY());
            if (position.getComponentName().equals("plain") &&
                    ((PropertyInt)position.getProperty(playersHash)).value == -1) {
                // TODO: some monsters want to spawn in lava/water.
                // This can be top-left corner, check if the other tiles are valid too
                boolean canPlace = true;
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        if (i == 0 && j == 0) continue;
                        Vector2D thisTile = new Vector2D(option.getX() + j, option.getY() + i);
                        BoardNode tile = dgs.masterBoard.getElement(thisTile.getX(), thisTile.getY());
                        if (!tile.getComponentName().equals("plain") ||
                                !tileCoords.contains(thisTile) ||
                                ((PropertyInt)tile.getProperty(playersHash)).value != -1) {
                            canPlace = false;
                        }
                    }
                }
                if (canPlace) {
                    monster.setLocation(option.copy());

                    for (int i = 0; i < h; i++) {
                        for (int j = 0; j < w; j++) {
                            PropertyInt prop = new PropertyInt("players", monster.getComponentID());
                            dgs.masterBoard.getElement(option.getX() + j, option.getY() + i).setProperty(prop);
                        }
                    }
                    break;
                }
            }
        }
    }
}
