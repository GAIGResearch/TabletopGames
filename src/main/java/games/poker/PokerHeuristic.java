package games.poker;

import core.AbstractGameState;
import core.components.FrenchCard;
import core.interfaces.IStateHeuristic;
import games.poker.PokerGameState;

import java.util.ArrayList;
import java.util.List;

public class PokerHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        PokerGameState pgs = (PokerGameState) gs;

        List<FrenchCard> localPlayerHand = pgs.getPlayerDecks().get(pgs.getCurrentPlayer()).getComponents();
        boolean communityCardAvailable = false;

        int value = 0;
        if (gs.getTurnOrder().getRoundCounter() == 2) {
            localPlayerHand.add(pgs.communityCards[0]);
            localPlayerHand.add(pgs.communityCards[1]);
            localPlayerHand.add(pgs.communityCards[2]);
            communityCardAvailable = true;
        }
        else if (gs.getTurnOrder().getRoundCounter() == 3) {
            localPlayerHand.add(pgs.communityCards[0]);
            localPlayerHand.add(pgs.communityCards[1]);
            localPlayerHand.add(pgs.communityCards[2]);
            localPlayerHand.add(pgs.communityCards[3]);
            communityCardAvailable = true;
        }
        else if (gs.getTurnOrder().getRoundCounter() >= 4) {
            localPlayerHand.add(pgs.communityCards[0]);
            localPlayerHand.add(pgs.communityCards[1]);
            localPlayerHand.add(pgs.communityCards[2]);
            localPlayerHand.add(pgs.communityCards[3]);
            localPlayerHand.add(pgs.communityCards[4]);
            communityCardAvailable = true;
        }
       /* else {
            //System.out.println("No Community Cards");
        }*/

        for (int i = 0; i < localPlayerHand.size(); i++) {
            FrenchCard.FrenchCardType player = localPlayerHand.get(i).type;
            switch (localPlayerHand.get(i).type) { //should be the player's card instead instead of card.type, replace with the actual card from the player
                case Number:
                    value += localPlayerHand.get(i).drawN;
                    break;
                case Jack:
                    if (localPlayerHand.get(i).suite == "Spades") {
                        value += 10;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Diamonds") {
                        value += 11;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Hearts") {
                        value += 12;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Clubs") {
                        value += 13;
                        break;
                    }
                    break;

                case Queen:
                    if (localPlayerHand.get(i).suite == "Spades") {
                        value += 14;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Diamonds") {
                        value += 15;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Hearts") {
                        value += 16;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Clubs") {
                        value += 17;
                        break;
                    }
                    break;
                case King:
                    if (localPlayerHand.get(i).suite == "Spades") {
                        value += 18;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Diamonds") {
                        value += 19;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Hearts") {
                        value += 20;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Clubs") {
                        value += 21;
                        break;
                    }
                    break;
                case Ace:
                    if (localPlayerHand.get(i).suite == "Spades") {
                        value += 22;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Diamonds") {
                        value += 23;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Hearts") {
                        value += 24;
                        break;
                    } else if (localPlayerHand.get(i).suite == "Clubs") {
                        value += 25;
                        break;
                    }
                    break;
            }
        }
        if (communityCardAvailable == true) {
            return value / (pgs.getPlayerMoney(pgs.getCurrentPlayer()) + 1);
        }
        else {
            return value;
        }
    }

}
