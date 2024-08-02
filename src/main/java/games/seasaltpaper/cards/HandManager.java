package games.seasaltpaper.cards;

import core.actions.AbstractAction;
import core.components.Card;
import core.components.PartialObservableDeck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.actions.BoatDuo;
import games.seasaltpaper.actions.FishDuo;
import games.seasaltpaper.actions.SailorSharkDuo;
import games.seasaltpaper.actions.ShellDuo;

import java.util.ArrayList;
import java.util.List;

public class HandManager {

    private HandManager(){}

    public static  List<AbstractAction> generateDuoActions(SeaSaltPaperGameState gs, int playerId) {
        ArrayList<AbstractAction> duoActions = new ArrayList<>();
        // TODO make this more general? Does ENUM correspond to number? can manually make them correspond to numbers
        int[] shellDuo, boatDuo, fishDuo, sailorSharkDuo; // = new int[]{-1, -1};    // array contain card number of the duo
        shellDuo = new int[]{-1, -1};
        boatDuo = new int[]{-1, -1};
        fishDuo = new int[]{-1, -1};
        sailorSharkDuo = new int[]{-1, -1};
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
                case SHELL:
                    duoCards = shellDuo;
                    break;
                case BOAT:
                    duoCards = boatDuo;
                    break;
                case FISH:
                    duoCards = fishDuo;
                    break;
                case SAILOR:
                    if (sailorSharkDuo[0] == -1) {
                        sailorSharkDuo[0] = i;
                    }
                    break;
                case SHARK:
                    if (sailorSharkDuo[1] == -1) {
                        sailorSharkDuo[1] = i;
                    }
            }
            if (suite == CardSuite.SAILOR || suite == CardSuite.SHARK) { continue; }
            if (duoCards[0] == -1) {
                duoCards[0] = i;
            }
            else if (duoCards[1] == -1)
            {
                duoCards[1] = i;
            }
        }
        if (shellDuo[0] != -1 && shellDuo[1] != -1) {
            duoActions.add(new ShellDuo(playerId, shellDuo));
        }
        if (boatDuo[0] != -1 && boatDuo[1] != -1) {
            duoActions.add(new BoatDuo(playerId, boatDuo));
        }
        if (fishDuo[0] != -1 && fishDuo[1] != -1) {
            duoActions.add(new FishDuo(playerId, fishDuo));
        }
        if (sailorSharkDuo[0] != -1 && sailorSharkDuo[1] != -1) {
            duoActions.add(new SailorSharkDuo(playerId, sailorSharkDuo));
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
