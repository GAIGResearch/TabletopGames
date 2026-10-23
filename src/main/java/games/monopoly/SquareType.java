package games.monopoly;

import games.monopoly.actions.BuyDecision;
import games.monopoly.actions.IncomeTaxChoice;
import games.monopoly.components.MonopolyCard;

/**
 * The kinds of square on the Monopoly board. INCOME_TAX is a tax square whose tax may instead be paid as
 * MonopolyParameters.incomeTaxPercent of the player's total worth.
 */
public enum SquareType {
    GO, STREET, STATION, UTILITY, CHANCE, COMMUNITY_CHEST, TAX, INCOME_TAX, JAIL, FREE_PARKING, GO_TO_JAIL;

    /**
     * Whether a square of this type can be bought and owned.
     */
    public boolean isProperty() {
        return this == STREET || this == STATION || this == UTILITY;
    }

    /**
     * Carries out what happens when the player's token stops on the square (of this type).
     */
    public void landOn(MonopolyGameState state, int player, MonopolySquare square) {
        if (isProperty()) {
            int holder = state.getOwner(square);
            // an unowned property may be bought; another player's charges rent
            if (holder == -1)
                state.setActionInProgress(new BuyDecision(player, square));
            else if (holder != player)
                state.pay(player, holder, state.getRent(square));
            return;
        }
        switch (this) {
            case TAX -> state.pay(player, -1, square.tax());
            case INCOME_TAX -> {
                if (((MonopolyParameters) state.getGameParameters()).incomeTaxPercent > 0)
                    state.setActionInProgress(new IncomeTaxChoice(player, square));
                else
                    state.pay(player, -1, square.tax());
            }
            case GO_TO_JAIL -> state.sendToJail(player);
            case CHANCE, COMMUNITY_CHEST -> {
                MonopolyCard card = state.drawCard(this == CHANCE ? MonopolyCard.Pile.CHANCE : MonopolyCard.Pile.COMMUNITY_CHEST);
                card.effect.apply(state, player, card);
            }
            default -> {
            }
        }
    }
}
