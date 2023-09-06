package games.sushigo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import games.sushigo.cards.SGCard.SGCardType;

import java.util.*;

import static games.sushigo.cards.SGCard.SGCardType.*;

public class SGHeuristic extends TunableParameters implements IStateHeuristic {

    double singleTempuraValue = 2.5;
    double singleSashimiValue = 10.0 / 3.0;
    double doubleSashimiValue = 20.0 / 3.0;
    double wasabiSquidValue = 6;
    double wasabiSalmonValue = 4;
    double wasabiEggValue = 2;
    double winningMakiValue = 6;
    double runnerupMakiValue = 3;
    double mostPuddingsValue = 6;
    double leastPuddingsValue = -6;

    // values for cards that don't have inherent values that are in the game score
    double[] roundModifiers = new double[] {};
    ArrayList<SGCard> availableCards = new ArrayList<SGCard>();
    int nPlayers = 0;

    public SGHeuristic() {
        addTunableParameter("singleTempuraValue", singleTempuraValue);
        addTunableParameter("singleSashimiValue", singleSashimiValue);
        addTunableParameter("doubleSashimiValue", doubleSashimiValue);
        addTunableParameter("wasabiSquidValue", wasabiSquidValue);
        addTunableParameter("wasabiSalmonValue", wasabiSalmonValue);
        addTunableParameter("wasabiEggValue", wasabiEggValue);
        addTunableParameter("winningMakiValue", winningMakiValue);
        addTunableParameter("runnerupMakiValue", runnerupMakiValue);
        addTunableParameter("mostPuddingsValue", mostPuddingsValue);
        addTunableParameter("leastPuddingsValue", leastPuddingsValue);
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (!gs.isNotTerminal())
            return gs.getPlayerResults()[playerId].value;
        SGGameState sggs = (SGGameState) gs;

        nPlayers = sggs.getNPlayers();
        // Get player game scores
        roundModifiers = new double[nPlayers];
        determineAvailableCards(sggs, playerId);
        evaluateUncompletedCards(sggs, playerId);
        // double heuristicScore = evaluateIndividualScore(sggs, playerId);
        // double heuristicScore = evaluateDiff2BestPlayer(sggs, playerId);
        double heuristicScore = evaluateWeightedDiffAllPlayers(sggs, playerId);
        return heuristicScore;
    }

    void determineAvailableCards(SGGameState sggs, int playerId) {
        ArrayList<SGCard> availableCards = new ArrayList<>();
        for (int i = 0; i < nPlayers; i++) {
            if (!sggs.hasNotSeenHand(playerId, i))
                availableCards.addAll(sggs.getPlayedCards().get(i).getComponents());
        }
        this.availableCards = availableCards;
    }

    double getModifiedScore(SGGameState sggs, int playerId) {
        double modifierFactor = nPlayers / 10.0;
        return sggs.getGameScore(playerId) + (modifierFactor * roundModifiers[playerId]);
    }

    double evaluateIndividualScore(SGGameState sggs, int playerId) {
        // Same as default heuristic, but with the modified game sore
        if (sggs.isNotTerminal()) {
            return getModifiedScore(sggs, playerId);
        }
        return sggs.getPlayerResults()[playerId].value;
    }

    double evaluateDiff2BestPlayer(SGGameState sggs, int playerId) {
        // Find the best player score (besides current player)
        double bestOtherPlayerScore = -1;
        double currentPlayerScore = getModifiedScore(sggs, playerId);
        double maxScore = currentPlayerScore;
        double minScore = currentPlayerScore;
        for (int i = 0; i < nPlayers; i++) {
            if (i == playerId)
                continue;
            double playerScore = getModifiedScore(sggs, i);
            if (playerScore > maxScore)
                maxScore = playerScore;
            if (playerScore < minScore)
                minScore = playerScore;
            if (playerScore > bestOtherPlayerScore)
                bestOtherPlayerScore = playerScore;
        }
        // Maximize difference to best other player
        double bestPlayerDif = currentPlayerScore - bestOtherPlayerScore;
        // Dividing by max score gives bigger leads more value, while keeping it
        // relative to current board state
        return bestPlayerDif / Math.max(maxScore, 1);
    }

    double evaluateWeightedDiffAllPlayers(SGGameState sggs, int playerId) {
        // Make a key value list of all other players scores, with key as player ID
        double[] scores = new double[nPlayers - 1];
        for (int _i = 0; _i < nPlayers; _i++) {
            int i = _i;
            if (i == playerId)
                continue;
            if (i > playerId)
                i--;
            scores[i] = getModifiedScore(sggs, _i);
        }
        // Sort it by highest to lowest
        Arrays.sort(scores);
        // Calculate weighted score difference
        double result = 0.0;
        double denominator = 1.0;
        double playerScore = getModifiedScore(sggs, playerId);
        for (int i = scores.length - 1; i >= 0; i--) {
            denominator *= 2.0;
            result += (playerScore - scores[i]) / denominator;
        }
        // account for the missing part by using this formula
        result *= denominator / (denominator - 1);
        // Normalize result between current score range
        double maxScore = Math.max(scores[scores.length - 1], playerScore);
        result /= Math.max(maxScore, 1);
        // Done
        return result;
    }

