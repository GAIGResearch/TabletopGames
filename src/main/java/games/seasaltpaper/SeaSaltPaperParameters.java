package games.seasaltpaper;

import core.AbstractParameters;
import games.seasaltpaper.cards.CardColor;
import games.seasaltpaper.cards.CardSuite;
import games.seasaltpaper.cards.CardType;
import utilities.Pair;

import java.util.HashMap;

import static games.seasaltpaper.cards.CardColor.*;
import static games.seasaltpaper.cards.CardSuite.*;

public class SeaSaltPaperParameters extends AbstractParameters {

    public String dataPath = "data/seasaltpaper/";
    int discardPileCount = 2;

    int[] victoryCondition = new int[]{40, 35, 30};

    // TODO make collector bonus of suites that do not have collector card empty array
    public int[] boatCollectorBonus = new int[]{};
    public int[] fishCollectorBonus = new int[]{};
    public int[] shellCollectorBonus = new int[]{0, 2, 4, 6, 8, 10};
    public int[] octopusCollectorBonus = new int[]{0, 3, 6, 9, 12};
    public int[] penguinCollectorBonus = new int[]{1, 3, 5};
    public int[] sailorCollectorBonus = new int[]{0, 5};
    public int[] sharkCollectorBonus = new int[]{};

    public HashMap<CardSuite, int[]> collectorBonusDict = new HashMap<>() {{
        put(BOAT, new int[]{});
        put(FISH, new int[]{});
        put(CRAB, new int[]{});
        put(SWIMMER, new int[]{});
        put(SHARK, new int[]{});
        put(SHELL, new int[]{0, 2, 4, 6, 8, 10});
        put(OCTOPUS, new int[]{0, 3, 6, 9, 12});
        put(PENGUIN, new int[]{1, 3, 5});
        put(SAILOR, new int[]{0, 5});
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
        //TODO last swimmer color? only say 4

        put(new Pair<>(SHELL, CardType.COLLECTOR),
            new Pair<>(6, new CardColor[]{GREEN, GREY, LIGHT_BLUE, BLUE, BLACK, YELLOW})); // Green, Grey, LightBlue, Blue, Black, Yellow
        put(new Pair<>(OCTOPUS, CardType.COLLECTOR),
            new Pair<>(5, new CardColor[]{LIGHT_BLUE, GREEN, GREY, PURPLE, YELLOW})); //Light Blue, Green, Grey, Purple, Yellow
        put(new Pair<>(PENGUIN, CardType.COLLECTOR),
            new Pair<>(3, new CardColor[]{PINK, LIGHT_ORANGE, PURPLE})); //Pink, LightOrange, Purple
        // TODO PINK VS PURPLE???
        put(new Pair<>(SAILOR, CardType.COLLECTOR),
            new Pair<>(2, new CardColor[]{ORANGE, PINK})); // Orange, Pink
        //TODO Orange vs. LightOrange????

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
        return new SeaSaltPaperParameters();
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return false;
    }


}
