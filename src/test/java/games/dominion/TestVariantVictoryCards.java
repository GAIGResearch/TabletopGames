package games.dominion;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static games.dominion.DominionConstants.DeckType;
import static org.junit.Assert.assertEquals;

public class TestVariantVictoryCards {


    Random rnd = new Random(373);
    List<AbstractPlayer> players = Arrays.asList(new TestPlayer(),
            new TestPlayer(),
            new TestPlayer(),
            new TestPlayer());

    Game game = new Game(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(new DominionFGParameters(), players.size()));
    DominionForwardModel fm = new DominionForwardModel();

    @Test
    public void oneGarden() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.GARDENS, 1, DeckType.DISCARD);
        state.getDeck(DeckType.HAND, 1).remove(DominionCard.create(CardType.COPPER));
        int startScore = 3;
        for (int addedCards = 0; addedCards < 33; addedCards++) {
            for (int p = 0; p < 4; p++) {
                assertEquals(startScore + (p == 1 ? 1 + (addedCards / 10) : 0), (int) state.getGameScore(p));
            }
            state.addCard(CardType.COPPER, 1, DeckType.DISCARD);
        }
    }

    @Test
    public void twoGardens() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.GARDENS, 1, DeckType.DISCARD);
        state.addCard(CardType.GARDENS, 1, DeckType.DISCARD);
        state.getDeck(DeckType.HAND, 1).remove(DominionCard.create(CardType.COPPER));
        state.getDeck(DeckType.HAND, 1).remove(DominionCard.create(CardType.COPPER));
        int startScore = 3;
        for (int addedCards = 0; addedCards < 33; addedCards++) {
            for (int p = 0; p < 4; p++) {
                assertEquals(startScore + (p == 1 ? 2 + (addedCards / 10) * 2 : 0), (int) state.getGameScore(p));
            }
            state.addCard(CardType.COPPER, 1, DeckType.DISCARD);
        }
    }

}
