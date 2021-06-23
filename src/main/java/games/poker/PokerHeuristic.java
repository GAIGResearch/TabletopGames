package games.poker;

import core.AbstractGameState;
import core.components.FrenchCard;
import core.interfaces.IStateHeuristic;
import java.util.List;

import static core.components.FrenchCard.Suite.*;

public class PokerHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        PokerGameState pgs = (PokerGameState) gs;

        List<FrenchCard> localPlayerHand = pgs.getPlayerDecks().get(playerId).getComponents();
        int value = 0;

        for (FrenchCard frenchCard : localPlayerHand) {
            FrenchCard.FrenchCardType player = frenchCard.type;
            switch (frenchCard.type) { //should be the player's card instead instead of card.type, replace with the actual card from the player
                case Number:
                    value += frenchCard.number;
                    break;
                case Jack:
                    if (frenchCard.suite == Spades) {
                        value += 10;
                        break;
                    } else if (frenchCard.suite == Diamonds) {
                        value += 11;
                        break;
                    } else if (frenchCard.suite == Hearts) {
                        value += 12;
                        break;
                    } else if (frenchCard.suite == Clubs) {
                        value += 13;
                        break;
                    }
                    break;

                case Queen:
                    if (frenchCard.suite == Spades) {
                        value += 14;
                        break;
                    } else if (frenchCard.suite == Diamonds) {
                        value += 15;
                        break;
                    } else if (frenchCard.suite == Hearts) {
                        value += 16;
                        break;
                    } else if (frenchCard.suite == Clubs) {
                        value += 17;
                        break;
                    }
                    break;
                case King:
                    if (frenchCard.suite == Spades) {
                        value += 18;
                        break;
                    } else if (frenchCard.suite == Diamonds) {
                        value += 19;
                        break;
                    } else if (frenchCard.suite == Hearts) {
                        value += 20;
                        break;
                    } else if (frenchCard.suite == Clubs) {
                        value += 21;
                        break;
                    }
                    break;
                case Ace:
                    if (frenchCard.suite == Spades) {
                        value += 22;
                        break;
                    } else if (frenchCard.suite == Diamonds) {
                        value += 23;
                        break;
                    } else if (frenchCard.suite == Hearts) {
                        value += 24;
                        break;
                    } else if (frenchCard.suite == Clubs) {
                        value += 25;
                        break;
                    }
                    break;
            }
        }
        return value * 1.0 / (pgs.playerMoney[playerId].getValue() + 1);
    }

}
