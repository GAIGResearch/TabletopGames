package games.seasaltpaper.cards;

import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SeaSaltPaperParameters;
import games.seasaltpaper.actions.BoatDuo;
import games.seasaltpaper.actions.FishDuo;
import games.seasaltpaper.actions.SwimmerSharkDuo;
import games.seasaltpaper.actions.CrabDuo;
import shapeless.ops.nat;

import java.util.*;

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
        if (crabDuo[0] != -1 && crabDuo[1] != -1 && !gs.allDiscardPilesEmpty()) { // checked if both piles are empty
            duoActions.add(new CrabDuo(playerId, crabDuo));
        }
        if (boatDuo[0] != -1 && boatDuo[1] != -1) {
            duoActions.add(new BoatDuo(playerId, boatDuo));
        }
        if (fishDuo[0] != -1 && fishDuo[1] != -1) {
            duoActions.add(new FishDuo(playerId, fishDuo));
        }
        if (swimmerSharkDuo[0] != -1 && swimmerSharkDuo[1] != -1 && !gs.allEnemiesProtectedOrEmpty(playerId)) {
            // checked if all enemies are either protected or empty hand
            duoActions.add(new SwimmerSharkDuo(playerId, swimmerSharkDuo));
        }
        return duoActions;
    }

    public static int calculatePoint(SeaSaltPaperGameState gs, int playerID) {
        PartialObservableDeck<SeaSaltPaperCard> playerHand = gs.getPlayerHands().get(playerID);
        Deck<SeaSaltPaperCard> playerDiscard = gs.getPlayerDiscards().get(playerID);
        Map<CardSuite, Integer> collectorDict = new HashMap<>();
        Map<CardSuite, Integer> multiplierDict = new HashMap<>();
        Map<CardSuite, Integer> duoDict = new HashMap<>();
        Map<CardSuite, Integer> suiteDict = new HashMap<>();
        int mermaidCount = 0;

        // Iterate through player hand
        for (int i=0; i<playerHand.getSize(); i++) {
            SeaSaltPaperCard c = playerHand.get(i);
            CardSuite suite = c.getCardSuite();
            CardType type = c.getCardType();

            if (type != CardType.MULTIPLIER) {  // Don't count multiplier card into overall suite count
                suiteDict.put(suite, suiteDict.getOrDefault(suite, 0) + 1);
            }
            else {
                multiplierDict.put(suite, multiplierDict.getOrDefault(suite, 0) + 1);
            }

            if (type == CardType.COLLECTOR) {
                collectorDict.put(suite, collectorDict.getOrDefault(suite, 0) + 1);
            }
            else if (type == CardType.DUO) {
                duoDict.put(suite, duoDict.getOrDefault(suite, 0) + 1);
            }

            if (suite==CardSuite.MERMAID) {mermaidCount++;}
        }

        // Iterate playerDiscard
        for (int i=0; i<playerDiscard.getSize(); i++) {
            CardSuite suite = playerDiscard.get(i).getCardSuite();
            suiteDict.put(suite, suiteDict.getOrDefault(suite, 0) + 1);
        }

        int score = 0;
        SeaSaltPaperParameters param = (SeaSaltPaperParameters) gs.getGameParameters();

        // Calculate Collection score
        for (CardSuite suite : collectorDict.keySet()) {
            score += param.collectorBonusDict.get(suite)[collectorDict.get(suite) - 1];
        }
        //Calculate Multiplier score
        for (CardSuite suite : multiplierDict.keySet()) {
            if (suiteDict.containsKey(suite)) {
                score += param.multiplierDict.get(suite) * suiteDict.get(suite);
            }
        }
        //Calculate Mermaid score
        if (mermaidCount >= 4) {
            return 999; // Instant Win
            // TODO implement this properly as its own event
        }
        score += calculateColorBonus(gs, playerID, mermaidCount);

        //Calculate Duo points
        // Duo cards in hand (not played)
        for (CardSuite suite : duoDict.keySet()) {
            if (suite == CardSuite.SHARK) { continue; }
            int pair;
            if (suite == CardSuite.SWIMMER) {
                pair = Math.min(duoDict.get(CardSuite.SWIMMER), duoDict.getOrDefault(CardSuite.SHARK, 0));
            }
            else {
                pair = duoDict.get(suite) / 2;
            }
            score += pair * param.duoBonusDict.get(suite);
        }
        score += gs.playerPlayedDuoPoints[playerID]; // Duo cards played

        return score;
    }

    public static int calculateColorBonus(SeaSaltPaperGameState gs, int playerID) {
        return calculateColorBonus(gs, playerID, 1);
    }

    // Calculate color bonus for the n_colors highest colors
    public static int calculateColorBonus(SeaSaltPaperGameState gs, int playerID, int n_colors) {
        if (n_colors == 0) { return 0; }

        PartialObservableDeck<SeaSaltPaperCard> playerHand = gs.getPlayerHands().get(playerID);
        Deck<SeaSaltPaperCard> playerDiscard = gs.getPlayerDiscards().get(playerID);
        Map<CardColor, Integer> colorDict = new HashMap<>();

        // Iterate through player hand
        for (int i=0; i<playerHand.getSize(); i++) {
            CardColor color = playerHand.get(i).getCardColor();
            colorDict.put(color, colorDict.getOrDefault(color, 0) + 1);
        }
        // Iterate through player discard
        for (int i=0; i<playerDiscard.getSize(); i++) {
            CardColor color = playerDiscard.get(i).getCardColor();
            colorDict.put(color, colorDict.getOrDefault(color, 0) + 1);
        }

        ArrayList<Integer> colorBonuses = new ArrayList<>(colorDict.values());
        Collections.sort(colorBonuses); Collections.reverse(colorBonuses);
        n_colors = Integer.min(n_colors, colorBonuses.size());
        int score = 0;
        for (int i=0; i<n_colors; i++) {
            score += colorBonuses.get(i);
        }
        return score;
    }

}
