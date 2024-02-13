package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Board;
import games.wonders7.cards.Wonder7Card;

import java.util.Objects;
import java.util.Set;

public class SpecialEffect extends DrawCard {

    public final String cardName;
    public final int player;

    // Player chooses card to play
    public SpecialEffect(int player, String cardName){
        super();
        this.player = player;
        this.cardName = cardName;
    }


    @Override
    public boolean execute(AbstractGameState gameState) {
        super.execute(gameState);
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

        Wonder7Board board = wgs.getPlayerWonderBoard(wgs.getCurrentPlayer());
        switch (board.type){
            case TheLighthouseOfAlexandria:
            case TheMausoleumOfHalicarnassus:
            case TheHangingGardensOfBabylon:
                wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).effectUsed = true;
            case TheStatueOfZeusInOlympia:
                // Gives player resources produced from card
                Set<Wonders7Constants.Resource> keys = card.resourcesProduced.keySet(); // Gets all the resources the card provides
                for (Wonders7Constants.Resource resource: keys){  // Goes through all keys for each resource
                    int cardValue = card.getNProduced(resource); // Number of resource the card provides
                    int playerValue = wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource); // Number of resource the player owns
                    wgs.getPlayerResources(wgs.getCurrentPlayer()).put(resource, playerValue + cardValue); // Adds the resources provided by the card to the players resource count
                }

                // remove the card from the players hand to the playedDeck
                boolean cardFound = wgs.getPlayerHand(wgs.getCurrentPlayer()).remove(card);
                if (!cardFound) {
                    throw new AssertionError("Card not found in player hand");
                }
                wgs.getPlayedCards(wgs.getCurrentPlayer()).add(card);
                wgs.getPlayerWonderBoard(wgs.getCurrentPlayer()).effectUsed = true;
                return true;
            default:
                break;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Player " + player + " uses Wonder special effect with card " + cardName;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpecialEffect)) return false;
        if (!super.equals(o)) return false;
        SpecialEffect that = (SpecialEffect) o;
        return player == that.player && Objects.equals(cardName, that.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardName, player);
    }

    @Override
    public SpecialEffect copy(){return this;}
}
