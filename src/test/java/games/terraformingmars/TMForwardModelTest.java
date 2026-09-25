package games.terraformingmars;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.terraformingmars.actions.*;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.effects.PlaceTileEffect;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TMForwardModelTest {

    TMGameState state;
    TMForwardModel fm;

    @Before
    public void setup() {
        Game game = GameType.TerraformingMars.createGameInstance(4, 42);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        state = (TMGameState) game.getGameState();
        fm = (TMForwardModel) game.getForwardModel();
    }

    private List<TMCard> allCards() {
        List<TMCard> cards = new ArrayList<>(state.getProjectCards().getComponents());
        cards.addAll(state.getCorpCards().getComponents());
        for (int p = 0; p < state.getNPlayers(); p++)
            cards.addAll(state.getPlayerCardChoice()[p].getComponents());
        return cards;
    }

    @Test
    public void cardActionsUseOneActionAndTheirPartsAreFree() {
        for (TMCard card : allCards()) {
            for (TMAction a : card.actions) {
                assertFalse(card.getComponentName() + " action should use up an action", a.freeActionPoint);
                if (a instanceof CompoundAction ca) {
                    for (TMAction part : ca.actions)
                        assertTrue(card.getComponentName() + " compound part should be free", part.freeActionPoint);
                }
            }
        }
    }

    @Test
    public void actionsFromCardDataAreEqualToTheirCopies() {
        // Card data loads with null requirements, while copies have an empty set
        for (TMCard card : allCards()) {
            for (TMAction a : card.immediateEffects) {
                TMAction copy = a.copy();
                assertEquals(card.getComponentName(), a, copy);
                assertEquals(card.getComponentName(), a.hashCode(), copy.hashCode());
            }
        }
    }

    @Test
    public void placeTileEffectsOnlyTriggerOnActualPlacement() {
        PlaceTileEffect effect = new PlaceTileEffect(false, new TMAction(0, true), true, TMTypes.Tile.City, null);
        PlaceTile request = new PlaceTile(0, TMTypes.Tile.City, TMTypes.MapTileType.Ground, true);
        assertFalse(effect.canExecute(state, request, 0));
        assertFalse(effect.canExecute(state, new PayForAction(0, request), 0));
    }

    @Test
    public void randomGamesRunToCompletionWithCopyableActions() {
        Random rnd = new Random(1);
        for (int g = 0; g < 3; g++) {
            Game game = GameType.TerraformingMars.createGameInstance(4, rnd.nextLong());
            List<AbstractPlayer> players = new ArrayList<>();
            for (int p = 0; p < 4; p++) players.add(new RandomPlayer(new Random(rnd.nextLong())));
            game.reset(players);
            AbstractForwardModel forwardModel = game.getForwardModel();
            TMGameState gs = (TMGameState) game.getGameState();
            int decisions = 0;
            while (gs.isNotTerminal()) {
                List<AbstractAction> actions = forwardModel.computeAvailableActions(gs);
                assertFalse("No actions available at decision " + decisions + " with " + gs.getActionsInProgress(), actions.isEmpty());
                for (AbstractAction a : actions)
                    assertEquals(a, a.copy());
                // Execute the listed action itself (as RandomPlayer does), so that aliasing with the state is exercised
                forwardModel.next(gs, actions.get(rnd.nextInt(actions.size())));
                decisions++;
                assertTrue("Game did not finish", decisions < 5000);
            }
            assertTrue(gs.getGeneration() > 1);
        }
    }
}
