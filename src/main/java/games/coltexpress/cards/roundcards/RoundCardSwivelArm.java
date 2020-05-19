package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;

public class RoundCardSwivelArm extends RoundCard{

    public RoundCardSwivelArm(int nPlayers){
        this.turnTypes = nPlayers > 4 ? new TurnType[3] : new TurnType[4];

        turnTypes[0] = TurnType.NormalTurn;
        turnTypes[1] = TurnType.HiddenTurn;
        turnTypes[2] = TurnType.NormalTurn;
        if (nPlayers <= 4)
            turnTypes[3] = TurnType.NormalTurn;
    }

    @Override
    public void endTurnEvent(ColtExpressGameState gameState) {
        //todo: Swivel Arm - All bandits on the roof of the train are swept to the caboose.
    }
}
