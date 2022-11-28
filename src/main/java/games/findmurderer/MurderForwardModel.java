package games.findmurderer;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.GridBoard;
import games.findmurderer.actions.Kill;
import games.findmurderer.actions.LookAt;
import games.findmurderer.actions.Move;
import games.findmurderer.actions.Query;
import games.findmurderer.components.Person;
import utilities.Utils;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MurderForwardModel extends AbstractForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        // Cast variables to correct types and create random number generator with correct random seed
        MurderGameState mgs = (MurderGameState) firstState;
        MurderParameters mp = (MurderParameters) firstState.getGameParameters();
        mp.civilianPolicy.setForwardModel(this);
        Random r = new Random(mp.getRandomSeed());

        // Create grid
        mgs.grid = new GridBoard<>(mp.gridWidth, mp.gridHeight);
        mgs.personToPositionMap = new HashMap<>();

        // Initialize detective information
        mgs.detectiveInformation = new HashMap<>();
        mgs.detectiveFocus = new Vector2D(mp.gridWidth/2, mp.gridHeight/2);  // Focus starts in the center

        // Add people to random locations in the grid: respect percentage of grid that should be covered by people
        int placed = 0;
        while (1.0*placed/(mp.gridWidth*mp.gridHeight) < mp.percPeopleOnGrid) {
            boolean p = false;
            while (!p) {
                int randX = r.nextInt(mp.gridWidth);
                int randY = r.nextInt(mp.gridHeight);
                if (mgs.grid.getElement(randX, randY) != null) continue;
                Person person = new Person();
                mgs.grid.setElement(randX, randY, person);
                mgs.personToPositionMap.put(person.getComponentID(), new Vector2D(randX, randY));
                p = true;
            }
            placed++;
        }

        // Select murderer at random
        int idx = r.nextInt(placed);
        mgs.killer = mgs.grid.getNonNullComponents().get(idx);
        mgs.killer.setPersonType(Person.PersonType.Killer);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        // Apply action
        action.execute(currentState);

        // Check end of game
        MurderGameState mgs = (MurderGameState) currentState;
        MurderParameters mp = (MurderParameters) currentState.getGameParameters();
        if (mgs.killer.status == Person.Status.Dead) {
            // Killer died, detective wins
            mgs.setGameStatus(Utils.GameResult.GAME_END);
            mgs.setPlayerResult(Utils.GameResult.WIN, MurderGameState.PlayerMapping.Detective.playerIdx);
            mgs.setPlayerResult(Utils.GameResult.LOSE, MurderGameState.PlayerMapping.Killer.playerIdx);
            return;
        } else {
            // Killer wins if enough civilians killed, defined by game parameter
            if (mgs.countDeadPerc() >= mp.percCivilianDeadWinKiller) {
                // Killer wins
                mgs.setGameStatus(Utils.GameResult.GAME_END);
                mgs.setPlayerResult(Utils.GameResult.LOSE, MurderGameState.PlayerMapping.Detective.playerIdx);
                mgs.setPlayerResult(Utils.GameResult.WIN, MurderGameState.PlayerMapping.Killer.playerIdx);
                return;
            }
            // Game also ends on max. ticks
            if (mgs.getTurnOrder().getRoundCounter() >= mp.maxTicks) {
                // Detective wins
                mgs.setGameStatus(Utils.GameResult.GAME_END);
                mgs.setPlayerResult(Utils.GameResult.WIN, MurderGameState.PlayerMapping.Detective.playerIdx);
                mgs.setPlayerResult(Utils.GameResult.LOSE, MurderGameState.PlayerMapping.Killer.playerIdx);
                return;
            }
        }

        // If not ended ...

        // Move civilians that are alive in the grid, after the detective moves
        if (currentState.getCurrentPlayer() == MurderGameState.PlayerMapping.Detective.playerIdx) {
            for (int i = 0; i < mgs.getGrid().getHeight(); i++) {
                for (int j = 0; j < mgs.getGrid().getWidth(); j++) {
                    Person p = mgs.getGrid().getElement(j, i);
                    if (p != null && p.personType != Person.PersonType.Killer && p.status == Person.Status.Alive) {
                        List<AbstractAction> moves = calculateMoves(mgs, p, j, i);
                        // Can also stay where they are, ensure that all can at least do this.
                        moves.add(new Move(p.getComponentID(), new Vector2D(j, i), new Vector2D(j, i)));
                        // Get chosen move and apply it in state
                        mp.civilianPolicy.getAction(mgs, moves).execute(mgs);  // TODO: potentially partial observations
                    }
                }
            }
        }

        // It's the next player's turn
        currentState.getTurnOrder().endPlayerTurn(currentState);
    }

    private List<AbstractAction> calculateMoves(MurderGameState mgs, Person p, int currentX, int currentY) {
        List<AbstractAction> moves = new ArrayList<>();
        int w = mgs.grid.getWidth();
        int h = mgs.grid.getHeight();
        for (MurderParameters.Direction d: MurderParameters.Direction.values()) {
            Vector2D targetPos = new Vector2D(currentX + d.xDiff, currentY + d.yDiff);
            // Cannot go off the grid
            if (targetPos.getX() >= 0 && targetPos.getX() < w
                    && targetPos.getY() >= 0 && targetPos.getY() < h
                    // Cannot overlap other people, only 1 per cell
                    && mgs.getGrid().getElement(targetPos.getX(), targetPos.getY()) == null) {
                moves.add(new Move(p.getComponentID(), new Vector2D(currentX, currentY), targetPos));
            }
        }
        return moves;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        // Create action list for current player
        MurderGameState mgs = (MurderGameState) gameState;
        MurderParameters mp = (MurderParameters) gameState.getGameParameters();
        int currentPlayer = gameState.getCurrentPlayer();
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Vector2D killerPosition = mgs.personToPositionMap.get(mgs.killer.getComponentID());

        for (Person p: mgs.grid.getNonNullComponents()) {
            // Can only interact with alive people
            if (p.status != Person.Status.Alive) continue;

            // Killer extra conditions for kill viability
            if (currentPlayer == MurderGameState.PlayerMapping.Killer.playerIdx) {
                // Killer can't suicide
                if (p.personType == Person.PersonType.Killer) continue;

                // Killer can only kill within some range defined in parameters
                double distance = mp.distanceFunction.apply(killerPosition, mgs.personToPositionMap.get(p.getComponentID()));
                if (distance > mp.killerMaxRange) continue;
            }

            // Detective can only interact with people in vision range
            if (currentPlayer == MurderGameState.PlayerMapping.Detective.playerIdx) {
                double distance = mp.distanceFunction.apply(mgs.detectiveFocus, mgs.personToPositionMap.get(p.getComponentID()));
                if (distance > mp.detectiveVisionRange) continue;

                // Detective can query this person to update their information of interactions
                actions.add(new Query(p.getComponentID()));
            }

            // Both killer and detective can kill this person
            actions.add(new Kill(p.getComponentID()));
        }

        if (currentPlayer == MurderGameState.PlayerMapping.Detective.playerIdx) {
            // Detective can change focus on grid
            for (int i = 0; i < mgs.grid.getHeight(); i++) {
                for (int j = 0; j < mgs.grid.getWidth(); j++) {
                    actions.add(new LookAt(new Vector2D(j, i)));
                }
            }
        }

        if (currentPlayer == MurderGameState.PlayerMapping.Killer.playerIdx) {
            // Killer can move
            actions.addAll(calculateMoves(mgs, mgs.killer, killerPosition.getX(), killerPosition.getY()));
        }

        // Can always pass
        actions.add(new DoNothing());

        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        // No state, no need to copy
        return this;
    }
}
