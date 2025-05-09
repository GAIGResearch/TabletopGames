package games.conquest;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.GridBoard;
import core.components.PartialObservableDeck;
import games.conquest.actions.CQAction;
import games.conquest.actions.EndTurn;
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

//    public CQForwardModel() {
//        super();
//    }

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
        cqgs.cells = new Cell[cqp.gridWidth][cqp.gridHeight];
        cqgs.troops = new HashSet<Troop>();
        cqgs.gridBoard = new GridBoard(cqp.gridWidth, cqp.gridHeight);
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

        if (cqp.p0TroopSetup.equals(CQParameters.Setup.Empty)) {
            cqgs.setGamePhase(CQGameState.CQGamePhase.SetupPhase);
        } else {
            cqgs.setupCommands(cqp.p0TroopSetup.commands, 0);
            cqgs.setupTroopsFromString(cqp.p0TroopSetup.troops, 0);
            cqgs.setupCommands(cqp.p1TroopSetup.commands, 1);
            cqgs.setupTroopsFromString(cqp.p1TroopSetup.troops, 1);
        }
        for (int i = 0; i < cqp.gridHeight; i++) {
            for (int j = 0; j < cqp.gridWidth; j++) {
                cqgs.cells[i][j] = new Cell(i, j);
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
        if (currentState.isActionInProgress() || (action instanceof CQAction)) return;
        CQGameState cqgs = (CQGameState) currentState;
//        System.out.println("Doing an afterAction! Ending for " + cqgs.getCurrentPlayer());
//        CQParameters cqp = (CQParameters) currentState.getGameParameters();
        // This is only called after an EndTurn action, so end the turn:
        cqgs.endTurn();
        endPlayerTurn(cqgs);
    }
}