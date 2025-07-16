package games.diamant;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.diamant.actions.ContinueInCave;
import games.diamant.actions.ExitFromCave;
import games.diamant.cards.DiamantCard;
import games.diamant.cards.DiamantCard.HazardType;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static games.diamant.cards.DiamantCard.DiamantCardType.Hazard;
import static org.junit.Assert.assertEquals;

public class DiamantExitTest {

    DiamantForwardModel fm = new DiamantForwardModel();

    @Test
    public void testGemsOnNewCardDivided() {
        // Setup game for 4 players
        for (int seed = 0; seed < 20; seed++) {

            DiamantParameters params = new DiamantParameters();
            params.setRandomSeed(seed * 37);
            Game game = new Game(GameType.Diamant, fm, new DiamantGameState(params, 4));

            DiamantGameState state = (DiamantGameState) game.getGameState();
            state.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Treasure, HazardType.None, 5));

            // All players are in the cave at the start
            assertEquals(4, state.getNPlayersInCave());

            // with one cards added at the start of the game
            assertEquals(1, state.gemsOnPath.size());
            int firstCardResidue = state.gemsOnPath.get(0);
            int firstCardCollected = state.hands.get(0).getValue();

            // Prepare actions: players 0 and 1 exit, 2 and 3 continue
            AbstractAction[] actions = new AbstractAction[4];
            actions[0] = new ExitFromCave();
            actions[1] = new ExitFromCave();
            actions[2] = new ContinueInCave();
            actions[3] = new ContinueInCave();

            // Simulate all players submitting their actions
            for (int i = 0; i < 4; i++) {
                assertEquals(i, state.getCurrentPlayer());
                fm.next(state, actions[i]);
            }
            assertEquals(0, state.getCurrentPlayer());
            assertEquals(5, state.getPath().get(0).getValue());

            // After the move, 2 players should have left the cave, 2 remain
            assertEquals(2, state.playerInCave.stream().filter(b -> b).count());

            assertEquals(2, state.getNPlayersInCave());

            assertEquals(2, state.gemsOnPath.size());
            assertEquals(1, (int) state.gemsOnPath.get(1));
            assertEquals(0, state.hands.get(0).getValue());
            assertEquals(firstCardCollected + firstCardResidue / 2, state.treasureChests.get(0).getValue());
            assertEquals(0, state.hands.get(1).getValue());
            assertEquals(firstCardCollected + firstCardResidue / 2, state.treasureChests.get(1).getValue());
            assertEquals(2 + firstCardCollected, state.hands.get(2).getValue());
            assertEquals(2 + firstCardCollected, state.hands.get(3).getValue());
        }
    }

    @Test
    public void testTreasureDivisionOnFirstCardForMultipleSeeds() {
        for (int seed = 0; seed < 20; seed++) {
            DiamantParameters params = new DiamantParameters();
            params.setRandomSeed(seed * 31);
            Game game = new Game(GameType.Diamant, fm, new DiamantGameState(params, 4));

            DiamantGameState state = (DiamantGameState) game.getGameState();

            // There should be exactly one card in the path after setup
            assertEquals(1, state.getPath().getSize());
            System.out.println(state.getPath().get(0));

            DiamantCard firstCard = state.getPath().peek();
            int value = firstCard.getValue();

            if (firstCard.getCardType() == DiamantCard.DiamantCardType.Treasure) {
                int expectedPerPlayer = value / 4;
                int expectedRemainder = value % 4;
                System.out.println(expectedPerPlayer + " " + expectedRemainder);

                for (int p = 0; p < 4; p++) {
                    assertEquals(expectedPerPlayer, state.getHands().get(p).getValue());
                }
                assertEquals(1, state.gemsOnPath.size());
                assertEquals(expectedRemainder, (int) state.gemsOnPath.get(0));
            }
        }
    }

    @Test
    public void testHazardCardRemovedEachRound() {
        DiamantParameters params = new DiamantParameters();
        params.setRandomSeed(12345);
        Game game = new Game(GameType.Diamant, fm, new DiamantGameState(params, 4));
        DiamantGameState state = (DiamantGameState) game.getGameState();

        Map<HazardType, Long> initialHazardCounts = state.getNHazardCardsInMainDeck();
        Map<HazardType, Long> onPath = state.getHazardsOnPath();
        // combine these two maps
        initialHazardCounts = initialHazardCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + onPath.getOrDefault(e.getKey(), 0L)));

        int currentCave = state.nCave;

        while (state.isNotTerminal()) {
            // All players continue until the cave ends (by double hazard)
            DiamantCard lastCard;
            do {
                lastCard = state.mainDeck.peek();
                for (int i = 0; i < 4; i++) {
                    if (state.playerInCave.get(i)) {
                        fm.next(state, new ContinueInCave());
                    }
                }
            } while (currentCave == state.nCave);
            if (state.isNotTerminal()) {
                System.out.println(currentCave + " : " + lastCard.getHazardType());
                assertEquals(Hazard, lastCard.getCardType());
                Map<HazardType, Long> hazardCounts = state.getNHazardCardsInMainDeck();
                Map<HazardType, Long> onPath2 = state.getHazardsOnPath();
                // combine these two maps
                hazardCounts = hazardCounts.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + onPath2.getOrDefault(e.getKey(), 0L)));
                for (HazardType hazardType : HazardType.values()) {
                    if (hazardType == lastCard.getHazardType()) {
                        assertEquals(1, initialHazardCounts.get(lastCard.getHazardType()) - hazardCounts.get(lastCard.getHazardType()));                    }
                    else if (hazardType != HazardType.None) {
                        assertEquals(initialHazardCounts.get(hazardType), hazardCounts.get(hazardType));
                    }
                }
                currentCave++;
                initialHazardCounts = hazardCounts;
            }
        }
    }
}
