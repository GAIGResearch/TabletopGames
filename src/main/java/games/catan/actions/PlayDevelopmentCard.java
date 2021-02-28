package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTurnOrder;

import java.time.Year;

public class PlayDevelopmentCard extends AbstractAction {
    Card card;

    public PlayDevelopmentCard(Card card){
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        Deck<Card> playerDevDeck = (Deck<Card>)cgs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        Deck<Card> developmentDiscardDeck = (Deck<Card>)cgs.getComponent(CatanConstants.developmentDiscardDeck);

        // check type of card and execute the relevant action
        // todo set game phase
        String cardType = card.getProperty(CatanConstants.cardType).toString();
        if (card.getProperty(CatanConstants.cardType).toString().equals("Knight")){
            cgs.addKnight(cgs.getCurrentPlayer());
            ((CatanTurnOrder)cgs.getTurnOrder()).addAllReactivePlayers(gs);
            cgs.setGamePhase(CatanGameState.CatanGamePhase.Discard);
        } else if (card.getProperty(CatanConstants.cardType).toString().equals("Monopoly")) {
            System.out.println("The player picks a resource and all other players have to give all the their resources of that type");
            // todo make sure that it is applicable to all resources
            new Monopoly(CatanParameters.Resources.BRICK, card);
        }else if (card.getProperty(CatanConstants.cardType).toString().equals("Year of Plenty")) {
            System.out.println("Take any 2 resources from the resourceDeck");
            new YearOfPlenty(CatanParameters.Resources.BRICK, CatanParameters.Resources.BRICK, card);
        }else if (card.getProperty(CatanConstants.cardType).toString().equals("Road Building")) {
            System.out.println("Player can immediately place 2 roads for free");
        }


        playerDevDeck.remove(card);
        developmentDiscardDeck.add(card);

        ((CatanTurnOrder)cgs.getTurnOrder()).setDevelopmentCardPlayed(true);

        return false;
    }

    @Override
    public AbstractAction copy() {
        return new PlayDevelopmentCard(card.copy());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof PlayDevelopmentCard){
            PlayDevelopmentCard otherAction = (PlayDevelopmentCard)other;
            return card.equals(otherAction.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play Development Card card = " + card.toString();
    }
}
