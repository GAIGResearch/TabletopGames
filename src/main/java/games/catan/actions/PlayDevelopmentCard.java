package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;

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

        // todo check type of card and execute it
        card.getProperty(CatanConstants.cardType);
//        card.getProperty(CatanConstants.cardType).toString().equals("Road Building")
        // Dev card can be either:
        //        KNIGHT_CARD,
        //        PROGRESS_CARD,
        //        VICTORY_POINT_CARD

        playerDevDeck.remove(card);
        developmentDiscardDeck.add(card);

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
