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

    public final int cardIdx, playerDiscarding;
    private int cardId;
    private boolean executed;

    public MovePlayerWithCard(MoveType type, int playerToMove, String city, int cardIdx, int playerDiscarding) {
        super(type, playerToMove, city);
        this.cardIdx = cardIdx;
        this.playerDiscarding = playerDiscarding;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        Deck<Card> playerHand = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerHandHash, playerDiscarding);
        Deck<Card> discardPile = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerDeckDiscardHash);
        DrawCard cardDiscardAction = new DrawCard(playerHand.getComponentID(), discardPile.getComponentID(), cardIdx);
        if (super.execute(gs) && cardDiscardAction.execute(gs)) {
            executed = true;
            cardId = cardDiscardAction.getCardId();
            return true;
        }
        return false;
    }

    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            Deck<Card> deck = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerHandHash, playerDiscarding);
            return deck.getComponents().get(cardIdx);
        }
        return (Card) gs.getComponentById(cardId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovePlayerWithCard)) return false;
        if (!super.equals(o)) return false;
        MovePlayerWithCard that = (MovePlayerWithCard) o;
        return cardIdx == that.cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIdx);
    }

    @Override
    public String toString() {
        return moveType.name() + ": p" + playerToMove + " to " + destination + " with card " + cardIdx + " of p" + playerDiscarding;
    }

    @Override
    public AbstractAction copy() {
        return new MovePlayerWithCard(moveType, playerToMove, destination, cardIdx, playerDiscarding);
    }
}
