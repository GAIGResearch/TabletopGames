package games.monopoly.components;

import core.components.Card;
import games.monopoly.MonopolyBoard;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolyParameters;
import games.monopoly.MonopolySquare;
import games.monopoly.actions.PayOrChance;

import java.util.Objects;

/**
 * A Chance or Community Chest card, loaded from the board file (MonopolyBoard). Shared between copies: its fields
 * are final, and its ownerId (set by Deck) is never read.
 * <ul>
 *     <li>effect - what the card does</li>
 *     <li>amount - the money collected or paid; the spaces to go back (GO_BACK); the cost per house (REPAIRS)</li>
 *     <li>hotelAmount - the cost per hotel (REPAIRS), 0 otherwise</li>
 *     <li>target - the square moved to (ADVANCE, GO_BACK_TO), null otherwise</li>
 * </ul>
 */
public class MonopolyCard extends Card {

    public enum Pile {CHANCE, COMMUNITY_CHEST}

    public enum Effect {
        /** move forward to the target square, collecting the GO salary on passing (or landing on) GO */
        ADVANCE,
        /** move backward to the target square, without collecting the GO salary */
        GO_BACK_TO,
        /** move back amount squares */
        GO_BACK,
        GO_TO_JAIL,
        COLLECT,
        PAY,
        /** collect amount from each other player */
        COLLECT_FROM_EACH,
        /** pay amount for each house and hotelAmount for each hotel */
        REPAIRS,
        /** kept until used, then returned to the bottom of its pile */
        GET_OUT_OF_JAIL,
        /** pay amount, or draw a Chance card instead */
        PAY_OR_CHANCE;

        /**
         * Carries out the card (of this effect), drawn by the player.
         */
        public void apply(MonopolyGameState state, int player, MonopolyCard card) {
            MonopolyBoard board = state.getBoard();
            switch (this) {
                case ADVANCE -> {
                    int from = state.getPosition(player).index();
                    int to = board.square(card.target).index();
                    state.moveForward(player, Math.floorMod(to - from, board.nSquares()));
                }
                case GO_BACK_TO -> state.moveBackTo(player, board.square(card.target));
                case GO_BACK -> {
                    int to = Math.floorMod(state.getPosition(player).index() - card.amount, board.nSquares());
                    state.moveBackTo(player, board.square(to));
                }
                case GO_TO_JAIL -> state.sendToJail(player);
                case COLLECT -> state.setCash(player, state.getCash(player) + card.amount);
                case PAY -> state.pay(player, -1, card.amount);
                case COLLECT_FROM_EACH -> {
                    for (int p = 0; p < state.getNPlayers(); p++)
                        if (p != player && !state.isBankrupt(p))
                            state.pay(p, player, card.amount);
                }
                case REPAIRS -> {
                    int cost = 0;
                    for (MonopolySquare s : state.getProperties(player)) {
                        int n = state.getBuildings(s);
                        cost += n == MonopolyParameters.HOTEL ? card.hotelAmount : n * card.amount;
                    }
                    state.pay(player, -1, cost);
                }
                case GET_OUT_OF_JAIL -> state.keepJailCard(player, card);
                case PAY_OR_CHANCE -> state.setActionInProgress(new PayOrChance(player, card.amount));
            }
        }
    }

    public final Pile pile;
    public final String text;
    public final Effect effect;
    public final int amount;
    public final int hotelAmount;
    public final String target;

    public MonopolyCard(Pile pile, String text, Effect effect, int amount, int hotelAmount, String target) {
        super(text);
        this.pile = pile;
        this.text = text;
        this.effect = effect;
        this.amount = amount;
        this.hotelAmount = hotelAmount;
        this.target = target;
    }

    @Override
    public MonopolyCard copy() {
        return this;
    }

    /**
     * By pile and text: no pile holds two cards with the same text.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof MonopolyCard other && other.pile == pile && other.text.equals(text);
    }

    @Override
    public int hashCode() {
        // the pile by ordinal, as an enum's own hash differs from one run to the next
        return Objects.hash(pile.ordinal(), text) + 6311;
    }

    @Override
    public String toString() {
        return text;
    }
}
