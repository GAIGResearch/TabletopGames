package games.uno;

import core.actions.IAction;
import core.AbstractGameState;
import core.ForwardModel;

public class UnoForwardModel extends ForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {

    }

    @Override
    public void next(AbstractGameState gameState, IAction action) {
        action.execute(gameState);
        gameState.getTurnOrder().endPlayerTurn(gameState);

        if (gameState.getTurnOrder().getRoundCounter() == 9) {
            gameState.endGame();
        }

        checkWinCondition((UnoGameState) gameState);
    }

    public void checkWinCondition(UnoGameState gameState) {
        for (int i = 0; i < gameState.getNPlayers(); i++)
        {
            if (gameState.playerDecks.get(i).getSize() == 0)
                gameState.registerWinner(i);
        }
    }


}
