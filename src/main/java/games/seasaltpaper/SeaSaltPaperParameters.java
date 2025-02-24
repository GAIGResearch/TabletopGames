package games.seasaltpaper;

import core.AbstractParameters;
import games.seasaltpaper.cards.CardColor;
import games.seasaltpaper.cards.CardSuite;
import games.seasaltpaper.cards.CardType;
import utilities.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import static games.seasaltpaper.cards.CardColor.*;
import static games.seasaltpaper.cards.CardSuite.*;

public class SeaSaltPaperParameters extends AbstractParameters {

    public String dataPath = "data/seasaltpaper/";
    public boolean individualVisibility = false;    // using individual card visibility for gameState copying
    public int discardPileCount = 2;
    public int numberOfCardsDraw = 2;
    public int numberOfCardsDiscard = 1;

    public int[] victoryCondition = new int[]{40, 35, 30}; // for 2, 3, 4 players
    int roundStopCondition = 7;

    // CollectorBonus = (base, increment)
    // collector score = base + increment * (count - 1)
    public HashMap<CardSuite, Pair<Integer, Integer>> collectorBonusDict = new HashMap<>() {{
        put(BOAT, new Pair<>(0, 0));
        put(FISH, new Pair<>(0, 0));
        put(CRAB, new Pair<>(0, 0));
        put(SWIMMER, new Pair<>(0, 0));
        put(SHARK, new Pair<>(0, 0));
        put(SHELL, new Pair<>(0, 2));
        put(OCTOPUS, new Pair<>(0, 3));
        put(PENGUIN, new Pair<>(1, 2));
        put(SAILOR, new Pair<>(0, 5));
    }};

    public HashMap<CardSuite, Integer> duoBonusDict = new HashMap<>() {{
        put(BOAT, 1);
        put(FISH, 1);
        put(CRAB, 1);
        put(SWIMMER, 1);
        put(SHARK, 1);
        put(SHELL, 1);
        put(OCTOPUS, 1);
        put(PENGUIN, 1);
        put(SAILOR, 1);
    }};

    public HashMap<CardSuite, Integer> multiplierDict = new HashMap<>() {{
        put(BOAT, 1);
        put(FISH, 1);
        put(CRAB, 1);
        put(SWIMMER, 0);
        put(SHARK, 0);
        put(SHELL, 0);
        put(OCTOPUS, 0);
        put(PENGUIN, 2);
        put(SAILOR, 3);
    }};


    public HashMap<Pair<CardSuite, CardType>, Pair<Integer, CardColor[]>> cardsInit = new HashMap<>() {{
        put(new Pair<>(CRAB, CardType.DUO),
            new Pair<>(9, new CardColor[]{LIGHT_BLUE, LIGHT_BLUE, BLUE, BLUE, YELLOW, YELLOW, GREEN, GREY, BLACK})); // LightBlue (x2), Blue (x2), Yellow (x2), Green, Grey, Black
        put(new Pair<>(BOAT, CardType.DUO),
            new Pair<>(8, new CardColor[]{LIGHT_BLUE, LIGHT_BLUE, BLUE, BLUE, YELLOW, YELLOW, BLACK, BLACK})); // LightBlue (x2), Blue (x2), Yellow (x2), Black (x2)
        put(new Pair<>(FISH, CardType.DUO),
            new Pair<>(7, new CardColor[]{BLUE, BLUE, BLACK, BLACK, YELLOW, LIGHT_BLUE, GREEN})); // Blue (x2), Black (x2), Yellow, LightBlue, Green
        put(new Pair<>(SHARK, CardType.DUO),
            new Pair<>(5, new CardColor[]{LIGHT_BLUE, BLUE, BLACK, GREEN, PURPLE}));// Light Blue, Blue, Black, Green, Purple
        put(new Pair<>(SWIMMER, CardType.DUO),
            new Pair<>(5, new CardColor[]{LIGHT_BLUE, LIGHT_BLUE, BLUE, YELLOW, LIGHT_ORANGE}));// Light Blue, Blue, Yellow, LightOrange - 5 total.

        put(new Pair<>(SHELL, CardType.COLLECTOR),
            new Pair<>(6, new CardColor[]{GREEN, GREY, LIGHT_BLUE, BLUE, BLACK, YELLOW})); // Green, Grey, LightBlue, Blue, Black, Yellow
        put(new Pair<>(OCTOPUS, CardType.COLLECTOR),
            new Pair<>(5, new CardColor[]{LIGHT_BLUE, GREEN, GREY, PURPLE, YELLOW})); //Light Blue, Green, Grey, Purple, Yellow
        put(new Pair<>(PENGUIN, CardType.COLLECTOR),
            new Pair<>(3, new CardColor[]{PINK, LIGHT_ORANGE, PURPLE})); //Pink, LightOrange, Purple
        put(new Pair<>(SAILOR, CardType.COLLECTOR),
            new Pair<>(2, new CardColor[]{ORANGE, PINK})); // Orange, Pink

        put(new Pair<>(BOAT, CardType.MULTIPLIER),
            new Pair<>(1, new CardColor[]{PURPLE})); //Purple
        put(new Pair<>(FISH, CardType.MULTIPLIER),
            new Pair<>(1, new CardColor[]{GREY})); //Grey
        put(new Pair<>(PENGUIN, CardType.MULTIPLIER),
            new Pair<>(1, new CardColor[]{GREEN})); //Green
        put(new Pair<>(SAILOR, CardType.MULTIPLIER),
            new Pair<>(1, new CardColor[]{LIGHT_ORANGE})); //LightOrange

        put(new Pair<>(MERMAID, CardType.MERMAID),
            new Pair<>(4, new CardColor[]{WHITE, WHITE, WHITE, WHITE}));// LightGrey

    }};

    public SeaSaltPaperParameters()
    {

    }

    @Override
    protected AbstractParameters _copy() {
        SeaSaltPaperParameters p = new SeaSaltPaperParameters();
        p.dataPath = dataPath;
        p.discardPileCount = discardPileCount;
        p.individualVisibility = individualVisibility;
        p.numberOfCardsDraw = numberOfCardsDraw;
        p.numberOfCardsDiscard = numberOfCardsDiscard;

        p.victoryCondition = victoryCondition.clone();

        p.collectorBonusDict = new HashMap<>(collectorBonusDict);
        p.duoBonusDict = new HashMap<>(duoBonusDict);
        p.multiplierDict = new HashMap<>(multiplierDict);
        p.cardsInit = new HashMap<>(cardsInit);

        return p;
    }

    @Override
    public boolean _equals(Object o) {
        return equals(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SeaSaltPaperParameters that = (SeaSaltPaperParameters) o;
        return numberOfCardsDraw == that.numberOfCardsDraw && numberOfCardsDiscard == that.numberOfCardsDiscard && individualVisibility == that.individualVisibility && discardPileCount == that.discardPileCount && Objects.equals(dataPath, that.dataPath) && Arrays.equals(victoryCondition, that.victoryCondition)
                && Objects.equals(collectorBonusDict, that.collectorBonusDict) && Objects.equals(duoBonusDict, that.duoBonusDict) && Objects.equals(multiplierDict, that.multiplierDict) && Objects.equals(cardsInit, that.cardsInit);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), numberOfCardsDraw, numberOfCardsDiscard, individualVisibility, dataPath, discardPileCount, collectorBonusDict, duoBonusDict, multiplierDict, cardsInit);
        result = 31 * result + Arrays.hashCode(victoryCondition);
        return result;
    }
}
