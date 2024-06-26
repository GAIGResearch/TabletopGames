package games.poker;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.FrenchCard;
import core.interfaces.*;
import evaluation.optimisation.TunableParameters;

import java.util.*;

import static core.components.FrenchCard.Suite.*;

public class PokerHeuristic extends TunableParameters implements IStateHeuristic {

    double FACTOR_MONEY = 0.8;
    double FACTOR_HAND = 0.2;
    double FACTOR_HAND_OVER_MONEY = 0.0;

    public PokerHeuristic() {
        addTunableParameter("FACTOR_MONEY", 0.8);
        addTunableParameter("FACTOR_HAND", 0.2);
        addTunableParameter("FACTOR_HAND_OVER_MONEY", 0.0);
    }

    @Override
    public void _reset() {
        FACTOR_MONEY = (double) getParameterValue("FACTOR_MONEY");
        FACTOR_HAND = (double) getParameterValue("FACTOR_HAND");
        FACTOR_HAND_OVER_MONEY = (double) getParameterValue("FACTOR_HAND_OVER_MONEY");
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected PokerHeuristic _copy() {
        PokerHeuristic retValue = new PokerHeuristic();
        retValue.FACTOR_MONEY = FACTOR_MONEY;
        retValue.FACTOR_HAND = FACTOR_HAND;
        retValue.FACTOR_HAND_OVER_MONEY = FACTOR_HAND_OVER_MONEY;
        return retValue;
    }

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    @Override
    protected boolean _equals(Object o) {
        if (o instanceof PokerHeuristic) {
            PokerHeuristic other = (PokerHeuristic) o;
            return other.FACTOR_MONEY == FACTOR_MONEY && other.FACTOR_HAND == FACTOR_HAND &&
                    other.FACTOR_HAND_OVER_MONEY == FACTOR_HAND_OVER_MONEY;
        }
        return false;
    }

    /**
     * @return Returns Tuned Parameters corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    @Override
    public PokerHeuristic instantiate() {
        return _copy();
    }


    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];
        PokerGameState pgs = (PokerGameState) gs;
        PokerGameParameters params = (PokerGameParameters) pgs.getGameParameters();

        double maxMoney = params.nWinMoney;
        if (!params.endMinMoney) {
            maxMoney = gs.getNPlayers() * params.nStartingMoney; // all of the money in the game
        }

        if (playerResult != CoreConstants.GameResult.GAME_ONGOING)
            return pgs.playerMoney[playerId].getValue() / maxMoney;
        // given the nature of Poker, we do not return Win/Lose, but the amount of money at the end of the game
        // as a fraction of the total the player could have won

        List<FrenchCard> localPlayerHand = pgs.getPlayerDecks().get(playerId).getComponents();
        int value = 0;

        for (FrenchCard frenchCard : localPlayerHand) {
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
        return value * FACTOR_HAND / 100.0 + pgs.playerMoney[playerId].getValue() / maxMoney * FACTOR_MONEY +
                value * 1.0 / (pgs.playerMoney[playerId].getValue() + 1) * FACTOR_HAND_OVER_MONEY;
        // FACTOR_HAND_OVER_MONEY is purely for backwards compatibility with the original heuristic
    }

}
