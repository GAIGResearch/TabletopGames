package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;

public class EndCardMarshallsRevenge extends RoundCard{

    public EndCardMarshallsRevenge(){
        turnTypes = new TurnType[] {TurnType.NormalTurn, TurnType.NormalTurn,
        TurnType.HiddenTurn, TurnType.NormalTurn};
    }

    @Override
    public void endRoundCardEvent(ColtExpressGameState gameState) {
        //todo  Marshall's Revenge - All bandits on the roof of the Marshall's car drop their least valuable purse.
        gameState.endGame();
    }
}
