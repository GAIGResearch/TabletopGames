package games.descent2e.components.cards;

import core.CoreConstants;
import core.components.Card;
import core.components.Deck;
import games.descent2e.DescentGameState;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;

import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

public class RelicCard extends Card {

    private boolean heroRelic;
    private String counterpart;
    private boolean usable = true;

    public RelicCard() {
        super("");
    }

    protected RelicCard(String componentName, int componentID) {
        super(componentName, componentID);
    }

    public static Deck<Card> loadCards(String filename) {
        JSONParser jsonParser = new JSONParser();
        Deck<Card> deck = new Deck<>("Relic Deck", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        try (FileReader reader = new FileReader(filename)) {
            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                RelicCard card = new RelicCard();
                card.loadCard((JSONObject) o);
                deck.add(card);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return deck;

    }

    public void loadCard(JSONObject card) {
        this.componentName = ((String) ( (JSONArray) card.get("name")).get(1));
        this.counterpart = ((String) ( (JSONArray) card.get("counterpart")).get(1));
        this.heroRelic = (Boolean) ((JSONArray) card.get("heroRelic")).get(1);
        parseComponent(this, card);
    }

    public RelicCard getCounterpart(DescentGameState dgs) {
        for (Card relic : dgs.getRelicCards()) {
            if (relic.getComponentName().equals(counterpart))
                return (RelicCard) relic;
        }
        return null;
    }

    public boolean counterpartAvailable(DescentGameState dgs) {
        return getCounterpart(dgs) != null;
    }

    public boolean isHeroRelic() {
        return heroRelic;
    }

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        this.usable = usable;
    }

    @Override
    public RelicCard copy() {
        RelicCard card = new RelicCard(componentName, componentID);
        card.heroRelic = heroRelic;
        card.counterpart = counterpart;
        copyComponentTo(card);
        return card;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RelicCard that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(counterpart, that.counterpart) && usable == that.usable &&
                heroRelic == that.heroRelic;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), heroRelic, counterpart, usable);
    }
}