    void evaluateUncompletedCards(SGGameState sggs, int playerId) {
        List<Deck<SGCard>> hands = sggs.getPlayerHands();
        // Receive amount of different card types of each player
        CardCount[] ccs = new CardCount[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            boolean seen = !sggs.hasNotSeenHand(playerId, i);
            // Evaluate uncompleted Cards
            CardCount cc = getPlayerCardCount(sggs, i);
            ccs[i] = cc;
            // Get hand of player i
            ArrayList<SGCard> hand = (ArrayList<SGCard>) hands.get(i).getComponents();

            // Evaluate Tempura
            // ... if player has an unpaired tempura on the field
            if (cc.nTempura == 1) {
                double likelihoodFactor = 1;
                // If player has another tempura in hand, tempura modifier can be ignored
                if (seen && hand.stream().anyMatch(c -> c.type == Tempura))
                    cc.nTempura = 0;
                else {
                    // Check how many tempuras are available, and determine some likelihood that
                    // player will get one
                    int nAvailableTempura = (int) availableCards.stream().filter(c -> c.type == Tempura).count();
                    if (nAvailableTempura < nPlayers)
                        likelihoodFactor = 1.0 / (nPlayers - nAvailableTempura);
                    roundModifiers[i] += likelihoodFactor * singleTempuraValue;
                }
            }

            // Evaluate Sashimi
            // ... if player is missing 2 sashimi to complete trio
            if (cc.nSashimi == 1) {
                double likelihoodFactor = 1;
                // If there are still >= 2 sashimi available, give modifier
                int nAvailableSashimi = (int) availableCards.stream().filter(c -> c.type == Sashimi).count();
                if (nAvailableSashimi < nPlayers)
                    likelihoodFactor = 1.0 / (nPlayers - nAvailableSashimi);
                // Square likelihood factor since player still needs 2 sashimi
                likelihoodFactor *= likelihoodFactor;
                if (nAvailableSashimi >= 2)
                    roundModifiers[i] += likelihoodFactor * singleSashimiValue;
            }
            // ... if player is missing 1 sashimi to complete trio
            if (cc.nSashimi == 2) {
                double likelihoodFactor = 1;
                // If player has another sashimi in hand, modifier can be ignored
                if (seen && hand.stream().anyMatch(c -> c.type == Sashimi))
                    cc.nSashimi = 0;
                // If no other sashimi are in play, modifier can be ignored
                else {
                    // Check how many sashimi are available, and determine some likelihood that
                    // player will get one
                    int nAvailableSashimi = (int) availableCards.stream().filter(c -> c.type == Sashimi).count();
                    if (nAvailableSashimi < nPlayers)
                        likelihoodFactor = 1.0 / (nPlayers - nAvailableSashimi);
                    roundModifiers[i] += likelihoodFactor * doubleSashimiValue;
                }
            }

            // Evaluate Wasabi
            if (cc.nWasabi > 0) {
                double likelihoodFactor = 1.0;
                int nAvailableNigiri = 0;
                double highestValue = 0.0;
                // If player has squid nigiri in hand, modifier can be ignored
                // Otherwise, as long as there are squid nigiri in play, add modifier for squid
                // nigiri
                if (!seen || hand.stream().noneMatch(c -> c.type == SquidNigiri)) {
                    nAvailableNigiri += (int) availableCards.stream().filter(c -> c.type == SquidNigiri).count();
                    if (nAvailableNigiri < nPlayers)
                        likelihoodFactor = 1.0 / (nPlayers - nAvailableNigiri);
                    highestValue = Math.max(highestValue, likelihoodFactor * wasabiSquidValue);
                }
                // Continue the same steps in descending order with salmon nigiri and egg nigiri
                if (!seen || hand.stream().noneMatch(c -> c.type == SalmonNigiri)) {
                    // nAvailableNigiri is added, since existing squid nigiri increase chance of
                    // getting salmon nigiri
                    nAvailableNigiri += (int) availableCards.stream().filter(c -> c.type == SalmonNigiri).count();
                    likelihoodFactor = (nAvailableNigiri < nPlayers) ? 1.0 / (nPlayers - nAvailableNigiri) : 1.0;
                    highestValue = Math.max(highestValue, likelihoodFactor * wasabiSalmonValue);
                }
                if (!seen || hand.stream().noneMatch(c -> c.type == EggNigiri)) {
                    nAvailableNigiri += (int) availableCards.stream().filter(c -> c.type == EggNigiri).count();
                    likelihoodFactor = (nAvailableNigiri < nPlayers) ? 1.0 / (nPlayers - nAvailableNigiri) : 1.0;
                    highestValue = Math.max(highestValue, likelihoodFactor * wasabiEggValue);
                }
                if (nAvailableNigiri > 0)
                    roundModifiers[i] += highestValue;
            }

            // Evaluate Chopsticks
            // ... chopsticks are worth as many rounds as there are left - 1 (as with 1
            // round left you cannot use them)
            if (cc.nChopsticks > 0)
                roundModifiers[i] += hand.size() - 1;

            // Evaluate Dumplings
            if (cc.nDumplings > 0 && cc.nDumplings < 5) {
                double likelihoodFactor = 1.0 / (nPlayers - 1);
                // Dumplings become worth the highest average amount they can be worth
                // e.g.: 2 dumplings on field, 2 in any hands, means player can have max 4
                // dumplings, which would be
                // worth 10 points. Therefore, each dumpling on board is worth 2.5 points, so
                // the 2 dumplings on
                // board are worth 5 points. Subtract from that the existing 3 points the
                // dumplings give to get
                // a 2 point modifier.
                int totalNDumplings = cc.nDumplings
                        + (int) availableCards.stream().filter(c -> c.type == Dumpling).count();
                if (totalNDumplings > 5)
                    totalNDumplings = 5;
                int[] score = new int[] { 1, 3, 6, 10, 15 };
                double pointsToAdd = cc.nDumplings * (score[totalNDumplings - 1] - score[cc.nDumplings - 1]);
                roundModifiers[i] += likelihoodFactor * pointsToAdd;
            }
        }

        // Evaluate Maki
        // Filter ccs list into arrays of pairs [id, nMaki] and sort by number of maki
        // in descending order
        int[][] sortedMaki = Arrays.stream(ccs).map(cc -> new int[] { cc.playerId, cc.nMaki })
                .sorted(Comparator.comparingInt(pair -> pair[1])).toArray(int[][]::new);
        Collections.reverse(Arrays.asList(sortedMaki));
        // Receive important information to distribute points
        int winningMakiScore = sortedMaki[0][1];
        int nWinnersMaki = (int) Arrays.stream(sortedMaki).filter(pair -> pair[1] == winningMakiScore).count();
        int runnerupMakiScore = (nWinnersMaki == nPlayers) ? 0 : sortedMaki[nWinnersMaki][1];
        int nRunnersupMaki = (nWinnersMaki == nPlayers) ? 0
                : (int) Arrays.stream(sortedMaki).filter(pair -> pair[1] == runnerupMakiScore).count();
        // Calculate actual modifier points based on number of players splitting it
        int winningMakiPoints = winningMakiScore > 0 ? (int) winningMakiValue / nWinnersMaki : 0;
        int runnerupMakiPoints = runnerupMakiScore > 0 ? (int) runnerupMakiValue / nRunnersupMaki : 0;
        // Add points to respective players
        for (int i = 0; i < nWinnersMaki; i++)
            roundModifiers[sortedMaki[i][0]] += winningMakiPoints;
        for (int i = nWinnersMaki; i < nWinnersMaki + nRunnersupMaki; i++)
            roundModifiers[sortedMaki[i][0]] += runnerupMakiPoints;

        // Evaluate Puddings
        // Filter ccs list into arrays of pairs [id, nPudding] and sort by number of
        // puddings in descending order
        int[][] sortedPuddings = Arrays.stream(ccs).map(cc -> new int[] { cc.playerId, cc.nPudding })
                .sorted(Comparator.comparingInt(pair -> pair[1])).toArray(int[][]::new);
        Collections.reverse(Arrays.asList(sortedPuddings));
        // Receive important information to distribute points
        int mostPuddingsScore = sortedPuddings[0][1];
        int nMostPuddings = (int) Arrays.stream(sortedPuddings).filter(pair -> pair[1] == mostPuddingsScore).count();
        int leastPuddingsScore = sortedPuddings[nPlayers - 1][1];
        int nLeastPuddings = (int) Arrays.stream(sortedPuddings).filter(pair -> pair[1] == leastPuddingsScore).count();
        // Calculate actual modifier points based on number of players splitting it
        int mostPuddingsPoints = mostPuddingsScore > 0 ? (int) mostPuddingsValue / nMostPuddings : 0;
        int leastPuddingPoints = (int) leastPuddingsValue / nLeastPuddings;
        // Add points to respective players
        for (int i = 0; i < nMostPuddings; i++)
            roundModifiers[sortedPuddings[i][0]] += mostPuddingsPoints;
        for (int i = nPlayers - 1; i >= nPlayers - nLeastPuddings; i--)
            roundModifiers[sortedPuddings[i][0]] += leastPuddingPoints;
    }

