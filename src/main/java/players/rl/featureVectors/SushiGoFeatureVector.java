package players.rl.featureVectors;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateFeatureVector;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import games.sushigo.cards.SGCard.SGCardType;

public class SushiGoFeatureVector implements IStateFeatureVector {

    // Factors to scale down feature values
    private final double HIGH_POINTS = 50.0;
    private final double MAX_PUDDING = 10.0;
    private final double HIGH_MAKI = 10.0;

    List<String> cardsToCount = Arrays.asList("Maki1", "Maki2", "Maki3", "SquidNigiri", "SalmonNigiri", "EggNigiri",
            "Tempura", "Sashimi", "Dumpling");

    List<SGCardType> cardTypes = Arrays.asList(SGCardType.values());

    String[] names = new LinkedList<String>() {
        {
            // Point Features
            add("Points");

            add("PointLeadToSecond");
            add("PointTrailToFirst");

            add("PointLeadToNext");
            add("PointTrailToNext");

            add("PointLeadToNextNext");
            add("PointTrailToNextNext");

            // Pudding Features
            add("PuddingLeadToSecond");
            add("PuddingTrailToFirst");

            add("PuddingLeadToLast");
            add("PuddingTrailToSecondLast");

            add("PuddingLeadToNext");
            add("PuddingTrailToNext");

            add("PuddingLeadToNextNext");
            add("PuddingTrailToNextNext");

            // Maki Features
            add("MakiLeadToSecond");
            add("MakiTrailToFirst");

            add("MakiLeadToThird");
            add("MakiTrailToSecond");

            add("MakiLeadToNext");
            add("MakiTrailToNext");

            add("MakiLeadToNextNext");
            add("MakiTrailToNextNext");

            // Tempura & Sashimi Features
            for (String prefix : Arrays.asList("", "Next", "NextNext")) {
                for (String postfix : Arrays.asList("", "AndInHand", "AndInNextHand", "AndInHandAndInNextHand")) {
                    add(prefix + "OneOffTempura" + postfix);
                    add(prefix + "OneOffSashimi" + postfix);
                    add(prefix + "TwoOffSashimi" + postfix);
                }
            }

            // Dumpling Features
            for (String prefix : Arrays.asList("", "Next", "NextNext")) {
                for (String postfix : Arrays.asList("", "AndInHand", "AndInNextHand", "AndInHandAndInNextHand")) {
                    for (int i = 1; i < 5; i++) { // Doesn't include 5 since there's no influence anymore
                        add(prefix + "DumplingsPlayed" + i + postfix);
                    }
                }
            }

            // Wasabi Features
            add("WasabiPlayed1");
            add("WasabiPlayed2+");
            add("NextWasabiPlayed");
            for (String prefix : Arrays.asList("", "Next")) {
                for (String nigiriType : Arrays.asList("Squid", "Salmon", "Egg")) {
                    for (String postfix : Arrays.asList("And" + nigiriType + "NigiriInHand",
                            "And" + nigiriType + "NigiriInNextHand")) {
                        add(prefix + "WasabiPlayed" + postfix);
                    }
                }
            }

            // Chopstick Features
            add("ChopsticksPlayed1");
            add("ChopsticksPlayed2+");
            add("NextChopsticksPlayed");
            for (String prefix : Arrays.asList("", "Next")) {
                for (int i = 0; i < cardsToCount.size(); i++)
                    for (int j = i; j < cardsToCount.size(); j++)
                        add(prefix + "ChopsticksAnd" + cardsToCount.get(i) + cardsToCount.get(j) + "InHand");
            }

            // Hand Features
            for (String cardName : cardsToCount)
                add(cardName + "InHand");
            for (String cardName : cardsToCount)
                add(cardName + "InNextHand");
        }
    }.toArray(String[]::new);

    public SushiGoFeatureVector() {
    }

    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        double[] features = new double[names.length];
        SGGameState sggs = (SGGameState) state;

        MutableInt f = new MutableInt(0);
        // NOTE: All feature values are scaled down to approximately [-1, 1]. This can
        // vary and some features are only [0, 1], while others can go below -1 or above
        // 1, but they should all be around that order of magnitude.

        calcPointFeatures(sggs, playerID, features, f);
        calcPuddingMakiFeatures(sggs, playerID, features, f);
        calcTempuraSashimiFeatures(sggs, playerID, features, f);
        calcDumplingFeatures(sggs, playerID, features, f);
        calcWasabiNigiriFeatures(sggs, playerID, features, f);
        calcChopstickFeatures(sggs, playerID, features, f);
        calcHandFeatures(sggs, playerID, features, f);

