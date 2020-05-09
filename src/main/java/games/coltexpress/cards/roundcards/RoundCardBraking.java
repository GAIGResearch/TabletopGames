package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Train;

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
        Train train = gameState.getTrain();
        Compartment targetCompartment = train.getCompartment(train.getSize()-1);
        Compartment sourceCompartment;
        for (int i = train.getSize()-2; i >= 0; i--){
            sourceCompartment = train.getCompartment(i);
            targetCompartment.playersOnTopOfCompartment.addAll(sourceCompartment.playersOnTopOfCompartment);
            sourceCompartment.playersOnTopOfCompartment.clear();
            targetCompartment = sourceCompartment;
        }
    }
}
