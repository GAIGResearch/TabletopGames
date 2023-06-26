package core.actions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;

import java.util.Objects;

@SuppressWarnings("unchecked")
public class MoveCardById extends AbstractAction{

    final protected int deckFromId;
    final protected int deckToId;
    final protected int cardId;

    public MoveCardById(int deckFromId, int deckToId, int cardId) {
        this.deckFromId = deckFromId;
        this.deckToId = deckToId;
        this.cardId = cardId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        Deck<Card> from = (Deck<Card>) gs.getComponentById(deckFromId);
        Deck<Card> to = (Deck<Card>) gs.getComponentById(deckToId);
        Card c = (Card) gs.getComponentById(cardId);
        if(from == null || to == null || c == null)
            return false;

        to.add(c);
        from.remove(c);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MoveCardById) {
            MoveCardById other = (MoveCardById) obj;
            return other.deckFromId == deckFromId && other.deckToId == deckToId && other.cardId == cardId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckFromId, deckToId, cardId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Deck<Card> from = (Deck<Card>) gameState.getComponentById(deckFromId);
        Deck<Card> to = (Deck<Card>) gameState.getComponentById(deckToId);
        Card card = (Card) gameState.getComponentById(cardId);
        return "Move card " + card.getComponentName()
                + " from " + from.getComponentName()
                + " to " + to.getComponentName();
    }

    @Override
    public String toString() {
        return "Move card " + cardId + " from deck " + deckFromId + " to deck " + deckToId;
    }

    public int getCardId(){
        return cardId;
    }

    public int getDeckToId(){
        return deckToId;
    }

    public int getDeckFromId(){
        return deckFromId;
    }
}
