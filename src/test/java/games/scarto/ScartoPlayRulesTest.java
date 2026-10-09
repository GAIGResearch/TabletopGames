package games.scarto;

import core.actions.AbstractAction;
import core.components.TarotCard;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static core.components.TarotCard.*;
import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Which cards may be played (ScartoUtils.PLAY_RULE) and the actions offered for them. Legal plays are in hand order.
 */
public class ScartoPlayRulesTest {

    ScartoForwardModel fm;
    ScartoGameState state;

    @Before
    public void setup() {
        fm = new ScartoForwardModel();
        state = newState(17, fm);
    }

    static List<TarotCard> legal(List<TarotCard> hand, TarotCard... trick) {
        return ScartoUtils.PLAY_RULE.legalPlays(hand, scartoTrick(0, trick));
    }

    // ---- PLAY_RULE ----

    @Test
    public void anyCardMayBeLed() {
        List<TarotCard> hand = List.of(cup(2), sword(KING), fool(), trump(4), coin(10));
        assertEquals(hand, legal(hand));
    }

    @Test
    public void aPlayerWhoCanFollowMustFollowButMayPlayTheFool() {
        // swords led; the hand holds swords -> the swords and the Fool, not the cup or the trump
        List<TarotCard> hand = List.of(cup(2), sword(KING), fool(), trump(4), sword(1));
        assertEquals(List.of(sword(KING), fool(), sword(1)), legal(hand, sword(5)));
    }

    @Test
    public void aPlayerWhoCanFollowWithoutTheFoolPlaysOnlyTheSuitLed() {
        List<TarotCard> hand = List.of(coin(QUEEN), cup(2), trump(ANGEL), cup(KNAVE));
        assertEquals(List.of(cup(2), cup(KNAVE)), legal(hand, cup(7)));
    }

    @Test
    public void aPlayerWhoCannotFollowMustTrumpButMayPlayTheFool() {
        // batons led; no batons in hand -> the trumps and the Fool
        List<TarotCard> hand = List.of(cup(2), trump(9), sword(KING), fool(), trump(WORLD));
        assertEquals(List.of(trump(9), fool(), trump(WORLD)), legal(hand, baton(5)));
    }

    @Test
    public void aPlayerWithNeitherTheSuitLedNorATrumpMayPlayAnything() {
        List<TarotCard> hand = List.of(cup(2), sword(KING), fool(), coin(7));
        assertEquals(hand, legal(hand, baton(5)));
    }

    @Test
    public void whenATrumpIsLedAPlayerMustPlayATrumpOrTheFool() {
        List<TarotCard> hand = List.of(cup(2), trump(3), fool(), trump(ANGEL), sword(KING));
        assertEquals(List.of(trump(3), fool(), trump(ANGEL)), legal(hand, trump(7)));
    }

    @Test
    public void whenOnlyTheFoolHasBeenPlayedAnyCardMayBePlayed() {
        List<TarotCard> hand = List.of(cup(2), sword(KING), trump(4), coin(10));
        assertEquals(hand, legal(hand, fool()));
    }

    @Test
    public void afterAFoolLeadTheThirdPlayerFollowsTheSecondPlayersSuit() {
        // Fool then cup 3: cups are the suit to follow
        List<TarotCard> hand = List.of(sword(2), cup(5), trump(4), cup(KING));
        assertEquals(List.of(cup(5), cup(KING)), legal(hand, fool(), cup(3)));
    }

    // ---- available actions ----

    @Test
    public void theActionsArePlayCardsOfTheLegalCardsInHandOrder() {
        // player 0 led cup 4; player 1 holds cups -> cup King, the Fool, cup 10
        giveHand(state, 1, sword(2), cup(KING), fool(), cup(10), trump(3));
        arrangeTrick(state, 0, cup(4));
        assertEquals(1, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(playCards(cup(KING), fool(), cup(10)), actions);
    }

    @Test
    public void aLeaderHasOnePlayCardForEachCardInHand() {
        giveHand(state, 2, sword(2), fool(), trump(ANGEL));
        arrangeTrick(state, 2);
        assertEquals(playCards(sword(2), fool(), trump(ANGEL)), fm.computeAvailableActions(state));
    }

    @Test
    public void atTheStartOfTheGamePlayerZeroMayLeadAnyOfTheirTwentyFiveCards() {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(25, actions.size());
        List<TarotCard> played = actions.stream().map(a -> ((PlayCard<?>) a).card).map(c -> (TarotCard) c).toList();
        assertEquals(state.getPlayerHand(0).getComponents(), played);
    }
}
