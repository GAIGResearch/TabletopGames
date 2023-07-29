package players.rl.featureVectors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateFeatureVector;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import games.sushigo.cards.SGCard.SGCardType;

public class SushiGoFeatureVector implements IStateFeatureVector {

    // Factors to scale down feature values
    private final double MAX_PLAYERS = 5.0;
    private final Map<Integer, Double> MAX_TURNS = Map.of(
            2, 10.0,
            3, 9.0,
            4, 8.0,
            5, 7.0);
    private final double MAX_HAND_SIZE = 10.0;
    private final double HIGH_POINTS = 30.0;
    private final double MAX_PUDDING = 10.0;
    private final double HIGH_MAKI = 10.0;
    private final Map<SGCardType, Double> HIGH_N_PLAYED = Map.of(
            SGCardType.Tempura, 2.0,
            SGCardType.Sashimi, 3.0,
            SGCardType.Dumpling, 5.0,
            SGCardType.Wasabi, 1.0,
            SGCardType.EggNigiri, 2.0,
            SGCardType.SalmonNigiri, 2.0,
            SGCardType.SquidNigiri, 2.0,
            SGCardType.Maki, 3.0,
            SGCardType.Pudding, 4.0,
            SGCardType.Chopsticks, 2.0);

    List<SGCardType> cardTypes = Arrays.asList(SGCardType.values());

    String[] names = new LinkedList<String>() {
        {
            add("nPlayers");
            add("nTurnsLeft");
            add("Points");
            add("PointLeadToSecond");
            add("PointTrailToFirst");
            add("PuddingLeadToSecond");
            add("PuddingLeadToLast");
            add("PuddingTrailToFirst");
            add("PuddingTrailToSecondLast");
            add("MakiLeadToSecond");
            add("MakiLeadToThird");
            add("MakiTrailToFirst");
            add("MakiTrailToSecond");
            add("OneOffTempura");
            add("OneOffSashimi");
            add("TwoOffSashimi");
            add("OpponentsOneOffTempura");
            add("OpponentsOneOffSashimi");
            add("OpponentsTwoOffSashimi");
            cardTypes.forEach(ct -> addToList(this, "nPlayed", ct));
            cardTypes.forEach(ct -> addToList(this, "nNextPlayerPlayed", ct));
            cardTypes.forEach(ct -> addToList(this, "nTotalPlayed", ct));
        }
    }.toArray(String[]::new);

    void addToList(List<String> list, String prefix, SGCardType cardType) {
        switch (cardType) {
            case Maki:
                list.add(prefix + cardType.name() + "1");
                list.add(prefix + cardType.name() + "2");
                list.add(prefix + cardType.name() + "3");
                break;
            default:
                list.add(prefix + cardType.name());
                break;
        }
    }

    public SushiGoFeatureVector() {
    }

    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        double[] features = new double[names.length];
        SGGameState sggs = (SGGameState) state;
        int nextPlayerID = (playerID + 1) % sggs.getNPlayers();

        Deck<SGCard> playerHand = sggs.getPlayerHands().get(playerID);
        Deck<SGCard> playedCards = sggs.getPlayedCards().get(playerID);
        Deck<SGCard> nextPlayerPlayedCards = sggs.getPlayedCards().get(nextPlayerID);
        List<Deck<SGCard>> allPlayedCards = sggs.getPlayedCards();

        double maxTurns = MAX_TURNS.get(sggs.getNPlayers());

        MutableInt f = new MutableInt(0);
        // NOTE: All feature values are scaled down to approximately [-1, 1]. This can
        // vary and some features are only [0, 1], while others can go below -1 or above
        // 1, but they should all be around that order of magnitude.

        // Core Features
        features[f.getAndIncrement()] = state.getNPlayers() / 5.0; // nPlayers
        features[f.getAndIncrement()] = playerHand.getSize() / maxTurns; // nTurnsLeft

