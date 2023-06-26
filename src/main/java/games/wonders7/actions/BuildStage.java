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
    private final int wonderStage;

    public BuildStage(String cardName, int wonderStage){
        this.cardName = cardName;
        this.wonderStage = wonderStage;
    }

    @Override
    public boolean execute(AbstractGameState gameState){
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        // Finds the played card
        int index=0; // The index of the card in hand
        for (int i=0; i<wgs.getPlayerHand(wgs.getCurrentPlayer()).getSize(); i++){ // Goes through each card in the playerHand
            if (cardName.equals(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName)){ // If cardName is the one searching for (being played)
                index = i;
            }
        }
        Wonder7Card card = wgs.getPlayerHand(wgs.getCurrentPlayer()).get(index); // Card being selected


        // The second stage has been built, now the player can play their special action (if they have the wonder)
        if (wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).wonderStage == 2){
            Wonder7Board board = wgs.getPlayerWonderBoard(wgs.getCurrentPlayer());
            switch (board.type){
                case lighthouse:
                case mausoleum:
                case gardens:
                case statue:
                    wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).effectUsed = false;
                default:
                    break;
            }}


        // Gives player resources produced from stage
        Set<Wonders7Constants.Resource> keys = wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).stageProduce.get(wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).wonderStage-1).keySet(); // Gets all the resources the stage provides
        for (Wonders7Constants.Resource resource: keys){  // Goes through all keys for each resource
            int stageValue =  wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).stageProduce.get(wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).wonderStage-1).get(resource); // Number of resource the stage provides
            int playerValue = wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource); // Number of resource the player owns
            wgs.getPlayerResources(wgs.getCurrentPlayer()).put(resource, playerValue + stageValue); // Adds the resources provided by the stage to the players resource count
        }

        // remove the card from the players hand to the playedDeck
        wgs.getPlayerHand(wgs.getCurrentPlayer()).remove(card);
        wgs.getDiscardPile().add(card);

        wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).changeStage(); // Increases wonderstage value to the next stage
        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {return toString();}


    @Override
    public String toString() {return "Built stage " + wonderStage + " using " + cardName;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildStage)) return false;
        BuildStage that = (BuildStage) o;
        return wonderStage == that.wonderStage && Objects.equals(cardName, that.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardName, wonderStage);
    }

    @Override
    public AbstractAction copy() {return this; }
}
