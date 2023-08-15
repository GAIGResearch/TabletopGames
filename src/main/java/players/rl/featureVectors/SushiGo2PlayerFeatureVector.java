package players.rl.featureVectors;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.mutable.MutableInt;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IActionFeatureVector;
import games.sushigo.SGGameState;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import games.sushigo.cards.SGCard.SGCardType;

public class SushiGo2PlayerFeatureVector implements IActionFeatureVector {

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
            add("PointLead");
            add("PointTrail");

            // Pudding Features
            add("PuddingLead");
            add("PuddingTrail");

            // Maki Features
            add("MakiLead");
            add("MakiTrail");

            // Tempura & Sashimi Features
            for (String prefix : Arrays.asList("", "Next")) {
                for (String postfix : Arrays.asList("", "AndInHand", "AndInNextHand",
                        "AndInHandAndInNextHand")) {
                    add(prefix + "OneOffTempura" + postfix);
                    add(prefix + "OneOffSashimi" + postfix);
                    add(prefix + "TwoOffSashimi" + postfix);
                }
            }

            // Dumpling Features
            for (String prefix : Arrays.asList("", "Next")) {
                for (String postfix : Arrays.asList("", "AndInHand", "AndInNextHand",
                        "AndInHandAndInNextHand")) {
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
                add("Next" + cardName + "InHand");
        }
    }.toArray(String[]::new);

    @Override
    public double[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        Random rng = new Random(state.getGameParameters().getRandomSeed());
        double[] features = new double[names.length];
        SGGameState sggs = (SGGameState) state.copy(playerID);

        // Update hand and played lists with the chosen card
        // Remove random card from every other players hand
        for (int p = 0; p < sggs.getNPlayers(); p++) {
            int cardIdx = p == playerID ? ((ChooseCard) action).cardIdx
                    : rng.nextInt(sggs.getPlayerHands().get(p).getSize());
            SGCard card = sggs.getPlayerHands().get(p).get(cardIdx);
            sggs.getPlayerHands().get(p).remove(cardIdx);
            sggs.getPlayedCardTypes(card.type, p).increment(card.count);

            // Handle special cases
            if (Arrays.asList(SGCardType.SquidNigiri, SGCardType.SalmonNigiri, SGCardType.EggNigiri)
                    .contains(card.type))
                sggs.getPlayedCardTypes(SGCardType.Wasabi, p).decrement();
            if (((ChooseCard) action).useChopsticks) {
                sggs.getPlayedCardTypes(SGCardType.Chopsticks, p).decrement();
                sggs.getPlayerHands().get(p).add(new SGCard(SGCardType.Chopsticks));
            }
        }

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

        double playerScore = sggs.getGameScore(playerID);
        double nextScore = sggs.getGameScore(nextPlayerID);
        double pointDiffToNext = (playerScore - nextScore) / HIGH_POINTS;

        features[offset.getAndIncrement()] = playerScore / HIGH_POINTS; // Points
        features[offset.getAndIncrement()] = Math.max(0, pointDiffToNext); // PointLeadToNext
        features[offset.getAndIncrement()] = Math.max(0, -pointDiffToNext); // PointTrailToNext
    }

    void calcPuddingMakiFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        int nextPlayerID = (playerID + 1) % sggs.getNPlayers();

        int pudding = sggs.getPlayedCardTypes(SGCardType.Pudding, playerID).getValue();
        int nextPudding = sggs.getPlayedCardTypes(SGCardType.Pudding, nextPlayerID).getValue();
        int maki = sggs.getPlayedCardTypes(SGCardType.Maki, playerID).getValue();
        int nextMaki = sggs.getPlayedCardTypes(SGCardType.Maki, nextPlayerID).getValue();

        double puddingDiffToNext = (pudding - nextPudding) / MAX_PUDDING;
        double makiDiffToNext = (maki - nextMaki) / HIGH_MAKI;

