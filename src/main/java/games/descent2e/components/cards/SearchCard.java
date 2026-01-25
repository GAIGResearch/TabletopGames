package games.descent2e.components.cards;

import core.CoreConstants;
import core.components.Card;
import core.components.Deck;
import core.components.Dice;
import games.descent2e.actions.tokens.SearchAction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

public class SearchCard extends Card {

    private String itemType;
    private int value;

    public SearchCard() {
        super("");
    }

    protected SearchCard(String componentName, int componentID) {
        super(componentName, componentID);
    }

    public static Deck<Card> loadCards(String filename) {
        JSONParser jsonParser = new JSONParser();
        Deck<Card> cardDeck = new Deck<>("Search Card Deck", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                SearchCard card = new SearchCard();
                card.loadCard((JSONObject) o);
                cardDeck.add(card);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return cardDeck;
    }

    public void loadCard(JSONObject dice) {
        this.componentName = ((String) ( (JSONArray) dice.get("name")).get(1));
        this.itemType = ((String) ( (JSONArray) dice.get("type")).get(1));
        this.value = ((Long) ( (JSONArray) dice.get("value")).get(1)).intValue();
        parseComponent(this, dice);
    }

    public String getItemType() {
        return itemType;
    }

    public int getValue() {
        return value;
    }

    @Override
    public SearchCard copy() {
        SearchCard card = new SearchCard(componentName, componentID);
        card.itemType = itemType;
        card.value = value;
        copyComponentTo(card);
        return card;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchCard that)) return false;
        if (!super.equals(o)) return false;
        return value == that.value && Objects.equals(itemType, that.itemType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), itemType, value);
    }
}
