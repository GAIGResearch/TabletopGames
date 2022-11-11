package games.findmurderer;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.GridBoard;
import games.findmurderer.actions.Kill;
import games.findmurderer.components.Person;
import utilities.Distance;
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
        Random r = new Random(mp.getRandomSeed());

        // Create grid
        mgs.grid = new GridBoard<>(mp.gridWidth, mp.gridHeight);
        mgs.personToPositionMap = new HashMap<>();

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
        }

        // If not ended, it's the next player's turn
        currentState.getTurnOrder().endPlayerTurn(currentState);

        List<AbstractAction> availableActionsNext = computeAvailableActions(currentState);
        if (availableActionsNext.size() == 1) {
            // Only pass actions available next, that player lost
            int player = currentState.getCurrentPlayer();
            mgs.setGameStatus(Utils.GameResult.GAME_END);
            mgs.setPlayerResult(Utils.GameResult.LOSE, player);
            mgs.setPlayerResult(Utils.GameResult.WIN, 1-player); // Assumes 2-player only
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        // Create action list for current player
        MurderGameState mgs = (MurderGameState) gameState;
        MurderParameters mp = (MurderParameters) gameState.getGameParameters();
        int currentPlayer = gameState.getCurrentPlayer();
        ArrayList<AbstractAction> actions = new ArrayList<>();

        for (Person p: mgs.grid.getNonNullComponents()) {
            // Can only kill alive people
            if (p.status != Person.Status.Alive) continue;

            // Killer extra conditions for kill viability
            if (currentPlayer == MurderGameState.PlayerMapping.Killer.playerIdx) {
                // Killer can't suicide
                if (p.personType == Person.PersonType.Killer) continue;

                // Killer can only kill within some range defined in parameters
                double distance = Distance.euclidian_distance(mgs.personToPositionMap.get(mgs.killer.getComponentID()), mgs.personToPositionMap.get(p.getComponentID()));
                if (distance > mp.killerMaxRange) continue;
            }

            // Add one kill action for each person we can kill
            actions.add(new Kill(p.getComponentID()));
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
