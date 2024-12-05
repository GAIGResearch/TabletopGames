package games.seasaltpaper;

import core.AbstractParameters;
import games.loveletter.LoveLetterParameters;
import games.seasaltpaper.cards.CardColor;
import games.seasaltpaper.cards.CardSuite;
import games.seasaltpaper.cards.CardType;
import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter;
import utilities.Pair;

import java.util.Arrays;
import java.util.HashMap;

import static games.seasaltpaper.cards.CardColor.*;

public class SeaSaltPaperParameters extends AbstractParameters {

    public String dataPath = "data/seasaltpaper/";
    int discardPileCount = 2;

    // TODO make collector bonus of suites that do not have collector card empty array
    public int[] boatCollectorBonus = new int[]{};
    public int[] fishCollectorBonus = new int[]{};
    public int[] shellCollectorBonus = new int[]{0, 2, 4, 6, 8, 10};
    public int[] octopusCollectorBonus = new int[]{0, 3, 6, 9, 12};
    public int[] penguinCollectorBonus = new int[]{1, 3, 5};
    public int[] sailorCollectorBonus = new int[]{0, 5};
    public int[] sharkCollectorBonus = new int[]{};


    public HashMap<Pair<CardSuite, CardType>, Pair<Integer, CardColor[]>> cardsInit = new HashMap<>() {{
        put(new Pair<>(CardSuite.CRAB, CardType.DUO),
            new Pair<>(9, new CardColor[]{LIGHT_BLUE, LIGHT_BLUE, BLUE, BLUE, YELLOW, YELLOW, GREEN, GREY, BLACK})); // LightBlue (x2), Blue (x2), Yellow (x2), Green, Grey, Black
        put(new Pair<>(CardSuite.BOAT, CardType.DUO),
            new Pair<>(8, new CardColor[]{LIGHT_BLUE, LIGHT_BLUE, BLUE, BLUE, YELLOW, YELLOW, BLACK, BLACK})); // LightBlue (x2), Blue (x2), Yellow (x2), Black (x2)
        put(new Pair<>(CardSuite.FISH, CardType.DUO),
            new Pair<>(7, new CardColor[]{BLUE, BLUE, BLACK, BLACK, YELLOW, LIGHT_BLUE, GREEN})); // Blue (x2), Black (x2), Yellow, LightBlue, Green
        put(new Pair<>(CardSuite.SHARK, CardType.DUO),
            new Pair<>(5, new CardColor[]{LIGHT_BLUE, BLUE, BLACK, GREEN, PURPLE}));// Light Blue, Blue, Black, Green, Purple
        put(new Pair<>(CardSuite.SWIMMER, CardType.DUO),
            new Pair<>(5, new CardColor[]{LIGHT_BLUE, LIGHT_BLUE, BLUE, YELLOW, ORANGE}));// Light Blue, Blue, Yellow, LightOrange - 5 total.
        //TODO last swimmer color? only say 4

        put(new Pair<>(CardSuite.SHELL, CardType.COLLECTOR),
            new Pair<>(6, new CardColor[]{GREEN, GREY, LIGHT_BLUE, BLUE, BLACK, YELLOW})); // Green, Grey, LightBlue, Blue, Black, Yellow
        put(new Pair<>(CardSuite.OCTOPUS, CardType.COLLECTOR),
            new Pair<>(5, new CardColor[]{LIGHT_BLUE, GREEN, GREY, PURPLE, YELLOW})); //Light Blue, Green, Grey, Purple, Yellow
        put(new Pair<>(CardSuite.PENGUIN, CardType.COLLECTOR),
            new Pair<>(3, new CardColor[]{PINK, ORANGE, PURPLE})); //Pink, LightOrange, Purple
        // TODO PINK VS PURPLE???
        put(new Pair<>(CardSuite.SAILOR, CardType.COLLECTOR),
            new Pair<>(2, new CardColor[]{ORANGE, PINK})); // Orange, Pink
        //TODO Orange vs. LightOrange????

        put(new Pair<>(CardSuite.BOAT, CardType.MULTIPLIER),
            new Pair<>(1, new CardColor[]{PURPLE})); //Purple
        put(new Pair<>(CardSuite.FISH, CardType.MULTIPLIER),
            new Pair<>(1, new CardColor[]{GREY})); //Grey
        put(new Pair<>(CardSuite.PENGUIN, CardType.MULTIPLIER),
            new Pair<>(1, new CardColor[]{GREEN})); //Green
        put(new Pair<>(CardSuite.SAILOR, CardType.MULTIPLIER),
            new Pair<>(1, new CardColor[]{ORANGE})); //LightOrange

        put(new Pair<>(CardSuite.MERMAID, CardType.MERMAID),
            new Pair<>(4, new CardColor[]{WHITE, WHITE, WHITE, WHITE,}));// LightGrey

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
