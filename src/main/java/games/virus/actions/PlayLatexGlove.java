package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;

import java.util.Objects;

public class PlayLatexGlove extends PlayVirusCard implements IPrintable {
    private int otherPlayerId;
    private Deck<VirusCard> otherPlayerHand;
    public PlayLatexGlove(int deckFrom, int deckTo, int fromIndex, int bodyId, int otherPlayerId, Deck<VirusCard> otherPlayerHand) {
        super(deckFrom, deckTo, fromIndex, bodyId);
        this.otherPlayerId = otherPlayerId;
        this.otherPlayerHand = otherPlayerHand;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VirusGameState vgs = (VirusGameState) gs;
        super.execute(gs);

        // Discard three card on other player hand
        Deck<Card> to = (Deck<Card>) gs.getComponentById(deckTo);

        for (int i=0; i<3; i++) {
            VirusCard card = otherPlayerHand.pick(0);
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
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), otherPlayerId, otherPlayerHand);
    }

    @Override
    public AbstractAction copy() {
        return new PlayLatexGlove(deckFrom, deckTo, fromIndex, bodyId, otherPlayerId, otherPlayerHand);
    }
}
