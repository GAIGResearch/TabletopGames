package games.diamant;

import core.Game;
import games.GameType;
import games.diamant.cards.DiamantCard;
import games.diamant.cards.DiamantCard.DiamantCardType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DiamantRelicTest {

    @Test
    public void testRelicVariantSetup() {
        // Set up the game with relicVariant enabled
        DiamantParameters params = new DiamantParameters();
        params.setParameterValue("relicVariant", true);
        Game game = new Game(GameType.Diamant, new DiamantForwardModel(), new DiamantGameState(params, 4));

        DiamantGameState state = (DiamantGameState) game.getGameState();

        // Check that there is exactly one Relic card in the deck
        long relicCountInDeck = state.getMainDeck().stream()
                .filter(card -> card.getCardType() == DiamantCardType.Relic)
                .count();
        assertEquals(1, relicCountInDeck);

        // Check that there are four relics in the relic pile
        assertEquals(4, state.getRelicDeck().getSize());

        // Check that the Relic card in the deck has a value of 5
        DiamantCard relicCard = state.getMainDeck().stream()
                .filter(card -> card.getCardType() == DiamantCardType.Relic)
                .findFirst()
                .orElse(null);
        assertEquals(5, relicCard.getValue());
    }
}