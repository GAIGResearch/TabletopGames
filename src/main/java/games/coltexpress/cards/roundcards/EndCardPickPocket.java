package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import games.coltexpress.components.Train;

import java.util.LinkedList;
import java.util.Random;

public class EndCardPickPocket extends RoundCard {

    private Random random = new Random();

    public EndCardPickPocket(){
        turnTypes = new TurnType[] {TurnType.NormalTurn, TurnType.NormalTurn,
                TurnType.HiddenTurn, TurnType.NormalTurn};
    }

    @Override
    public void endRoundCardEvent(ColtExpressGameState gameState) {
        Train train = gameState.getTrain();
        for (int i = 0; i < train.getSize(); i++){
            Compartment currentCompartment = train.getCompartment(i);

            if (currentCompartment.playersInsideCompartment.size() == 1){
                LinkedList<Loot> purses = new LinkedList<>();
                for (Loot loot : currentCompartment.lootInside.getComponents()){
                    if (loot.getLootType() == Loot.LootType.Purse)
                        purses.add(loot);
                }
                if (purses.size() > 0){
                    for (Integer playerID : currentCompartment.playersInsideCompartment)
                        gameState.addLoot(playerID, purses.get(random.nextInt(purses.size())));
                }
            }

            if (currentCompartment.playersOnTopOfCompartment.size() == 1){
                LinkedList<Loot> purses = new LinkedList<>();
                for (Loot loot : currentCompartment.lootOnTop.getComponents()){
                    if (loot.getLootType() == Loot.LootType.Purse)
                        purses.add(loot);
                }
                if (purses.size() > 0){
                    for (Integer playerID : currentCompartment.playersOnTopOfCompartment)
                        gameState.addLoot(playerID, purses.get(random.nextInt(purses.size())));
                }
            }
        }

        gameState.endGame();
    }
}