        // Special Features
        calculatePointFeatures(sggs, playerID, features, f);
        calculatePuddingAndMakiFeatures(sggs, playerID, features, f);
        calculateUnfinishedSetFeatures(sggs, playerID, features, f);

        // Basic Features
        // nPlayed
        for (SGCardType ct : cardTypes) {
            if (ct == SGCardType.Maki) {
                features[f.getAndIncrement()] = playedCards.stream().filter(c -> c.type == ct && c.count == 1).count()
                        / HIGH_N_PLAYED.get(ct);
                features[f.getAndIncrement()] = playedCards.stream().filter(c -> c.type == ct && c.count == 2).count()
                        / HIGH_N_PLAYED.get(ct);
                features[f.getAndIncrement()] = playedCards.stream().filter(c -> c.type == ct && c.count == 3).count()
                        / HIGH_N_PLAYED.get(ct);
            } else
                features[f.getAndIncrement()] = playedCards.stream().filter(c -> c.type == ct).count()
                        / HIGH_N_PLAYED.get(ct);
        }
        // nNextPlayer
        for (SGCardType ct : cardTypes) {
            if (ct == SGCardType.Maki) {
                features[f.getAndIncrement()] = nextPlayerPlayedCards.stream().filter(c -> c.type == ct && c.count == 1)
                        .count() / HIGH_N_PLAYED.get(ct);
                features[f.getAndIncrement()] = nextPlayerPlayedCards.stream().filter(c -> c.type == ct && c.count == 2)
                        .count() / HIGH_N_PLAYED.get(ct);
                features[f.getAndIncrement()] = nextPlayerPlayedCards.stream().filter(c -> c.type == ct && c.count == 3)
                        .count() / HIGH_N_PLAYED.get(ct);
            } else
                features[f.getAndIncrement()] = sggs.getPlayedCardTypes(ct, nextPlayerID).getValue()
                        / HIGH_N_PLAYED.get(ct);
        }
        // nTotalPlayed
        for (SGCardType ct : cardTypes) {
            if (ct == SGCardType.Maki) {
                features[f.getAndIncrement()] = allPlayedCards.stream()
                        .mapToLong(d -> d.stream().filter(c -> c.type == ct && c.count == 1).count()).sum()
                        / (HIGH_N_PLAYED.get(ct) * 5.0);
                features[f.getAndIncrement()] = allPlayedCards.stream()
                        .mapToLong(d -> d.stream().filter(c -> c.type == ct && c.count == 2).count()).sum()
                        / (HIGH_N_PLAYED.get(ct) * 5.0);
                features[f.getAndIncrement()] = allPlayedCards.stream()
                        .mapToLong(d -> d.stream().filter(c -> c.type == ct && c.count == 3).count()).sum()
                        / (HIGH_N_PLAYED.get(ct) * 5.0);
            } else
                features[f.getAndIncrement()] = allPlayedCards.stream()
                        .mapToLong(d -> d.stream().filter(c -> c.type == ct).count()).sum()
                        / (HIGH_N_PLAYED.get(ct) * 5.0);
        }
        return features;
    }

    void calculatePointFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        double playerScore = sggs.getGameScore(playerID);
        double highestOpponentScore = Double.NEGATIVE_INFINITY;
        for (int p = 0; p < sggs.getNPlayers(); p++) {
            if (p == playerID)
                continue;
            double ps = sggs.getGameScore(p);
            if (ps > highestOpponentScore)
                highestOpponentScore = ps;
        }
        double pointDiffToFirstOpponent = (playerScore - highestOpponentScore) / HIGH_POINTS;
        features[offset.getAndIncrement()] = playerScore / HIGH_POINTS; // Points
        features[offset.getAndIncrement()] = Math.max(0, pointDiffToFirstOpponent); // PointLeadToSecond
        features[offset.getAndIncrement()] = Math.max(0, -pointDiffToFirstOpponent); // pointTrailToFirst
    }

    void calculatePuddingAndMakiFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        int playerPudding = 0;
        int playerMaki = 0;
        int mostPudding = 0;
        int leastPudding = Integer.MAX_VALUE;
        int mostMaki = 0;
        int secondMostMaki = 0;
        for (int p = 0; p < sggs.getNPlayers(); p++) {
            Deck<SGCard> cardsPlayed = sggs.getPlayedCards().get(p);
            int pudding = (int) cardsPlayed.stream().filter(c -> c.type == SGCardType.Pudding).count();
            int maki = cardsPlayed.stream().filter(c -> c.type == SGCardType.Maki).mapToInt(c -> c.count).sum();
            if (p == playerID) {
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
        }

        double puddingDiffToFirstOpponent = (playerPudding - mostPudding) / MAX_PUDDING;
        double puddingDiffToLastOpponent = (sggs.getNPlayers() > 2) ? (playerPudding - leastPudding) / MAX_PUDDING : 0;
        double makiDiffToFirstOpponent = (playerMaki - mostMaki) / HIGH_MAKI;
        double makiDiffToSecondOpponent = (sggs.getNPlayers() > 2) ? (playerMaki - secondMostMaki) / HIGH_MAKI : 0;

        features[offset.getAndIncrement()] = Math.max(0, puddingDiffToFirstOpponent); // PuddingLeadToSecond
        features[offset.getAndIncrement()] = Math.max(0, puddingDiffToLastOpponent); // PuddingLeadToLast
        features[offset.getAndIncrement()] = Math.max(0, -puddingDiffToFirstOpponent); // PuddingTrailToFirst
        features[offset.getAndIncrement()] = Math.max(0, -puddingDiffToLastOpponent); // PuddingTrailToLast
        features[offset.getAndIncrement()] = Math.max(0, makiDiffToFirstOpponent); // MakiLeadToSecond
        features[offset.getAndIncrement()] = Math.max(0, makiDiffToSecondOpponent); // MakiLeadToThird
        features[offset.getAndIncrement()] = Math.max(0, -makiDiffToFirstOpponent); // MakiTrailToFirst
        features[offset.getAndIncrement()] = Math.max(0, -makiDiffToSecondOpponent); // MakiTrailToSecond
    }

    void calculateUnfinishedSetFeatures(SGGameState sggs, int playerID, double[] features, MutableInt offset) {
        int oneOffTempura = 0;
        int oneOffSashimi = 0;
        int twoOffSashimi = 0;
        int opponentsOneOffTempura = 0;
        int opponentsOneOffSashimi = 0;
        int opponentsTwoOffSashimi = 0;
        for (int p = 0; p < sggs.getNPlayers(); p++) {
            Deck<SGCard> cardsPlayed = sggs.getPlayedCards().get(p);
            int nUnfinishedTempura = (int) (cardsPlayed.stream().filter(c -> c.type == SGCardType.Tempura).count() % 2);
            int nUnfinishedSashimi = (int) (cardsPlayed.stream().filter(c -> c.type == SGCardType.Sashimi).count() % 3);
            if (p == playerID) {
                oneOffTempura = nUnfinishedTempura;
                oneOffSashimi = nUnfinishedSashimi / 2;
                twoOffSashimi = nUnfinishedSashimi % 2;
            } else {
                opponentsOneOffTempura += nUnfinishedTempura;
                opponentsOneOffSashimi += nUnfinishedSashimi / 2;
                opponentsTwoOffSashimi += nUnfinishedSashimi % 2;
            }
        }
        features[offset.getAndIncrement()] = oneOffTempura;
        features[offset.getAndIncrement()] = oneOffSashimi;
        features[offset.getAndIncrement()] = twoOffSashimi;
        features[offset.getAndIncrement()] = opponentsOneOffTempura / MAX_PLAYERS; // Divide by 5 since max 5 players
        features[offset.getAndIncrement()] = opponentsOneOffSashimi / MAX_PLAYERS;
        features[offset.getAndIncrement()] = opponentsTwoOffSashimi / MAX_PLAYERS;
    }

    @Override
    public String[] names() {
        return names;
    }

}