        return features;
    }

    void calcPointFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        int nextPlayerID = (playerID + 1) % sggs.getNPlayers();
        int nextNextPlayerID = (playerID + 2) % sggs.getNPlayers();
        double playerScore = sggs.getGameScore(playerID);
        double highestOpponentScore = Double.NEGATIVE_INFINITY;
        for (int _p = 0; _p < sggs.getNPlayers(); _p++) {
            int pID = (playerID + _p) % sggs.getNPlayers();
            if (pID == playerID)
                continue;
            double ps = sggs.getGameScore(pID);
            if (ps > highestOpponentScore)
                highestOpponentScore = ps;
        }
        double pointDiffToFirstOpponent = (playerScore - highestOpponentScore) / HIGH_POINTS;
        double pointDiffToNext = (playerScore - sggs.getGameScore(nextPlayerID)) / HIGH_POINTS;
        double pointDiffToNextNext = (playerScore - sggs.getGameScore(nextNextPlayerID)) / HIGH_POINTS;

        features[offset.getAndIncrement()] = playerScore / HIGH_POINTS; // Points

        features[offset.getAndIncrement()] = Math.max(0, pointDiffToFirstOpponent); // PointLeadToSecond
        features[offset.getAndIncrement()] = Math.max(0, -pointDiffToFirstOpponent); // pointTrailToFirst

        features[offset.getAndIncrement()] = Math.max(0, pointDiffToNext); // PointLeadToNext
        features[offset.getAndIncrement()] = Math.max(0, -pointDiffToNext); // PointTrailToNext

        features[offset.getAndIncrement()] = Math.max(0, pointDiffToNextNext); // PointLeadToNextNext
        features[offset.getAndIncrement()] = Math.max(0, -pointDiffToNextNext); // PointTrailToNextNext
    }

    void calcPuddingMakiFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        int playerPudding = -1;
        int mostPudding = 0;
        int leastPudding = Integer.MAX_VALUE;
        int nextPudding = -1;
        int nextNextPudding = -1;

        int playerMaki = 0;
        int mostMaki = 0;
        int secondMostMaki = 0;
        int nextMaki = -1;
        int nextNextMaki = -1;

        for (int _p = 0; _p < sggs.getNPlayers(); _p++) {
            int pID = (playerID + _p) % sggs.getNPlayers();
            int pudding = sggs.getPlayedCardTypes(SGCardType.Pudding, pID).getValue();
            int maki = sggs.getPlayedCardTypes(SGCardType.Maki, pID).getValue();
            if (pID == playerID) {
                playerPudding = pudding;
                playerMaki = maki;
            } else {
                mostPudding = Math.max(pudding, mostPudding);
                leastPudding = Math.min(pudding, leastPudding);
                if (maki > mostMaki) {
                    secondMostMaki = mostMaki;
                    mostMaki = maki;
                } else if (maki > secondMostMaki)
                    secondMostMaki = maki;
            }
            // Also save next and nextNext values
            if (_p == 1) { // next player
                nextMaki = maki;
                nextPudding = pudding;
            } else if (_p == 2) { // next next player
                nextNextMaki = maki;
                nextNextPudding = pudding;
            }
        }

        double puddingDiffToFirstOpponent = (playerPudding - mostPudding) / MAX_PUDDING;
        double puddingDiffToLastOpponent = (playerPudding - leastPudding) / MAX_PUDDING;
        double puddingDiffToNext = (playerPudding - nextPudding) / MAX_PUDDING;
        double puddingDiffToNextNext = (playerPudding - nextNextPudding) / MAX_PUDDING;

        double makiDiffToFirstOpponent = (playerMaki - mostMaki) / HIGH_MAKI;
        double makiDiffToSecondOpponent = (playerMaki - secondMostMaki) / HIGH_MAKI;
        double makiDiffToNext = (playerMaki - nextMaki) / HIGH_MAKI;
        double makiDiffToNextNext = (playerMaki - nextNextMaki) / HIGH_MAKI;

        // Pudding Features
        features[offset.getAndIncrement()] = Math.max(0, puddingDiffToFirstOpponent); // PuddingLeadToSecond
        features[offset.getAndIncrement()] = Math.max(0, -puddingDiffToFirstOpponent); // PuddingTrailToFirst

        features[offset.getAndIncrement()] = Math.max(0, puddingDiffToLastOpponent); // PuddingLeadToLast
        features[offset.getAndIncrement()] = Math.max(0, -puddingDiffToLastOpponent); // PuddingTrailToSecondLast

        features[offset.getAndIncrement()] = Math.max(0, puddingDiffToNext); // PuddingLeadToNext
        features[offset.getAndIncrement()] = Math.max(0, -puddingDiffToNext); // PuddingTrailToNext

        features[offset.getAndIncrement()] = Math.max(0, puddingDiffToNextNext); // PuddingLeadToNextNext
        features[offset.getAndIncrement()] = Math.max(0, -puddingDiffToNextNext); // PuddingTrailToNextNext

        // Maki Features
        features[offset.getAndIncrement()] = Math.max(0, makiDiffToFirstOpponent); // MakiLeadToSecond
        features[offset.getAndIncrement()] = Math.max(0, -makiDiffToFirstOpponent); // MakiTrailToFirst

        features[offset.getAndIncrement()] = Math.max(0, makiDiffToSecondOpponent); // MakiLeadToThird
        features[offset.getAndIncrement()] = Math.max(0, -makiDiffToSecondOpponent); // MakiTrailToSecond

        features[offset.getAndIncrement()] = Math.max(0, makiDiffToNext); // MakiLeadToNext
        features[offset.getAndIncrement()] = Math.max(0, -makiDiffToNext); // MakiTrailToNext

        features[offset.getAndIncrement()] = Math.max(0, makiDiffToNextNext); // MakiLeadToNextNext
        features[offset.getAndIncrement()] = Math.max(0, -makiDiffToNextNext); // MakiTrailToNextNext
    }

    void calcTempuraSashimiFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        int previousPlayerID = (playerID - 1 + sggs.getNPlayers()) % sggs.getNPlayers();
        boolean tempuraInHand = sggs.getPlayerHands().get(playerID).stream()
                .anyMatch(c -> c.type == SGCardType.Tempura);
        boolean tempuraInNextHand = sggs.getPlayerHands().get(previousPlayerID).stream()
                .anyMatch(c -> c.type == SGCardType.Tempura);
        boolean sashimiInHand = sggs.getPlayerHands().get(playerID).stream()
                .anyMatch(c -> c.type == SGCardType.Sashimi);
        boolean sashimiInNextHand = sggs.getPlayerHands().get(previousPlayerID).stream()
                .anyMatch(c -> c.type == SGCardType.Sashimi);

        for (int _p = 0; _p <= 2; _p++) {
            boolean ignoreNextNext = _p == 2 && sggs.getNPlayers() == 2;

            int pID = (playerID + _p) % sggs.getNPlayers();
            int nUnfinishedTempura = sggs.getPlayedCardTypes(SGCardType.Tempura, pID).getValue() % 2;
            int nUnfinishedSashimi = sggs.getPlayedCardTypes(SGCardType.Sashimi, pID).getValue() % 3;

            boolean oneOffTempura = ignoreNextNext ? false : nUnfinishedTempura == 1;
            boolean oneOffSashimi = ignoreNextNext ? false : nUnfinishedSashimi == 2;
            boolean twoOffSashimi = ignoreNextNext ? false : nUnfinishedSashimi == 1;
            for (boolean isHot : new boolean[] {
                    oneOffTempura, // {_, Next, NextNext}OneOffTempura
                    oneOffSashimi, // {_, Next, NextNext}OneOffSashimi
                    twoOffSashimi, // {_, Next, NextNext}TwoOffSashimi
                    oneOffTempura && tempuraInHand, // {_, Next, NextNext}OneOffTempuraAndInHand
                    oneOffSashimi && sashimiInHand, // {_, Next, NextNext}OneOffSashimiAndInHand
                    twoOffSashimi && sashimiInHand, // {_, Next, NextNext}TwoOffSashimiAndInHand
                    oneOffTempura && tempuraInNextHand, // {_, Next, NextNext}OneOffTempuraAndInNextHand
                    oneOffSashimi && sashimiInNextHand, // {_, Next, NextNext}OneOffSashimiAndInNextHand
                    twoOffSashimi && sashimiInNextHand, // {_, Next, NextNext}TwoOffSashimiAndInNextHand
                    oneOffTempura && tempuraInHand && tempuraInNextHand, // {...}OneOffTempuraAndInHandAndInNextHand
                    oneOffSashimi && sashimiInHand && sashimiInNextHand, // {...}OneOffSashimiAndInHandAndInNextHand
                    twoOffSashimi && sashimiInHand && sashimiInNextHand, // {...}TwoOffSashimiAndInHandAndInNextHand
            }) {
                features[offset.getAndIncrement()] = isHot ? 1 : 0;
            }
        }
    }

    void calcDumplingFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        int previousPlayerID = (playerID - 1 + sggs.getNPlayers()) % sggs.getNPlayers();
        boolean dumplingInHand = sggs.getPlayerHands().get(playerID).stream()
                .anyMatch(c -> c.type == SGCardType.Dumpling);
        boolean dumplingInNextHand = sggs.getPlayerHands().get(previousPlayerID).stream()
                .anyMatch(c -> c.type == SGCardType.Dumpling);

        for (int _p = 0; _p <= 2; _p++) {
            boolean ignoreNextNext = _p == 2 && sggs.getNPlayers() == 2;
            int pID = (playerID + _p) % sggs.getNPlayers();
            int nDumplingsPlayed = sggs.getPlayedCardTypes(SGCardType.Dumpling, pID).getValue();

            boolean[] dumplingsPlayed = new boolean[4];
            boolean[] dumplingsPlayedAndInHand = new boolean[4];
            boolean[] dumplingsPlayedAndInNextHand = new boolean[4];
            boolean[] dumplingsPlayedAndInHandAndInNextHand = new boolean[4];
            for (int i = 0; i < dumplingsPlayed.length; i++) {
                int nDumplings = i + 1;
                dumplingsPlayed[i] = !ignoreNextNext && nDumplingsPlayed == nDumplings;
                dumplingsPlayedAndInHand[i] = dumplingsPlayed[i] && dumplingInHand;
                dumplingsPlayedAndInNextHand[i] = dumplingsPlayed[i] && dumplingInNextHand;
                dumplingsPlayedAndInHandAndInNextHand[i] = dumplingsPlayed[i] && dumplingInHand && dumplingInNextHand;
            }
            for (int i = 0; i < dumplingsPlayed.length; i++)
                // {_, Next, NextNext}DumplingsPlayed{i}
                features[offset.getAndIncrement()] = dumplingsPlayed[i] ? 1 : 0;
            for (int i = 0; i < dumplingsPlayedAndInHand.length; i++)
                // {_, Next, NextNext}DumplingsPlayed{i}AndInHand
                features[offset.getAndIncrement()] = dumplingsPlayedAndInHand[i] ? 1 : 0;
            for (int i = 0; i < dumplingsPlayedAndInNextHand.length; i++)
                // {_, Next, NextNext}DumplingsPlayed{i}AndInNextHand
                features[offset.getAndIncrement()] = dumplingsPlayedAndInNextHand[i] ? 1 : 0;
            for (int i = 0; i < dumplingsPlayedAndInHandAndInNextHand.length; i++)
                // {_, Next, NextNext}DumplingsPlayed{i}AndInHandAndInNextHand
                features[offset.getAndIncrement()] = dumplingsPlayedAndInHandAndInNextHand[i] ? 1 : 0;
        }
    }

    void calcWasabiNigiriFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        final String[] nigiriType = { "Squid", "Salmon", "Egg" };
        int previousPlayerID = (playerID - 1 + sggs.getNPlayers()) % sggs.getNPlayers();
        Deck<SGCard> hand = sggs.getPlayerHands().get(playerID);
        Deck<SGCard> nextHand = sggs.getPlayerHands().get(previousPlayerID);
        boolean[] nigiriInHand = {
                hand.stream().anyMatch(c -> c.type == SGCardType.valueOf(nigiriType[0] + "Nigiri")),
                hand.stream().anyMatch(c -> c.type == SGCardType.valueOf(nigiriType[1] + "Nigiri")),
                hand.stream().anyMatch(c -> c.type == SGCardType.valueOf(nigiriType[2] + "Nigiri"))
        };
        boolean[] nigiriInNextHand = {
                nextHand.stream().anyMatch(c -> c.type == SGCardType.valueOf(nigiriType[0] + "Nigiri")),
                nextHand.stream().anyMatch(c -> c.type == SGCardType.valueOf(nigiriType[1] + "Nigiri")),
                nextHand.stream().anyMatch(c -> c.type == SGCardType.valueOf(nigiriType[2] + "Nigiri"))
        };

        int nextPlayerID = (playerID + 1) % sggs.getNPlayers();
        int wasabiPlayed = sggs.getPlayedCardTypes(SGCardType.Wasabi, playerID).getValue();
        int nextPlayerWasabiPlayed = sggs.getPlayedCardTypes(SGCardType.Wasabi, nextPlayerID).getValue();

        features[offset.getAndIncrement()] = wasabiPlayed == 1 ? 1 : 0; // WasabiPlayed1
        features[offset.getAndIncrement()] = wasabiPlayed > 1 ? 1 : 0; // WasabiPlayed2+
        features[offset.getAndIncrement()] = nextPlayerWasabiPlayed >= 1 ? 1 : 0; // NextPlayerWasabiPlayed
        for (int p = 0; p <= 1; p++) {
            for (int i = 0; i < nigiriType.length; i++) {
                boolean wasabi = p == 0 ? wasabiPlayed >= 1 : nextPlayerWasabiPlayed >= 1;
                // {_, Next}WasabiPlayedAnd{nigiriType}NigiriInHand
                features[offset.getAndIncrement()] = nigiriInHand[i] && wasabi ? 1 : 0;
                // {_, Next}WasabiPlayedAnd{nigiriType}NigiriInNextHand
                features[offset.getAndIncrement()] = nigiriInNextHand[i] && wasabi ? 1 : 0;
            }
        }
    }

    void calcChopstickFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        int nextPlayerID = (playerID + 1) % sggs.getNPlayers();
        int chopsticksPlayed = sggs.getPlayedCardTypes(SGCardType.Chopsticks, playerID).getValue();
        boolean nextChopsticksPlayed = sggs.getPlayedCardTypes(SGCardType.Chopsticks, nextPlayerID).getValue() > 0;

        Deck<SGCard> hand = sggs.getPlayerHands().get(playerID);
        List<Integer> nCardsInHand = new LinkedList<Integer>() {
            {
                for (String card : cardsToCount) {
                    if (card.startsWith("Maki")) {
                        int makiValue = Integer.parseInt(card.substring(4));
                        add((int) hand.stream().filter(c -> c.type == SGCardType.Maki && c.count == makiValue).count());
                    } else
                        add((int) hand.stream().filter(c -> c.type == SGCardType.valueOf(card)).count());
                }
            }
        };

        // Size of triangular number of cardsToCount.size(), since order doesn't matter
        boolean[] bothCardsInHand = new boolean[(cardsToCount.size() * (cardsToCount.size() + 1)) / 2];
        int idx = 0;
        for (int i = 0; i < cardsToCount.size(); i++) {
            int nCard1 = nCardsInHand.get(i);
            for (int j = i; j < cardsToCount.size(); j++) {
                int nCard2 = nCardsInHand.get(i);
                bothCardsInHand[idx++] = i == j ? nCard1 >= 2 : nCard1 >= 1 && nCard2 >= 1;
            }
        }

        features[offset.getAndIncrement()] = chopsticksPlayed == 1 ? 1 : 0; // ChopsticksPlayed1
        features[offset.getAndIncrement()] = chopsticksPlayed > 1 ? 1 : 0; // ChopsticksPlayed2+
        features[offset.getAndIncrement()] = nextChopsticksPlayed ? 1 : 0; // NextChopsticksPlayed;
        for (int p = 0; p <= 1; p++) {
            boolean chopPlayed = p == 0 ? chopsticksPlayed > 0 : nextChopsticksPlayed;
            for (int i = 0; i < bothCardsInHand.length; i++) {
                // {_, Next}ChopsticksAnd{card1}{card2}InHand
                features[offset.getAndIncrement()] = chopPlayed && bothCardsInHand[i] ? 1 : 0;
            }
        }
    }

    void calcHandFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        for (int _p = 0; _p <= 1; _p++) {
            int pID = (playerID - _p + sggs.getNPlayers()) % sggs.getNPlayers();
            Deck<SGCard> hand = sggs.getPlayerHands().get(pID);
            for (String cardName : cardsToCount) {
                // {cardName}In{_, Next}Hand
                features[offset.getAndIncrement()] = (cardName.startsWith("Maki"))
                        ? (int) hand.stream()
                                .filter(c -> c.type == SGCardType.Maki
                                        && c.count == Integer.parseInt(cardName.substring(4)))
                                .count()
                        : (int) hand.stream().filter(c -> c.type == SGCardType.valueOf(cardName)).count();
            }
        }
    }

    @Override
    public String[] names() {
        return names;
    }

}
