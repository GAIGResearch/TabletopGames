package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;

public class RoundCardTakeItAll extends RoundCard{

    public RoundCardTakeItAll(int nPlayers) {
        if ( nPlayers > 4){
            turnTypes = new RoundCard.TurnType[3];
            turnTypes[0] = TurnType.NormalTurn;
            turnTypes[1] = TurnType.DoubleTurn;
            turnTypes[2] = TurnType.ReverseTurn;
        } else {
            turnTypes = new RoundCard.TurnType[4];
            turnTypes[0] = TurnType.NormalTurn;
            turnTypes[1] = TurnType.HiddenTurn;
            turnTypes[2] = TurnType.DoubleTurn;
            turnTypes[3] = TurnType.ReverseTurn;
        }
    }

    @Override
    public void endRoundCardEvent(ColtExpressGameState gameState) {
        // todo place the second strongbox in the car where the marshall currently is
    }
}
