package games.diamant;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.diamant.actions.ContinueInCave;
import games.diamant.actions.ExitFromCave;
import games.diamant.actions.OutOfCave;
import games.diamant.cards.DiamantCard;
import games.diamant.cards.DiamantCard.DiamantCardType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiamantRelicTest {

    @Test
    public void testRelicVariantSetup() {
        // Set up the game with relicVariant enabled
        DiamantParameters params = new DiamantParameters();
        params.setParameterValue("relicVariant", true);
        Game game = new Game(GameType.Diamant, new DiamantForwardModel(), new DiamantGameState(params, 4));
        DiamantGameState state = (DiamantGameState) game.getGameState();

        removeRelicsFromPath(state);

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

    @Test
    public void testRelicPickupAndNextCave() {
        // Set up the game with relicVariant enabled
        DiamantParameters params = new DiamantParameters();
        params.setParameterValue("relicVariant", true);
        Game game = new Game(GameType.Diamant, new DiamantForwardModel(), new DiamantGameState(params, 4));
        DiamantGameState state = (DiamantGameState) game.getGameState();
        removeRelicsFromPath(state);

        // Place a treasure as the first card, and a relic as the second card in the path
        placeRelicAsSecondPathCard(state, 5);

        // First move: 3 exit, 1 continues
        executePlayerActions(game, state, new boolean[]{false, false, false, true});

        // After first move, 3 players have left, 1 remains
        assertEquals(1, state.getNPlayersInCave());

        // check relic is in path
        assertEquals(1, state.getPath().stream().
                filter(c -> c.getCardType() == DiamantCardType.Relic)
                .count());

        // Next move: last player exits
        int currentScore = state.hands.get(3).getValue(); // Player 3 is the last one in the cave
        int expectedScoreIncrease = 5 + state.gemsOnPath.stream().mapToInt(i -> i).sum(); // 5 from relic, plus gems on path
        executePlayerActions(game, state, new boolean[]{false, false, false, false});

        // After this, all players have left, cave ends, new cave is prepared
        // Player 3 should have picked up the relic (value 5)
        assertEquals(currentScore + expectedScoreIncrease, state.getTreasureChests().get(3).getValue());

        removeRelicsFromPath(state);
        // Prepare for the next cave: there should be a relic of value 7 in the main deck
        assertRelicsInMainDeck(state, new int[]{7});
    }

    @Test
    public void testRelicNotPickedUpWithTwoPlayers() {
        // Set up the game with relicVariant enabled
        DiamantParameters params = new DiamantParameters();
        params.setParameterValue("relicVariant", true);
        Game game = new Game(GameType.Diamant, new DiamantForwardModel(), new DiamantGameState(params, 4));
        DiamantGameState state = (DiamantGameState) game.getGameState();
        removeRelicsFromPath(state);

        // Place relic as second card in path (reuse helper)
        placeRelicAsSecondPathCard(state, 5);

        // First move: 2 exit, 2 continue
        executePlayerActions(game, state, new boolean[]{false, false, true, true});

        // After first move, 2 players remain
        assertEquals(2, state.getNPlayersInCave());

        int[] currentScores = new int[4];
        int expectedIncrease = state.gemsOnPath.stream().mapToInt(g -> g / 2).sum();
        for (int i = 0; i < 4; i++) {
            currentScores[i] = state.hands.get(i).getValue();
        }

        // check relic is still in path
        assertEquals(1, state.getPath().stream().
                filter(c -> c.getCardType() == DiamantCardType.Relic)
                .count());

        // Next move: both remaining players exit
        executePlayerActions(game, state, new boolean[]{false, false, false, false});

        // No player should have received the relic bonus
        for (int i = 2; i < 4; i++) {
            assertEquals(currentScores[i] + expectedIncrease, state.getTreasureChests().get(i).getValue());
        }

        // Prepare for the next cave: there should be a relic of value 7 in the main deck
        assertRelicsInMainDeck(state, new int[]{7});
    }

    @Test
    public void testNoRelicDrawnBothRelicsInNextCave() {
        // Set up the game with relicVariant enabled
        DiamantParameters params = new DiamantParameters();
        params.setParameterValue("relicVariant", true);
        Game game = new Game(GameType.Diamant, new DiamantForwardModel(), new DiamantGameState(params, 4));
        DiamantGameState state = (DiamantGameState) game.getGameState();
        removeRelicsFromPath(state);

        // Do not place any relic in the path; just play a normal round
        // First move: all players exit
        executePlayerActions(game, state, new boolean[]{false, false, false, false});

        // After this, all players have left, cave ends, new cave is prepared
        // Both relics (5 and 7) should be present in the main deck for the new cave
        removeRelicsFromPath(state);
        assertRelicsInMainDeck(state, new int[]{5, 7});
    }

    // --- Helper for both tests ---
    private void placeRelicAsSecondPathCard(DiamantGameState state, int relicValue) {
        // Find the relic in the main deck, extract it, and put it at the front
        DiamantCard relicCard = state.getMainDeck().stream()
                .filter(card -> card.getCardType() == DiamantCardType.Relic && card.getValue() == relicValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected relic card not found in main deck"));
        state.getMainDeck().remove(relicCard);
        state.getPath().add(relicCard); // Add relic to the path
    }
    private void removeRelicsFromPath(DiamantGameState state) {
        // if there are any relics or hazards on the path, remove them and replace with a zero gem card
        for (int i = 0; i < state.getPath().getSize(); i++) {
            DiamantCard card = state.getPath().get(i);
            if (card.getCardType() == DiamantCardType.Relic || card.getCardType() == DiamantCardType.Hazard) {
                state.getPath().remove(i);
                state.getPath().add(new DiamantCard(DiamantCardType.Treasure, null, 0), i);
                state.mainDeck.add(card);
            }
        }

    }

    // Helper: Check that the main deck contains exactly the relics with the given values
    private void assertRelicsInMainDeck(DiamantGameState state, int[] expectedRelicValues) {
        java.util.List<Integer> relicsInDeck = state.getMainDeck().stream()
                .filter(card -> card.getCardType() == DiamantCardType.Relic)
                .map(DiamantCard::getValue)
                .sorted()
                .toList();
        int[] actual = relicsInDeck.stream().mapToInt(Integer::intValue).toArray();
        int[] expected = expectedRelicValues.clone();
        java.util.Arrays.sort(expected);
        assertEquals(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual));
    }

    // Helper: Execute player actions for a round.
    // For each player, true = stay (ContinueInCave), false = leave (ExitFromCave), null = OutOfCave
    private void executePlayerActions(Game game, DiamantGameState state, boolean[] stayOrLeave) {
        AbstractAction[] actions = new AbstractAction[stayOrLeave.length];
        for (int i = 0; i < stayOrLeave.length; i++) {
            if (!state.playerInCave.get(i)) {
                actions[i] = new OutOfCave();
            } else if (stayOrLeave[i]) {
                actions[i] = new ContinueInCave();
            } else {
                actions[i] = new ExitFromCave();
            }
        }
        for (int i = 0; i < stayOrLeave.length; i++) {
            game.getForwardModel().next(state, actions[i]);
        }
    }
}