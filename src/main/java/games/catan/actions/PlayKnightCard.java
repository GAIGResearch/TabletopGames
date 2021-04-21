package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTurnOrder;

public class PlayKnightCard extends AbstractAction {
    //TODO HASH,Equals,Copy,State
    Card card;

    public PlayKnightCard(Card card){
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        Deck<Card> playerDevDeck = (Deck<Card>)cgs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        Deck<Card> developmentDiscardDeck = (Deck<Card>)cgs.getComponent(CatanConstants.developmentDiscardDeck);

        cgs.addKnight(cgs.getCurrentPlayer());
        cgs.setGamePhase(CatanGameState.CatanGamePhase.Robber);

        playerDevDeck.remove(card);
        developmentDiscardDeck.add(card);

        ((CatanTurnOrder)cgs.getTurnOrder()).setDevelopmentCardPlayed(true);

        return false;
    }

    @Override
    public AbstractAction copy() {
        return new PlayKnightCard(card.copy());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof PlayKnightCard){
            PlayKnightCard otherAction = (PlayKnightCard)other;
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
        return "Play Knight Card card = " + card.toString();
    }
}
