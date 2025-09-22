package games.descent2e.components.cards;

import core.CoreConstants;
import core.components.Card;
import core.components.Deck;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;

import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

public class ShopCard extends Card {

    private int act;
    private int value;
    private String equipmentType;

    public ShopCard() {
        super("");
    }

    protected ShopCard(String componentName, int componentID) {
        super(componentName, componentID);
    }

    public static Pair<Deck<Card>, Deck<Card>> loadCards(String filename) {
        JSONParser jsonParser = new JSONParser();
        Deck<Card> act1Deck = new Deck<>("Act 1 Shop Deck", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        Deck<Card> act2Deck = new Deck<>("Act 2 Shop Deck", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                ShopCard card = new ShopCard();
                card.loadCard((JSONObject) o);
                if (card.act == 1)
                    act1Deck.add(card);
                else
                    act2Deck.add(card);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return new Pair<>(act1Deck, act2Deck);

    }

    public void loadCard(JSONObject card) {
        this.componentName = ((String) ( (JSONArray) card.get("name")).get(1));
        this.act = ((Long) ( (JSONArray) card.get("act")).get(1)).intValue();
        this.value = ((Long) ( (JSONArray) card.get("cost")).get(1)).intValue();
        parseComponent(this, card);
    }

    public int getAct() {
        return act;
    }

    public int getValue() {
        return value;
    }

    @Override
    public ShopCard copy() {
        ShopCard card = new ShopCard(componentName, componentID);
        card.act = act;
        card.value = value;
        card.equipmentType = equipmentType;
        copyComponentTo(card);
        return card;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShopCard that)) return false;
        if (!super.equals(o)) return false;
        return value == that.value && Objects.equals(equipmentType, that.equipmentType) &&
                act == that.act;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), act, equipmentType, value);
    }
}
