package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;

public class RoundCardBridge extends RoundCard {

    public RoundCardBridge(int nPlayers) {
        this.turnTypes = nPlayers > 4 ? new TurnType[2] : new TurnType[3];

        turnTypes[0] = TurnType.NormalTurn;
        turnTypes[1] = TurnType.DoubleTurn;
        if (nPlayers <= 4)
            turnTypes[2] = TurnType.NormalTurn;
    }

    @Override
    public void endTurnEvent(ColtExpressGameState gameState) {
        //no special event
    }
}
