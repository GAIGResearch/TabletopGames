package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTurnOrder;

import java.util.Objects;
import java.util.Optional;

public class PlayKnightCard extends AbstractAction {

    public PlayKnightCard(){}
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        Deck<Card> playerDevDeck = (Deck<Card>)cgs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        Deck<Card> developmentDiscardDeck = (Deck<Card>)cgs.getComponent(CatanConstants.developmentDiscardDeck);

        Optional<Card> knight = playerDevDeck.stream()
                .filter(card -> card.getProperty(CatanConstants.cardType).toString().equals(CatanParameters.CardTypes.KNIGHT_CARD.toString()))
                .findFirst();
        if(knight.isPresent()){
            Card card = knight.get();

            cgs.addKnight(cgs.getCurrentPlayer());
            cgs.setGamePhase(CatanGameState.CatanGamePhase.Robber);

            playerDevDeck.remove(card);
            developmentDiscardDeck.add(card);

            ((CatanTurnOrder)cgs.getTurnOrder()).setDevelopmentCardPlayed(true);
        } else {
            throw new AssertionError("Cannot use a Knight card that is not in hand.");
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PlayKnightCard){
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "Play Knight Card";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
