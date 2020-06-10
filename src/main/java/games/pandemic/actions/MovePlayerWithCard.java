package games.pandemic.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;
import static core.CoreConstants.playerHandHash;


@SuppressWarnings("unchecked")
public class MovePlayerWithCard extends MovePlayer {

    private int cardIdx;
    private int cardId;
    private boolean executed;

    public MovePlayerWithCard(int playerIdx, String city, int cardIdx) {
        super(playerIdx, city);
        this.cardIdx = cardIdx;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        executed = true;
        Deck<Card> playerHand = (Deck<Card>) ((PandemicGameState)gs).getComponentActingPlayer(playerHandHash);
        Deck<Card> discardPile = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerDeckDiscardHash);
        cardId = playerHand.getComponents().get(cardIdx).getComponentID();
        return super.execute(gs) & new DrawCard(playerHand.getComponentID(), discardPile.getComponentID(), cardIdx).execute(gs);
    }

    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            Deck<Card> deck = (Deck<Card>) ((PandemicGameState)gs).getComponentActingPlayer(playerHandHash);
            return deck.getComponents().get(cardIdx);
        }
        return (Card) gs.getComponentById(cardId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MovePlayerWithCard that = (MovePlayerWithCard) o;
        return cardIdx == that.cardIdx &&
                cardId == that.cardId &&
                executed == that.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIdx, cardId, executed);
    }

    @Override
    public String toString() {
        return "MovePlayerWithCard{" +
                "cardIdx=" + cardIdx +
                ", cardId=" + cardId +
                ", executed=" + executed +
                '}';
    }

    @Override
    public AbstractAction copy() {
        return new MovePlayerWithCard(playerIdx, destination, cardIdx);
    }
}
