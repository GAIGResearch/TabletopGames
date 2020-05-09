package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;

public class RoundCardBraking extends RoundCard{

    public RoundCardBraking(int nPlayers){
        this.turnTypes = new TurnType[4];

        turnTypes[0] = TurnType.NormalTurn;
        turnTypes[1] = TurnType.HiddenTurn;
        turnTypes[2] = nPlayers > 4 ? TurnType.HiddenTurn : TurnType.NormalTurn;
        turnTypes[3] = TurnType.HiddenTurn;
    }

    @Override
    public void endRoundCardEvent(ColtExpressGameState gameState) {
        //todo Braking - All bandits on the roof of the train move one car toward the locomotive.
    }
}
