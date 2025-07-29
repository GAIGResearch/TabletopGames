package games.loveletter.features;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import evaluation.features.TunableStateFeatures;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static games.loveletter.cards.CardType.*;

/**
 * A set of features designed to tie in exactly with those used in LoveLetterHeuristic
 */
public class LLStateFeaturesTunable extends TunableStateFeatures {

    static String[] allNames = new String[]{"CARDS", "AFFECTION", "COUNTESS", "BARON",
            "GUARD", "HANDMAID", "KING", "PRIEST", "PRINCE", "PRINCESS", "HIDDEN",
            "ADVANTAGE", "TURN", "ROUND",
            "KNOWLEDGE", "PROTECTION",
            "GUARD_DISCARD", "HANDMAID_DISCARD", "PRIEST_DISCARD", "BARON_DISCARD", "PRINCE_DISCARD",
            "KING_DISCARD", "COUNTESS_DISCARD", "PRINCESS_DISCARD"};

    public LLStateFeaturesTunable() {
        super(allNames);
    }

    @Override
    public double[] fullFeatureVector(AbstractGameState gs, int playerId) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;

        double[] data = new double[allNames.length];

        double cardValues = 0;

        Set<CardType> cardTypes = new HashSet<>();
        if (active[0]) {
            for (LoveLetterCard card : llgs.getPlayerHandCards().get(playerId).getComponents()) {
                cardValues += card.cardType.getValue();
                cardTypes.add(card.cardType);
            }
            data[0] = cardValues;
        }

        data[1] = llgs.getGameScore(playerId);

        if (cardTypes.contains(Countess)) data[2] = 1.0;
        if (cardTypes.contains(King)) data[6] = 1.0;
        if (cardTypes.contains(Baron)) data[3] = 1.0;
        if (cardTypes.contains(Handmaid)) data[5] = 1.0;
        if (cardTypes.contains(Guard)) data[4] = 1.0;
        if (cardTypes.contains(Priest)) data[7] = 1.0;
        if (cardTypes.contains(Prince)) data[8] = 1.0;
        if (cardTypes.contains(Princess)) data[9] = 1.0;

        if (active[10]) {
            int visibleCards = 0;
            for (int player = 0; player < llgs.getNPlayers(); player++) {
                if (player != playerId) {
                    PartialObservableDeck<LoveLetterCard> deck = llgs.getPlayerHandCards().get(player);
                    visibleCards += (int) IntStream.range(0, deck.getSize()).filter(i -> deck.getVisibilityForPlayer(i, playerId)).count();
                }
            }
            data[10] = visibleCards;
        }

        if (active[11]) {
            int maxOtherScore = IntStream.range(0, llgs.getNPlayers())
                    .filter(p -> p != playerId)
                    .map(p -> (int) llgs.getGameScore(p)).max().orElseThrow(() -> new AssertionError("??"));
            data[11] = llgs.getGameScore(playerId) - maxOtherScore;
        }
        data[12] = llgs.getTurnCounter();
        data[13] = llgs.getRoundCounter();
        // Knowledge - known player identities
        if (active[14]) {
            int value = 0;
            for (int p = 0; p < llgs.getNPlayers(); p++) {
                if (p != playerId && llgs.isNotTerminalForPlayer(p)) {
                    PartialObservableDeck<LoveLetterCard> hand = llgs.getPlayerHandCards().get(p);
                    if (hand.getSize() > 0 && hand.getVisibilityForPlayer(0, playerId)) {
                        value += 1;
                    }
                }
            }
            data[14] = value;
        }
        // Protection - whether the player is protected
        if (active[15]) {
            data[15] = IntStream.range(0, llgs.getNPlayers()).map(p -> llgs.isProtected(p) ? (int) Math.pow(2, p) : 0).sum();
        }
        // Discard piles
        if (active[16])
            data[16] = llgs.getPlayerDiscardCards().stream().flatMap(deck -> deck.getComponents().stream())
                    .filter(card -> card.cardType == Guard).count();
        if (active[17])
            data[17] = llgs.getPlayerDiscardCards().stream().flatMap(deck -> deck.getComponents().stream())
                    .filter(card -> card.cardType == Handmaid).count();
        if (active[18])
            data[18] = llgs.getPlayerDiscardCards().stream().flatMap(deck -> deck.getComponents().stream())
                    .filter(card -> card.cardType == Priest).count();
        if (active[19])
            data[19] = llgs.getPlayerDiscardCards().stream().flatMap(deck -> deck.getComponents().stream())
                    .filter(card -> card.cardType == Baron).count();
        if (active[20])
            data[20] = llgs.getPlayerDiscardCards().stream().flatMap(deck -> deck.getComponents().stream())
                    .filter(card -> card.cardType == Prince).count();
        if (active[21])
            data[21] = llgs.getPlayerDiscardCards().stream().flatMap(deck -> deck.getComponents().stream())
                    .filter(card -> card.cardType == King).count();
        if (active[22])
            data[22] = llgs.getPlayerDiscardCards().stream().flatMap(deck -> deck.getComponents().stream())
                    .filter(card -> card.cardType == Countess).count();
        if (active[23])
            data[23] = llgs.getPlayerDiscardCards().stream().flatMap(deck -> deck.getComponents().stream())
                    .filter(card -> card.cardType == Princess).count();

        return data;
    }

    @Override
    protected LLStateFeaturesTunable _copy() {
        return new LLStateFeaturesTunable();
        // setting of values is done in TunableParameters
    }

}
