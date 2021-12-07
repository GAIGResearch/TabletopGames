package games.dicemonastery;

import core.AbstractGameData;
import core.components.*;

import java.util.*;

public class DiceMonasteryData extends AbstractGameData {

    List<Deck<Card>> decks;

    @Override
    public void load(String dataPath) {
        decks = Deck.loadDecksOfCards(dataPath + "decks.json");
    }
}
