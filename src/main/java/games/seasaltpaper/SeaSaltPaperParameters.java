package games.seasaltpaper;

import core.AbstractParameters;
import games.loveletter.LoveLetterParameters;
import games.seasaltpaper.cards.CardSuite;
import games.seasaltpaper.cards.CardType;
import utilities.Pair;

import java.util.Arrays;
import java.util.HashMap;

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


    public HashMap<Pair<CardSuite, CardType>, Integer> nCardsPerType = new HashMap<>() {{
        put(new Pair<>(CardSuite.SHELL, CardType.DUO), 9); // LightBlue (x2), Blue (x2), Yellow (x2), Green, Grey, Black - This is CRAB.
        put(new Pair<>(CardSuite.BOAT, CardType.DUO), 8); // LightBlue (x2), Blue (x2), Yellow (x2), Black (x2)
        put(new Pair<>(CardSuite.FISH, CardType.DUO), 7); // Blue (x2), Black (x2), Yellow, LightBlue, Green
        put(new Pair<>(CardSuite.SHARK, CardType.DUO), 10);// Light Blue, Blue, Black, Green, Purple

        // You actually need Swimmers, I think
        //put(new Pair<>(CardSuite.SWIMMER, CardType.DUO), 5);// Light Blue, Blue, Yellow, LightOrange - 5 total.

        put(new Pair<>(CardSuite.SHELL, CardType.COLLECTOR), 6); // Green, Grey, LightBlue, Blue, Black, Yellow
        put(new Pair<>(CardSuite.OCTOPUS, CardType.COLLECTOR), 5); //Light Blue, Green, Grey, Purple, Yellow
        put(new Pair<>(CardSuite.PENGUIN, CardType.COLLECTOR), 3); //Pink, LightOrange, Purple
        put(new Pair<>(CardSuite.SAILOR, CardType.COLLECTOR), 2); // Orange, Pink

        put(new Pair<>(CardSuite.BOAT, CardType.MULTIPLIER), 1); //Purple
        put(new Pair<>(CardSuite.FISH, CardType.MULTIPLIER), 1); //Grey
        put(new Pair<>(CardSuite.PENGUIN, CardType.MULTIPLIER), 1); //Green
        put(new Pair<>(CardSuite.SAILOR, CardType.MULTIPLIER), 1); //LightOrange
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
