package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;

public class RoundCardAngryMarshall extends RoundCard{

    public RoundCardAngryMarshall(int nPlayers){
        if ( nPlayers > 4){
            turnTypes = new RoundCard.TurnType[3];
            turnTypes[0] = TurnType.NormalTurn;
            turnTypes[1] = TurnType.NormalTurn;
            turnTypes[2] = TurnType.ReverseTurn;
        } else {
            turnTypes = new RoundCard.TurnType[4];
            turnTypes[0] = TurnType.NormalTurn;
            turnTypes[1] = TurnType.NormalTurn;
            turnTypes[2] = TurnType.HiddenTurn;
            turnTypes[3] = TurnType.ReverseTurn;
        }
    }

    @Override
    public void endTurnEvent(ColtExpressGameState gameState) {
        //todo Angry Marshall - The Marshall shoots all bandits on the roof of his car and then moves one car toward the caboose.
    }
}
