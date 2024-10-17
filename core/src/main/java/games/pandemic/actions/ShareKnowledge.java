package games.pandemic.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.properties.PropertyString;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static core.CoreConstants.nameHash;
import static core.CoreConstants.playerHandHash;

/**
 * DrawCard wrapper with intermediate giver player ID and receiver player ID as intermediates for more information.
 */
public class ShareKnowledge extends DrawCard {
    int giver;
    int receiver;

    public ShareKnowledge(int giver, int receiver, int cardIdx) {
        super();
        this.fromIndex = cardIdx;
        this.giver = giver;
        this.receiver = receiver;
    }

    @Override
    public ShareKnowledge copy() {
        return new ShareKnowledge(giver, receiver, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState) gs;
        Deck<Card> giverHand = (Deck<Card>) pgs.getComponent(playerHandHash, giver);
        Deck<Card> receiverHand = (Deck<Card>) pgs.getComponent(playerHandHash, receiver);
        this.deckFrom = giverHand.getComponentID();
        this.deckTo = receiverHand.getComponentID();
        return super.execute(gs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShareKnowledge)) return false;
        if (!super.equals(o)) return false;
        ShareKnowledge that = (ShareKnowledge) o;
        return giver == that.giver && receiver == that.receiver;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), giver, receiver);
    }

    @Override
    public Card getCard(AbstractGameState gs) {
        if (!executed || cardId == -1) {
            if (fromIndex == -1) return null;
            PandemicGameState pgs = (PandemicGameState) gs;
            Deck<Card> giverHand = (Deck<Card>) pgs.getComponent(playerHandHash, giver);
            if (giverHand != null) return giverHand.get(fromIndex);
            Card c = (Card) gs.getComponentById(cardId);
            cardId = c.getComponentID();
            return c;
        }
        return (Card) gs.getComponentById(cardId);
    }

    @Override
    public String toString() {
        return "ShareKnowledge{" +
                "cardIdx=" + fromIndex +
                ", giver=" + giver +
                ", receiver=" + receiver +
                '}';
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Share Knowledge: " + giver + " gives " + ((PropertyString)(getCard(gameState).getProperty(nameHash))).value + " to " + receiver;
    }

    public int getGiver() {
        return giver;
    }

    public int getReceiver() {
        return receiver;
    }
}
