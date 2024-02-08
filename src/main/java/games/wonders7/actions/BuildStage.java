package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Board;
import games.wonders7.cards.Wonder7Card;

import java.util.Objects;
import java.util.Set;

public class BuildStage extends AbstractAction {
    public final String cardName;
    private final int player;

    public BuildStage(int player, String cardName){
        this.player = player;
        this.cardName = cardName;
    }

    @Override
    public boolean execute(AbstractGameState gameState){
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        // Finds the played card
        Wonder7Card card = null;
        for (Wonder7Card cardSearch: wgs.getPlayerHand(player).getComponents()){ // Goes through each card in the playerHand
            if (cardName.equals(cardSearch.cardName)){ // If cardName is the one searching for (being played)
                card = cardSearch;
                break;
            }
        }

        if (card == null) {
            throw new AssertionError("Card not found in player hand");
        }
        
        // The second stage has been built, now the player can play their special action (if they have the wonder)
        if (wgs.getPlayerWonderBoard(player).wonderStage == 2){
            Wonder7Board board = wgs.getPlayerWonderBoard(player);
            switch (board.type){
                case TheLighthouseOfAlexandria:
                case TheMausoleumOfHalicarnassus:
                case TheHangingGardensOfBabylon:
                case TheStatueOfZeusInOlympia:
                    wgs.getPlayerWonderBoard(player).effectUsed = false;
                default:
                    break;
            }}

        // Gives player resources produced from stage
        Wonder7Board board = wgs.getPlayerWonderBoard(player);
        Set<Wonders7Constants.Resource> keys = board.type.stageProduce.get(board.wonderStage-1).keySet(); // Gets all the resources the stage provides
        for (Wonders7Constants.Resource resource: keys){  // Goes through all keys for each resource
            int stageValue = board.type.getStageProduce(board.wonderStage - 1, resource); // Number of resource the stage provides
            int playerValue = wgs.getPlayerResources(player).get(resource); // Number of resource the player owns
            wgs.getPlayerResources(player).put(resource, playerValue + stageValue); // Adds the resources provided by the stage to the players resource count
        }

        // remove the card from the players hand to the playedDeck
        boolean cardFound = wgs.getPlayerHand(player).remove(card);
        if (!cardFound) {
            throw new AssertionError("Card not found in player hand");
        }
        wgs.getDiscardPile().add(card);

        wgs.getPlayerWonderBoard(player).changeStage(); // Increases wonderstage value to the next stage
        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {return toString();}


    @Override
    public String toString() {return "Player " + player + " built wonder stage";}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildStage)) return false;
        BuildStage that = (BuildStage) o;
        return player == that.player && cardName.equals(that.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardName, player);
    }

    @Override
    public BuildStage copy() {return this; }
}
