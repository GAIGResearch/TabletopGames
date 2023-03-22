package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.catan.CatanGameState;
import games.catan.components.CatanCard;

import java.util.Objects;
import java.util.Optional;

public class PlayKnightCard extends AbstractAction {
    public final int player;

    public PlayKnightCard(int player){
        this.player = player;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        Deck<CatanCard> playerDevDeck = cgs.getPlayerDevCards(player);

        Optional<CatanCard> knight = playerDevDeck.stream()
                .filter(card -> card.cardType == CatanCard.CardType.KNIGHT_CARD)
                .findFirst();
        if (knight.isPresent()){
            CatanCard card = knight.get();

            cgs.addKnight(player);
            cgs.setGamePhase(CatanGameState.CatanGamePhase.Robber);

            playerDevDeck.remove(card);
            cgs.setDevelopmentCardPlayed(true);

            // TODO largest army
        } else {
            throw new AssertionError("Cannot use a Knight card that is not in hand.");
        }

        return true;
    }

    @Override
    public PlayKnightCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayKnightCard)) return false;
        PlayKnightCard that = (PlayKnightCard) o;
        return player == that.player;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player);
    }

    @Override
    public String toString() {
        return player + " plays Knight Card";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
