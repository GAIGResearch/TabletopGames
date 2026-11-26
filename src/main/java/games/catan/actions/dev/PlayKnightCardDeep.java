package games.catan.actions.dev;

import core.AbstractGameState;
import core.components.Deck;
import core.components.grid.HexCoord;
import games.catan.CatanGameState;
import games.catan.actions.robber.MoveRobber;
import games.catan.components.CatanCard;

import java.util.Optional;

public class PlayKnightCardDeep extends MoveRobber {

    public PlayKnightCardDeep(HexCoord hex, int player) {
        super(hex, player);
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
            playerDevDeck.remove(card);
            cgs.setDevelopmentCardPlayed(true);
        } else {
            throw new AssertionError("Cannot use a Knight card that is not in hand.");
        }

        return super.execute(gs);
    }

    @Override
    public String toString() {
        return "p" + player + " plays Dev:KnightCard. Moves robber to " + hex;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
