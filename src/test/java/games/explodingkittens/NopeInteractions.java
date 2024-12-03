package games.explodingkittens;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static games.explodingkittens.cards.ExplodingKittensCard.CardType.*;
import static games.explodingkittens.cards.ExplodingKittensCard.CardType.NOPE;
import static org.junit.Assert.fail;

public class NopeInteractions {


    ExplodingKittensGameState state;
    ExplodingKittensForwardModel fm;
    ExplodingKittensParameters params;
    Random rnd = new Random(4033);

    @Before
    public void init() {
        params = new ExplodingKittensParameters();
        // this removes any cards which have extra action decisions to make
        params.cardCounts.put(ATTACK, 0);
        params.cardCounts.put(SKIP, 0);
        params.cardCounts.put(FAVOR, 0);
        state = new ExplodingKittensGameState(params, 4);
        fm = new ExplodingKittensForwardModel();
        fm.setup(state);
    }

    @Test
    public void doNotInterruptSingleCatCard() {
        fail("Not implemented");
    }

    @Test
    public void interruptOptionForNopePlayers() {
        fail("Not implemented");
    }

    @Test
    public void nopingACardMeansItHasNoEffect() {
        fail("Not implemented");
        // and also that Nope card is discarded
    }

    @Test
    public void nopingANopeCardMeansCardHasEffect() {
        fail("Not implemented");
    }

    @Test
    public void nopingThreeFoldRecursionHasNoEffect() {
        fail("Not implemented");
    }

}
