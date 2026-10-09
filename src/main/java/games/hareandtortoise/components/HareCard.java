package games.hareandtortoise.components;

import core.components.Card;
import games.hareandtortoise.HareAndTortoiseGameState;
import games.hareandtortoise.SquareType;
import games.hareandtortoise.actions.DrawOrDiscardCarrots;

import java.util.Objects;
import java.util.function.IntPredicate;

import static games.hareandtortoise.HareAndTortoiseParameters.BOARD;
import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;
import static games.hareandtortoise.HareAndTortoiseUtils.firstSquare;

/**
 * A hare card of the 1978 Ravensburger edition. Immutable: two cards of the same type are interchangeable.
 */
public class HareCard extends Card {

    public enum Type {
        FALL_BACK_ONE_POSITION("Fall back one position"),
        LAST_TURN_FREE("Your last turn costs nothing"),
        DRAW_OR_DISCARD("Either draw or discard carrots"),
        LEAP_AHEAD_ONE_POSITION("Leap ahead by one position"),
        NEXT_CARROT_SQUARE("Leap ahead to the next carrot square"),
        PREVIOUS_CARROT_SQUARE("Fall back to the previous carrot square"),
        ANOTHER_TURN("Have another turn"),
        MISS_A_TURN("Miss a turn"),
        CHEW_A_LETTUCE("Chew a lettuce");

        public final String text;

        Type(String text) {
            this.text = text;
        }

        /**
         * Carries out the card for the player, who has just paid the given number of carrots to move onto a hare
         * square.
         */
        public void apply(HareAndTortoiseGameState state, int player, int carrotsPaid) {
            int square = state.getSquare(player);
            IntPredicate freeCarrot = s -> BOARD[s] == SquareType.CARROT && !state.isOccupied(s);
            switch (this) {
                case FALL_BACK_ONE_POSITION -> {
                    int behind = 0;
                    for (int p = 0; p < state.getNPlayers(); p++)
                        if (p != player && !state.isHome(p) && state.getSquare(p) < square)
                            behind = Math.max(behind, state.getSquare(p));
                    if (behind > 0)
                        moveRunner(state, player, firstSquare(behind - 1, -1, s -> state.canLandOn(player, s)));
                }
                case LAST_TURN_FREE -> state.addCarrots(player, carrotsPaid);
                case DRAW_OR_DISCARD -> state.setActionInProgress(new DrawOrDiscardCarrots(player));
                case LEAP_AHEAD_ONE_POSITION -> {
                    int ahead = HOME_SQUARE;
                    for (int p = 0; p < state.getNPlayers(); p++)
                        if (p != player && !state.isHome(p) && state.getSquare(p) > square)
                            ahead = Math.min(ahead, state.getSquare(p));
                    moveRunner(state, player, firstSquare(ahead + 1, 1,
                            s -> state.canLandOn(player, s) && BOARD[s] != SquareType.TORTOISE));
                }
                case NEXT_CARROT_SQUARE -> moveRunner(state, player, firstSquare(square + 1, 1, freeCarrot));
                case PREVIOUS_CARROT_SQUARE -> moveRunner(state, player, firstSquare(square - 1, -1, freeCarrot));
                case MISS_A_TURN -> state.setMissNextTurn(player, true);
                case CHEW_A_LETTUCE -> {
                    if (state.getLettuces(player) > 0)
                        state.chewLettuce(player);
                }
                case ANOTHER_TURN -> state.setAnotherTurn(true);
            }
        }

        /**
         * Moves the runner free of charge. Square 0 means the card found no square and the runner stays where it is.
         */
        private void moveRunner(HareAndTortoiseGameState state, int player, int square) {
            if (square == 0) return;
            state.moveRunner(player, square, 0);
            // a hare square reached this way does not draw another card
            if (BOARD[square] == SquareType.LETTUCE)
                state.setLettuceToChew(player, true);
        }
    }

    public final Type type;

    public HareCard(Type type) {
        super(type.text);
        this.type = type;
    }

    @Override
    public HareCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HareCard other && other.type == type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type) + 7219;
    }

    @Override
    public String toString() {
        return type.text;
    }
}