        // Pudding Features
        features[offset.getAndIncrement()] = Math.max(0, puddingDiffToNext); // PuddingLeadToNext
        features[offset.getAndIncrement()] = Math.max(0, -puddingDiffToNext); // PuddingTrailToNext
        // Maki Features
        features[offset.getAndIncrement()] = Math.max(0, makiDiffToNext); // MakiLeadToNext
        features[offset.getAndIncrement()] = Math.max(0, -makiDiffToNext); // MakiTrailToNext
    }

    void calcTempuraSashimiFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        // Here, playerID and previousPlayerID are seemingly swapped in the
        // getPlayerHands() functions, this is because we are interested in the hands
        // after(!) playing the card, which will be swapped.
        int prevPlayerID = (playerID - 1 + sggs.getNPlayers()) % sggs.getNPlayers();
        int prevPrevPlayerID = (playerID - 2 + sggs.getNPlayers()) % sggs.getNPlayers(); // = playerID in 2-player
        boolean tempuraInHand = sggs.getPlayerHands().get(prevPlayerID).stream()
                .anyMatch(c -> c.type == SGCardType.Tempura);
        boolean tempuraInNextHand = sggs.getPlayerHands().get(prevPrevPlayerID).stream()
                .anyMatch(c -> c.type == SGCardType.Tempura);
        boolean sashimiInHand = sggs.getPlayerHands().get(prevPlayerID).stream()
                .anyMatch(c -> c.type == SGCardType.Sashimi);
        boolean sashimiInNextHand = sggs.getPlayerHands().get(prevPrevPlayerID).stream()
                .anyMatch(c -> c.type == SGCardType.Sashimi);

        for (int _p = 0; _p <= 1; _p++) {
            int pID = (playerID + _p) % sggs.getNPlayers();
            int nUnfinishedTempura = sggs.getPlayedCardTypes(SGCardType.Tempura, pID).getValue() % 2;
            int nUnfinishedSashimi = sggs.getPlayedCardTypes(SGCardType.Sashimi, pID).getValue() % 3;

            boolean oneOffTempura = nUnfinishedTempura == 1;
            boolean oneOffSashimi = nUnfinishedSashimi == 2;
            boolean twoOffSashimi = nUnfinishedSashimi == 1;
            for (boolean isHot : new boolean[] {
                    oneOffTempura, // {_, Next}OneOffTempura
                    oneOffSashimi, // {_, Next}OneOffSashimi
                    twoOffSashimi, // {_, Next}TwoOffSashimi
                    oneOffTempura && tempuraInHand, // {_, Next}OneOffTempuraAndInHand
                    oneOffSashimi && sashimiInHand, // {_, Next}OneOffSashimiAndInHand
                    twoOffSashimi && sashimiInHand, // {_, Next}TwoOffSashimiAndInHand
                    oneOffTempura && tempuraInNextHand, // {_, Next}OneOffTempuraAndInNextHand
                    oneOffSashimi && sashimiInNextHand, // {_, Next}OneOffSashimiAndInNextHand
                    twoOffSashimi && sashimiInNextHand, // {_, Next}TwoOffSashimiAndInNextHand
                    oneOffTempura && tempuraInHand && tempuraInNextHand, // {...}OneOffTempuraAndInHandAndInNextHand
                    oneOffSashimi && sashimiInHand && sashimiInNextHand, // {...}OneOffSashimiAndInHandAndInNextHand
                    twoOffSashimi && sashimiInHand && sashimiInNextHand, // {...}TwoOffSashimiAndInHandAndInNextHand
            }) {
                features[offset.getAndIncrement()] = isHot ? 1 : 0;
            }
        }
    }

    void calcDumplingFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        // Here, playerID and previousPlayerID are seemingly swapped in the
        // getPlayerHands() functions, this is because we are interested in the hands
        // after(!) playing the card, which will be swapped.
        int prevPlayerID = (playerID - 1 + sggs.getNPlayers()) % sggs.getNPlayers();
        int prevPrevPlayerID = (playerID - 2 + sggs.getNPlayers()) % sggs.getNPlayers(); // = playerID in 2-player
        boolean dumplingInHand = sggs.getPlayerHands().get(prevPlayerID).stream()
                .anyMatch(c -> c.type == SGCardType.Dumpling);
        boolean dumplingInNextHand = sggs.getPlayerHands().get(prevPrevPlayerID).stream()
                .anyMatch(c -> c.type == SGCardType.Dumpling);

        for (int _p = 0; _p <= 1; _p++) {
            int pID = (playerID + _p) % sggs.getNPlayers();
            int nDumplingsPlayed = sggs.getPlayedCardTypes(SGCardType.Dumpling, pID).getValue();

            boolean[] dumplingsPlayed = new boolean[4];
            boolean[] dumplingsPlayedAndInHand = new boolean[4];
            boolean[] dumplingsPlayedAndInNextHand = new boolean[4];
            boolean[] dumplingsPlayedAndInHandAndInNextHand = new boolean[4];
            for (int i = 0; i < dumplingsPlayed.length; i++) {
                int nDumplings = i + 1;
                dumplingsPlayed[i] = nDumplingsPlayed == nDumplings;
                dumplingsPlayedAndInHand[i] = dumplingsPlayed[i] && dumplingInHand;
                dumplingsPlayedAndInNextHand[i] = dumplingsPlayed[i] && dumplingInNextHand;
                dumplingsPlayedAndInHandAndInNextHand[i] = dumplingsPlayed[i] && dumplingInHand && dumplingInNextHand;
            }
            for (boolean isHot : dumplingsPlayed)
                features[offset.getAndIncrement()] = isHot ? 1 : 0; // {_, Next}DumplingsPlayed{i}
            for (boolean isHot : dumplingsPlayedAndInHand)
                features[offset.getAndIncrement()] = isHot ? 1 : 0; // {_, Next}DumplingsPlayed{i}AndInHand
            for (boolean isHot : dumplingsPlayedAndInNextHand)
                features[offset.getAndIncrement()] = isHot ? 1 : 0; // {_, Next}DumplingsPlayed{i}AndInNextHand
            for (boolean isHot : dumplingsPlayedAndInHandAndInNextHand)
                features[offset.getAndIncrement()] = isHot ? 1 : 0; // {_, Next}DumplingsPlayed{i}AndInHandAndInNextHand
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
        int prevPlayerID = (playerID - 1 + sggs.getNPlayers()) % sggs.getNPlayers();
        int chopsticksPlayed = sggs.getPlayedCardTypes(SGCardType.Chopsticks, playerID).getValue();
        boolean nextChopsticksPlayed = sggs.getPlayedCardTypes(SGCardType.Chopsticks, nextPlayerID).getValue() > 0;

        // Here, we use the next playerID, since we are interested in the hand after(!)
        // playing the card, which is the next player's current hand
        Deck<SGCard> hand = sggs.getPlayerHands().get(prevPlayerID);
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
                int nCard2 = nCardsInHand.get(j);
                bothCardsInHand[idx++] = i == j ? nCard1 >= 2 : nCard1 >= 1 && nCard2 >= 1;
            }
        }

        features[offset.getAndIncrement()] = chopsticksPlayed == 1 ? 1 : 0; // ChopsticksPlayed1
        features[offset.getAndIncrement()] = chopsticksPlayed > 1 ? 1 : 0; // ChopsticksPlayed2+
        features[offset.getAndIncrement()] = nextChopsticksPlayed ? 1 : 0; // NextChopsticksPlayed;
        for (int p = 0; p <= 1; p++) {
            boolean chopPlayed = p == 0 ? chopsticksPlayed > 0 : nextChopsticksPlayed;
            for (boolean isHot : bothCardsInHand) {
                // {_, Next}ChopsticksAnd{card1}{card2}InHand
                features[offset.getAndIncrement()] = chopPlayed && isHot ? 1 : 0;
            }
        }
    }

    void calcHandFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        for (int _p = 0; _p <= 1; _p++) {
            int pID = (playerID - _p - 1 + sggs.getNPlayers()) % sggs.getNPlayers();
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
