package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Train;

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
    public void endRoundCardEvent(ColtExpressGameState gameState) {
        Train train = gameState.getTrain();
        Compartment caboose = train.getCompartment(0);
        for (int i = 0; i < train.getSize(); i++){
            Compartment compartment = train.getCompartment(i);
            caboose.playersOnTopOfCompartment.addAll(compartment.playersOnTopOfCompartment);
            compartment.playersOnTopOfCompartment.clear();
        }
    }
}
