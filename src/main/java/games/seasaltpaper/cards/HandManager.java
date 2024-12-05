package games.seasaltpaper.cards;

import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.actions.BoatDuo;
import games.seasaltpaper.actions.FishDuo;
import games.seasaltpaper.actions.SwimmerSharkDuo;
import games.seasaltpaper.actions.CrabDuo;

import java.util.ArrayList;
import java.util.List;

public class HandManager {

    private HandManager(){}

    public static  List<AbstractAction> generateDuoActions(SeaSaltPaperGameState gs, int playerId) {
        ArrayList<AbstractAction> duoActions = new ArrayList<>();
        // TODO make this more general? Does ENUM correspond to number? can manually make them correspond to numbers
        int[] crabDuo, boatDuo, fishDuo, swimmerSharkDuo; // = new int[]{-1, -1};    // array contain card number of the duo
        crabDuo = new int[]{-1, -1};
        boatDuo = new int[]{-1, -1};
        fishDuo = new int[]{-1, -1};
        swimmerSharkDuo = new int[]{-1, -1};
        PartialObservableDeck<SeaSaltPaperCard> playerHand =  gs.getPlayerHands().get(playerId);
        // get duo cards
        for (int i = 0; i < playerHand.getSize(); i++) {
            SeaSaltPaperCard card = playerHand.get(i);
            if (card.cardType != CardType.DUO) {
                continue;
            }
            CardSuite suite = card.cardSuite;
            int[] duoCards = null;
            switch(suite) {
                case CRAB:
                    duoCards = crabDuo;
                    break;
                case BOAT:
                    duoCards = boatDuo;
                    break;
                case FISH:
                    duoCards = fishDuo;
                    break;
                case SWIMMER:
                    if (swimmerSharkDuo[0] == -1) {
                        swimmerSharkDuo[0] = i;
                    }
                    break;
                case SHARK:
                    if (swimmerSharkDuo[1] == -1) {
                        swimmerSharkDuo[1] = i;
                    }
                    break;
                default:
                    throw(new RuntimeException(suite.name() + " is not a valid Duo Card, but exist anyway"));
            }
            if (suite == CardSuite.SWIMMER || suite == CardSuite.SHARK) { continue; }
            if (duoCards[0] == -1) {
                duoCards[0] = i;
            }
            else if (duoCards[1] == -1)
            {
                duoCards[1] = i;
            }
        }
        if (crabDuo[0] != -1 && crabDuo[1] != -1) {
            // TODO check at least one discard pile is not empty
            duoActions.add(new CrabDuo(playerId, crabDuo));
        }
        if (boatDuo[0] != -1 && boatDuo[1] != -1) {
            duoActions.add(new BoatDuo(playerId, boatDuo));
        }
        if (fishDuo[0] != -1 && fishDuo[1] != -1) {
            duoActions.add(new FishDuo(playerId, fishDuo));
        }
        if (swimmerSharkDuo[0] != -1 && swimmerSharkDuo[1] != -1) {
            duoActions.add(new SwimmerSharkDuo(playerId, swimmerSharkDuo));
        }
        return duoActions;
    }

    public static int calculatePoint(SeaSaltPaperGameState gs, int playerID)
    {
        return 0;
    }

    public static int calculateColorBonus(SeaSaltPaperGameState gs, int playerID)
    {
        return 0;
    }

}
