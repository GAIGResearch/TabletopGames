package games.uno;

import actions.IAction;
import core.AbstractGameState;
import core.ForwardModel;
import turnorder.TurnOrder;

public class UnoForwardModel extends ForwardModel {

    @Override
    public void next(AbstractGameState gameState, TurnOrder turnOrder, IAction action) {
        action.Execute(gameState, turnOrder);
        turnOrder.endPlayerTurn(gameState);

        if (turnOrder.getTurnCounter() == 9)
            gameState.endGame();

        checkWinCondition((UnoGameState) gameState);
    }

    public void checkWinCondition(UnoGameState gameState) {
        for (int i = 0; i < gameState.getNPlayers(); i++)
        {
            if (gameState.playerDecks.get(i).getCards().size() == 0)
                gameState.registerWinner(i);
        }
    }


}
