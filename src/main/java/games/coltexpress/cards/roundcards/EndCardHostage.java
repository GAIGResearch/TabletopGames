package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;

public class EndCardHostage extends RoundCard {

    public EndCardHostage(){
        turnTypes = new TurnType[] {TurnType.NormalTurn, TurnType.NormalTurn,
                TurnType.HiddenTurn, TurnType.NormalTurn};
    }

    @Override
    public void endRoundCardEvent(ColtExpressGameState gameState) {
        //todo Hostage - All bandits in or on the locomotive collect $250 ransom.
        gameState.endGame();
    }

}
