package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;

public class RoundCardTunnel extends RoundCard {

    public RoundCardTunnel(int nPlayers) {
        this.turnTypes = nPlayers > 4 ? new TurnType[4] : new TurnType[5];

        turnTypes[0] = TurnType.NormalTurn;
        turnTypes[1] = TurnType.HiddenTurn;
        turnTypes[2] = TurnType.NormalTurn;
        turnTypes[3] = TurnType.HiddenTurn;
        if (nPlayers <= 4)
            turnTypes[4] = TurnType.NormalTurn;
    }

    @Override
    public void endTurnEvent(ColtExpressGameState gameState) {
        //
    }
}
