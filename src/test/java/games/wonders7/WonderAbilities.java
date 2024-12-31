package games.wonders7;

import core.actions.AbstractAction;
import games.wonders7.actions.ChooseCard;
import games.wonders7.actions.PlayCard;
import games.wonders7.cards.Wonder7Board;
import games.wonders7.cards.Wonder7Card;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.wonders7.cards.Wonder7Card.CardType.*;
import static games.wonders7.cards.Wonder7Card.CardType.Temple;
import static org.junit.Assert.*;

public class WonderAbilities {

    Wonders7ForwardModel fm = new Wonders7ForwardModel();
    Wonders7GameParameters params;
    Wonders7GameState state;

    @Before
    public void setup() {
        params = new Wonders7GameParameters();
        params.setRandomSeed(4902);
        state = new Wonders7GameState(params, 4);
        fm.setup(state);
    }

    @Test
    public void olympiaLevel2BuildsFirstCardOfColourForFree() {
        state.playerWonderBoard[0] = new Wonder7Board(Wonder7Board.Wonder.TheStatueOfZeusInOlympia);
        state.getPlayerHand(0).add(Wonder7Card.factory(Palace));
        state.getPlayerHand(0).add(Wonder7Card.factory(University));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, false))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, false))));

        state.getPlayerWonderBoard(0).changeStage();
        actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, true))));

        state.getPlayerWonderBoard(0).changeStage();
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new ChooseCard(new PlayCard(0, Palace, true))));
        assertTrue(actions.contains(new ChooseCard(new PlayCard(0, University, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, false))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, false))));

        state.getPlayedCards(0).add(Wonder7Card.factory(Altar));
        actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, true))));
        assertTrue(actions.contains(new ChooseCard(new PlayCard(0, University, true))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, Palace, false))));
        assertFalse(actions.contains(new ChooseCard(new PlayCard(0, University, false))));
    }

    @Test
    public void halicarnassusLevel2BuildsForFreeFromDiscardPileAtEndOfAge() {
        state.playerWonderBoard[1] = new Wonder7Board(Wonder7Board.Wonder.TheMausoleumOfHalicarnassus);
        // at the end of the Age (and only then), if the second stage is built then the player can build a card from the discard pile for free
        fail("Not implemented");
    }

    @Test
    public void halicarnassusLevel1DoesNotBuildForFreeFromDiscardPileAtEndOfAge() {
        state.playerWonderBoard[1] = new Wonder7Board(Wonder7Board.Wonder.TheMausoleumOfHalicarnassus);
        // at the end of the Age (and only then), if the second stage is built then the player can build a card from the discard pile for free
        fail("Not implemented");
    }

}
