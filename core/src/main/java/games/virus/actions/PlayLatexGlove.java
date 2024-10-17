package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.virus.VirusGameParameters;
import games.virus.cards.VirusCard;

import java.util.Objects;

public class PlayLatexGlove extends PlayVirusCard implements IPrintable {
    private int otherPlayerId;
    private int otherPlayerHandId;

    public PlayLatexGlove(int deckFrom, int deckTo, int fromIndex, int bodyId, int otherPlayerId, int otherPlayerHandId) {
        super(deckFrom, deckTo, fromIndex, bodyId);
        this.otherPlayerId = otherPlayerId;
        this.otherPlayerHandId = otherPlayerHandId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        // Discard three card on other player hand
        Deck<Card> to = (Deck<Card>) gs.getComponentById(deckTo);
        Deck<Card> otherPlayerHand = (Deck<Card>) gs.getComponentById(otherPlayerHandId);

        int nCards = ((VirusGameParameters) gs.getGameParameters()).nCardsDiscardLatexGlove;
        for (int i = 0; i < nCards; i++) {
            VirusCard card = (VirusCard) otherPlayerHand.draw();
            if (card != null)
                to.add(card, toIndex);
        }
        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + getCard(gameState).toString() + " on player " + otherPlayerId;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play Treatment Latex Glove");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayLatexGlove)) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), otherPlayerId, otherPlayerHandId);
    }

    @Override
    public AbstractAction copy() {
        return new PlayLatexGlove(deckFrom, deckTo, fromIndex, bodyId, otherPlayerId, otherPlayerHandId);
    }
}
