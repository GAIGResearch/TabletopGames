package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import games.coltexpress.components.Train;

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
        Train train = gameState.getTrain();
        for (int i = 0; i < train.getSize(); i++){
            Compartment c = train.getCompartment(i);
            if (c.containsMarshal){
                c.lootInside.add(new Loot(Loot.LootType.Strongbox, 1000));
                break;
            }
        }
    }
}