    @Override
    public void _reset() {
        singleTempuraValue = (double) getParameterValue("singleTempuraValue");
        singleSashimiValue = (double) getParameterValue("singleSashimiValue");
        doubleSashimiValue = (double) getParameterValue("doubleSashimiValue");
        wasabiSquidValue = (double) getParameterValue("wasabiSquidValue");
        wasabiSalmonValue = (double) getParameterValue("wasabiSalmonValue");
        wasabiEggValue = (double) getParameterValue("wasabiEggValue");
        winningMakiValue = (double) getParameterValue("winningMakiValue");
        runnerupMakiValue = (double) getParameterValue("runnerupMakiValue");
        mostPuddingsValue = (double) getParameterValue("mostPuddingsValue");
        leastPuddingsValue = (double) getParameterValue("leastPuddingsValue");
        roundModifiers = new double[nPlayers];
        availableCards = new ArrayList<>();
    }

    @Override
    protected AbstractParameters _copy() {
        SGHeuristic retValue = new SGHeuristic();
        retValue.singleTempuraValue = singleTempuraValue;
        retValue.singleSashimiValue = singleSashimiValue;
        retValue.doubleSashimiValue = doubleSashimiValue;
        retValue.wasabiSquidValue = wasabiSquidValue;
        retValue.wasabiSalmonValue = wasabiSalmonValue;
        retValue.wasabiEggValue = wasabiEggValue;
        retValue.winningMakiValue = winningMakiValue;
        retValue.runnerupMakiValue = runnerupMakiValue;
        retValue.mostPuddingsValue = mostPuddingsValue;
        retValue.leastPuddingsValue = leastPuddingsValue;
        retValue.roundModifiers = roundModifiers.clone();
        retValue.availableCards = (ArrayList<SGCard>) availableCards.clone();
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof SGHeuristic) {
            SGHeuristic other = (SGHeuristic) o;
            return other.singleTempuraValue == singleTempuraValue &&
                    other.singleSashimiValue == singleSashimiValue &&
                    other.doubleSashimiValue == doubleSashimiValue &&
                    other.wasabiSquidValue == wasabiSquidValue &&
                    other.wasabiSalmonValue == wasabiSalmonValue &&
                    other.wasabiEggValue == wasabiEggValue &&
                    other.winningMakiValue == winningMakiValue &&
                    other.runnerupMakiValue == runnerupMakiValue &&
                    other.mostPuddingsValue == mostPuddingsValue &&
                    other.leastPuddingsValue == leastPuddingsValue;
        }
        return false;
    }

    @Override
    public Object instantiate() {
        return this._copy();
    }

    class CardCount {
        int playerId;
        int nTempura;
        int nSashimi;
        int nWasabi;
        int nChopsticks;
        int nDumplings;
        int nMaki;
        int nPudding;

        CardCount(int playerId, int nTempura, int nSashimi, int nWasabi, int nChopsticks, int nDumplings, int nMaki,
                int nPudding) {
            this.playerId = playerId;
            this.nTempura = nTempura;
            this.nSashimi = nSashimi;
            this.nWasabi = nWasabi;
            this.nChopsticks = nChopsticks;
            this.nDumplings = nDumplings;
            this.nMaki = nMaki;
            this.nPudding = nPudding;
        }
    }

    CardCount getPlayerCardCount(SGGameState sggs, int playerId) {
        int nTempura = sggs.getPlayedCardTypes(Tempura, playerId).getValue() % 2;
        int nSashimi = sggs.getPlayedCardTypes(Sashimi, playerId).getValue() % 3;
        int nWasabi = sggs.getPlayedCardTypes(Wasabi, playerId).getValue();
        int nChopsticks = sggs.getPlayedCardTypes(Chopsticks, playerId).getValue();
        int nDumplings = sggs.getPlayedCardTypes(Dumpling, playerId).getValue();
        int nMaki = 0;
        int nPudding = 0;
        for (SGCard card : sggs.getPlayedCards().get(playerId).getComponents()) {
            if (card.type == Maki)
                nMaki += card.count;
            if (card.type == Pudding)
                nPudding++;
        }
        return new CardCount(playerId, nTempura, nSashimi, nWasabi, nChopsticks, nDumplings, nMaki, nPudding);
    }
}

class PlayerInfo<T> {
    final int id;
    final T value;

    PlayerInfo(int id, T value) {
        this.id = id;
        this.value = value;
    }
}
