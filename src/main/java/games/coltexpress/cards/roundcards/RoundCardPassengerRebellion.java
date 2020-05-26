package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import games.coltexpress.components.Train;

public class RoundCardPassengerRebellion extends RoundCard {

    public RoundCardPassengerRebellion(int nPlayers){
        if ( nPlayers > 4){
            turnTypes = new RoundCard.TurnType[4];
            turnTypes[0] = TurnType.NormalTurn;
            turnTypes[1] = TurnType.HiddenTurn;
            turnTypes[2] = TurnType.NormalTurn;
            turnTypes[3] = TurnType.ReverseTurn;
        } else {
            turnTypes = new RoundCard.TurnType[5];
            turnTypes[0] = TurnType.NormalTurn;
            turnTypes[1] = TurnType.NormalTurn;
            turnTypes[2] = TurnType.HiddenTurn;
            turnTypes[3] = TurnType.NormalTurn;
            turnTypes[4] = TurnType.NormalTurn;
        }
    }

    @Override
    public void endRoundCardEvent(ColtExpressGameState gameState) {
        Train train = gameState.getTrain();
        for (int i = 0; i < train.getSize(); i++){
            Compartment c = train.getCompartment(i);
            for (Integer playerID : c.playersInsideCompartment){
                gameState.addNeutralBullet(playerID);
            }
        }
    }
}
