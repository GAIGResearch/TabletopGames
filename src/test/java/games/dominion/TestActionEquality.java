package games.dominion;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.dominion.actions.BuyCard;
import games.dominion.actions.GainCard;
import games.dominion.cards.CardType;
import org.junit.Test;
import players.rhea.RHEAParams;
import players.rhea.RHEAPlayer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Buying and gaining a card are different moves (a buy uses up a buy and coins), so they must not be equal,
 * or an agent that matches stored actions against the legal ones can execute a buy where only a gain is legal.
 */
public class TestActionEquality {

    @Test
    public void buyCardIsNotEqualToGainCard() {
        BuyCard buy = new BuyCard(CardType.SILVER, 0);
        GainCard gain = new GainCard(CardType.SILVER, 0);
        assertNotEquals(buy, gain);
        assertNotEquals(gain, buy);
    }

    @Test
    public void legalGainDoesNotContainBuy() {
        List<GainCard> legal = List.of(new GainCard(CardType.SILVER, 0));
        assertFalse(legal.contains(new BuyCard(CardType.SILVER, 0)));
    }

    @Test
    public void sameClassAndFieldsAreStillEqual() {
        assertEquals(new BuyCard(CardType.SILVER, 0), new BuyCard(CardType.SILVER, 0));
        assertEquals(new BuyCard(CardType.SILVER, 0).hashCode(), new BuyCard(CardType.SILVER, 0).hashCode());
        assertEquals(new GainCard(CardType.SILVER, 0), new GainCard(CardType.SILVER, 0));
        assertNotEquals(new GainCard(CardType.SILVER, 0), new GainCard(CardType.GOLD, 0));
    }

    @Test
    public void rheaCompletesDominionGames() {
        for (int nPlayers : new int[]{2, 4}) {
            for (long seed = 1; seed <= 3; seed++) {
                Game game = GameType.Dominion.createGameInstance(nPlayers, seed);
                List<AbstractPlayer> players = new ArrayList<>();
                for (int p = 0; p < nPlayers; p++) {
                    RHEAParams params = new RHEAParams();
                    params.budget = 200;
                    params.setRandomSeed(seed * 10 + p);
                    players.add(new RHEAPlayer(params));
                }
                game.reset(players);
                game.run();
                assertFalse(game.getGameState().isNotTerminal());
            }
        }
    }
}
