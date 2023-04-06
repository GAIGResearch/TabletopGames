package games.catan.actions.dev;

import core.AbstractGameState;
import core.components.Deck;
import games.catan.CatanGameState;
import games.catan.actions.robber.MoveRobberAndSteal;
import games.catan.components.CatanCard;

import java.util.Optional;

public class PlayKnightCard extends MoveRobberAndSteal {

    public PlayKnightCard(int x, int y, int player, int targetPlayer) {
        super(x, y, player, targetPlayer);
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
        return "p" + player + " plays Dev:KnightCard";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
