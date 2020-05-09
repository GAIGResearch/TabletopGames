package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;

public class EndCardPickPocket extends RoundCard {

    public EndCardPickPocket(){
        turnTypes = new TurnType[] {TurnType.NormalTurn, TurnType.NormalTurn,
                TurnType.HiddenTurn, TurnType.NormalTurn};
    }

    @Override
    public void endRoundCardEvent(ColtExpressGameState gameState) {
        //todo Pick Pocket - Any bandit alone in or on a car can pick up a purse if there is one.
        gameState.endGame();
    }
}
