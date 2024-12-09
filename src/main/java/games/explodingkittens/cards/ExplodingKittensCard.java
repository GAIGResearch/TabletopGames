package games.explodingkittens.cards;

import core.components.Card;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.actions.ChoiceOfCardToGive;
import org.apache.spark.sql.catalyst.plans.logical.BinaryCommand;

import java.util.function.BiConsumer;


public class ExplodingKittensCard extends Card {

    public static int SEE_THE_FUTURE_CARDS = 3;
    public static int ADDITIONAL_TURNS = 2;

    public enum CardType {
        EXPLODING_KITTEN(false),
        DEFUSE(false),
        NOPE(false),
        ATTACK(true,  (state, target) -> {
            int attackLevel = ADDITIONAL_TURNS;
            state.setSkip(true);
            if (state.getCurrentPlayerTurnsLeft() > 1) {
                attackLevel += state.getCurrentPlayerTurnsLeft();
                state.setCurrentPlayerTurnsLeft(1);
            }
            state.setNextAttackLevel(attackLevel);
        }),
        SKIP(true, (state, target) -> {
            state.setSkip(true);
        }),
        FAVOR(true, (state, target) -> {
            int cards = state.getPlayerHand(target).getSize();
            if (cards > 0) { // edge cases make zero cards possible
                state.setActionInProgress(new ChoiceOfCardToGive(target, state.getCurrentPlayer()));
            }
        }),
        SHUFFLE(true, (state, target) -> {
            state.getDrawPile().shuffle(state.getRnd());
        }),
        SEETHEFUTURE(true, (state, target) -> {
            // we set the visibility of the top 3 cards of the draw pile to the current player
            int cardsToSee = Math.min(SEE_THE_FUTURE_CARDS, state.getDrawPile().getSize());
            for (int i = 0; i < cardsToSee; i++) {
                state.getDrawPile().setVisibilityOfComponent(i, state.getCurrentPlayer(), true);
            }
        }),
        TACOCAT(true, true),
        MELONCAT(true, true),
        FURRYCAT(true, true),
        BEARDCAT(true, true),
        RAINBOWCAT(true, true);

        public final boolean nopeable;
        public final boolean catCard;
        private final BiConsumer<ExplodingKittensGameState, Integer> lambda;

        CardType(boolean nope) {
            this(nope, (state, target) -> {
                throw new AssertionError("Card should not be executed directly");
            });
        }

        CardType (boolean nope, boolean catCard) {
            this.nopeable = nope;
            this.catCard = catCard;
            // lambda for all cat cards; need to play a pair to take a random card from the target
            this.lambda = (state, target) -> {
                if (state.getPlayerHand(target).getSize() > 0) {
                    int index = state.getRnd().nextInt(state.getPlayerHand(target).getSize());
                    ExplodingKittensCard card = state.getPlayerHand(target).get(index);
                    state.getPlayerHand(target).remove(card);
                    state.getPlayerHand(state.getCurrentPlayer()).add(card);
                }
            };
        }

        CardType(boolean nope, BiConsumer<ExplodingKittensGameState, Integer> lambda) {
            this.nopeable = nope;
            this.catCard = false;
            this.lambda = lambda;
        }

        public void execute(ExplodingKittensGameState state, int target) {
            lambda.accept(state, target);
        }
    }

    public CardType cardType;

    public ExplodingKittensCard(CardType cardType) {
        super(cardType.toString());
        this.cardType = cardType;
    }

    public ExplodingKittensCard(CardType cardType, int ID) {
        super(cardType.toString(), ID);
        this.cardType = cardType;
    }

    @Override
    public Card copy() {
        return this; // immutable
    }

    @Override
    public String toString() {
        return cardType.name();
    }
}
