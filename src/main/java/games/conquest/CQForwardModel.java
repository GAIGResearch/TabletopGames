package games.conquest;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.GridBoard;
import core.components.PartialObservableDeck;
import games.conquest.components.*;
import utilities.Vector2D;

import java.util.*;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class CQForwardModel extends StandardForwardModel {
    private int maxTroops;
    private int maxCommands;

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        CQGameState cqgs = (CQGameState) firstState;
        CQParameters cqp = (CQParameters) firstState.getGameParameters();

        maxTroops = cqp.maxTroops;
        maxCommands = cqp.maxCommands;

        cqgs.setGamePhase(CQGameState.CQGamePhase.SelectionPhase);

        cqgs.locationToTroopMap = new HashMap<Vector2D, Integer>();
        cqgs.cells = new Cell[cqp.gridHeight][cqp.gridWidth];
        cqgs.troops = new HashSet<Troop>();
        cqgs.gridBoard = new GridBoard<>(cqp.gridWidth, cqp.gridHeight);
        cqgs.chosenCommands = new PartialObservableDeck[]{
                new PartialObservableDeck<Command>("commands0", 0, 2, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER),
                new PartialObservableDeck<Command>("commands1", 1, 2, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER)
        };

        // Initialize cells and add to gridBoard
        for (int i=0; i<cqgs.cells.length; i++) {
            for (int j=0; j<cqgs.cells[i].length; j++) {
                Cell cell = new Cell(i, j);
                cqgs.gridBoard.setElement(i, j, cell);
            }
        }

        this.setupCommands(cqp.p0TroopSetup.commands, 0, cqgs);
        this.setupTroopsFromString(cqp.p0TroopSetup.troops, 0, cqgs, cqp);
        this.setupCommands(cqp.p1TroopSetup.commands, 1, cqgs);
        this.setupTroopsFromString(cqp.p1TroopSetup.troops, 1, cqgs, cqp);
        for (int i = 0; i < cqp.gridHeight; i++) {
            for (int j = 0; j < cqp.gridWidth; j++) {
                cqgs.cells[i][j] = new Cell(i, j);
            }
        }
    }


    private void setupCommands(HashSet<CommandType> commands, int uid, CQGameState cqgs) {
        boolean[] visibility = new boolean[] {uid==0, uid==1};
        for (CommandType cmd : commands) {
            if (cqgs.chosenCommands[uid].getSize() < maxCommands) {
                cqgs.chosenCommands[uid].add(new Command(cmd, uid), visibility);
            }
        }
    }

    /**
     * Use a string to set up troops on individual positions.
     * The string should consist of up to 3 lines containing at most 20 characters.
     * The lines represent front-to-back order, from left to right.
     * An empty line indicates no troops on that line.
     * Entering a single line will place the troops as far to the front as possible.
     */
    protected void setupTroopsFromString(String str, int uid, CQGameState cqgs, CQParameters cqp) {
        String[] lines = str.split("\n", -1);
        assert(lines.length <= cqp.nSetupRows);
        assert(Arrays.stream(lines).mapToInt(String::length).max().orElse(0) <= cqp.gridWidth); // lines[i].length() <= cqp.gridWidth for all i
        Troop unit;
        int nTroops = 0; // keep track of troops for this owner
        for (int j = 0; j < lines.length; j++) {
            for (int i = 0; i < cqp.gridWidth; i++) {
                if (lines[j].length() <= i) break;
                char ch = lines[j].charAt(i);
                unit = switch (ch) {
                    case ' ' -> null;
                    case 'S' -> new Troop(TroopType.Scout, uid);
                    case 'F' -> new Troop(TroopType.FootSoldier, uid);
                    case 'H' -> new Troop(TroopType.Halberdier, uid);
                    case 'A' -> new Troop(TroopType.Archer, uid);
                    case 'M' -> new Troop(TroopType.Mage, uid);
                    case 'K' -> new Troop(TroopType.Knight, uid);
                    case 'C' -> new Troop(TroopType.Champion, uid);
                    default -> throw new IllegalStateException("Unexpected value: " + ch);
                };
                if (unit == null) continue;
                else nTroops++;
                cqgs.troops.add(unit);
                int x,y;
                if (uid == 0) {
                    x = i;
                    // move troops forward as much as possible, when fewer than 3 lines are provided.
                    y = cqp.nSetupRows - lines.length + j;
                } else {
                    x = cqp.gridWidth-1 - i;
                    // move troops forward as much as possible, when fewer than 3 lines are provided.
                    y = cqp.gridHeight-1 - (cqp.nSetupRows - lines.length + j);
                }
                cqgs.addTroop(unit, new Vector2D(x, y));
                if (nTroops >= maxTroops) return;
            }
        }
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        CQGameState cqgs = (CQGameState) gameState;
        return cqgs.getAvailableActions();
    }

    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        if (currentState.isActionInProgress()) return;
        CQGameState cqgs = (CQGameState) currentState;
        CQParameters cqp = (CQParameters) currentState.getGameParameters();
        // This is only called after an EndTurn action, so end the turn:
        cqgs.endTurn();
        endPlayerTurn(cqgs);
    }
}