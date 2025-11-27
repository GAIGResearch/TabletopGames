package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.util.Objects;

public class Discard extends AbstractAction implements IPrintable {

    final int playerID;
    final int discardCardId;
    final int discardPileId;

    public Discard(int discardCardId, int discardPileId, int playerID) {
        this.discardCardId = discardCardId;
        this.discardPileId = discardPileId;
        this.playerID = playerID;
    }

    // TODO add an add function to make the top card hidden then add a new top card.
    @Override
    public boolean execute(AbstractGameState gameState) {
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gameState;
        SeaSaltPaperCard discardCard =  (SeaSaltPaperCard) sspgs.getComponentById(discardCardId);
        Deck<SeaSaltPaperCard> discardPile = (Deck<SeaSaltPaperCard>) sspgs.getComponentById(discardPileId);
        PartialObservableDeck<SeaSaltPaperCard> playerHand = sspgs.getPlayerHands().get(playerID);
        playerHand.remove(discardCard);
        discardPile.add(discardCard);
        return true;
    }

    @Override
    public Discard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Discard discard = (Discard) o;
        return playerID == discard.playerID && discardCardId == discard.discardCardId && discardPileId == discard.discardPileId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, discardCardId, discardPileId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Discard " + gameState.getComponentById(discardCardId).toString() + " to " + gameState.getComponentById(discardPileId).getComponentName();
    }

    @Override
    public String toString() {
        return "Discard card " + discardCardId + " to pile " + discardPileId;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(getString(gameState));
    }
}
