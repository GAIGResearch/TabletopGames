package games.coltexpress.cards.roundcards;

import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import games.coltexpress.components.Train;

public class EndCardMarshallsRevenge extends RoundCard{

    public EndCardMarshallsRevenge(){
        turnTypes = new TurnType[] {TurnType.NormalTurn, TurnType.NormalTurn,
        TurnType.HiddenTurn, TurnType.NormalTurn};
    }

    @Override
    public void endRoundCardEvent(ColtExpressGameState gameState) {
        Train train = gameState.getTrain();
        for (int i = 0; i < train.getSize(); i++){
            Compartment c = train.getCompartment(i);
            if (c.containsMarshal){
                for (Integer playerID : c.playersOnTopOfCompartment){
                    PartialObservableDeck<Loot> playerLoot = gameState.getLoot(playerID);
                    Loot lestValueablePurse = null;
                    for (Loot loot : playerLoot.getCards()) {
                        if (loot.getType() == Loot.LootType.Purse &&
                                (lestValueablePurse == null || lestValueablePurse.getValue() < loot.getValue()))
                            lestValueablePurse = loot;
                    }
                    if (lestValueablePurse != null)
                        playerLoot.remove(lestValueablePurse);
                }
                break;
            }
        }

        gameState.endGame();
    }
}
