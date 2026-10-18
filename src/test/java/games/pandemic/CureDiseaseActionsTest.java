package games.pandemic;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import core.properties.PropertyColor;
import games.GameType;
import games.pandemic.actions.CureDisease;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.colorHash;
import static core.CoreConstants.playerHandHash;
import static games.pandemic.PandemicConstants.playerDeckHash;
import static org.junit.Assert.*;

public class CureDiseaseActionsTest {

    PandemicGameState state;
    Deck<Card> hand;
    List<Integer> blueCards;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        PandemicParameters params = (PandemicParameters) GameType.Pandemic.createParameters(34);
        params.setParameterValue("player0Role", "Medic");
        params.setParameterValue("player1Role", "Medic");
        Game game = GameType.Pandemic.createGameInstance(2, 34, params);
        game.reset(IntStream.range(0, 2).mapToObj(p -> (AbstractPlayer) new RandomPlayer()).toList());
        state = (PandemicGameState) game.getGameState();

        // Give the acting player (in Atlanta, which has a research station) six blue city cards and nothing else
        hand = (Deck<Card>) state.getComponentActingPlayer(playerHandHash);
        Deck<Card> playerDeck = (Deck<Card>) state.getComponent(playerDeckHash);
        hand.clear();
        blueCards = new ArrayList<>();
        for (Card c : new ArrayList<>(playerDeck.getComponents())) {
            if (blueCards.size() < 6 && c.getProperty(colorHash) instanceof PropertyColor col && col.valueStr.equals("blue")) {
                playerDeck.remove(c);
                hand.add(c);
                blueCards.add(c.getComponentID());
            }
        }
        assertEquals(6, blueCards.size());
    }

    private List<CureDisease> cureActions() {
        List<CureDisease> retValue = new ArrayList<>();
        for (AbstractAction a : PandemicActionFactory.getPlayerActions(state))
            if (a instanceof CureDisease cure) retValue.add(cure);
        return retValue;
    }

    @Test
    public void oneCureActionPerChoiceOfCardsNeeded() {
        List<CureDisease> cures = cureActions();
        // choose 5 of the 6 blue cards
        assertEquals(6, cures.size());
        Set<Set<Integer>> distinct = new HashSet<>();
        for (CureDisease cure : cures) {
            assertEquals("blue", cure.getColor());
            assertEquals(5, cure.getCards().size());
            assertTrue(blueCards.containsAll(cure.getCards()));
            distinct.add(new HashSet<>(cure.getCards()));
        }
        assertEquals(6, distinct.size());
    }

    @Test
    public void curingDiscardsOnlyTheCardsUsed() {
        CureDisease cure = cureActions().get(0);
        cure.execute(state);
        assertEquals(1, hand.getSize());
        assertFalse(cure.getCards().contains(hand.get(0).getComponentID()));
    }
}
