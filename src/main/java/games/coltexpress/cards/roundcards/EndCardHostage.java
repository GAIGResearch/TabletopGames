package games.coltexpress.cards.roundcards;

import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import utilities.Utils;

public class EndCardHostage extends RoundCard {

    public EndCardHostage(){
        turnTypes = new TurnType[] {TurnType.NormalTurn, TurnType.NormalTurn,
                TurnType.HiddenTurn, TurnType.NormalTurn};
    }

    @Override
    public  void endRoundCardEvent(ColtExpressGameState gameState){
        Compartment locomotive = gameState.getTrain().getCompartment(gameState.getNPlayers());
        for (Integer playerID : locomotive.playersOnTopOfCompartment){
            gameState.addLoot(playerID, new Loot(Loot.LootType.Purse, 250));
        }
        for (Integer playerID : locomotive.playersInsideCompartment){
            gameState.addLoot(playerID, new Loot(Loot.LootType.Purse, 250));
        }
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        gameState.endGame();
    }

}
