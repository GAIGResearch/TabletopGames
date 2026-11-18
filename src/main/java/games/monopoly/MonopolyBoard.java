package games.monopoly;

import games.monopoly.components.MonopolyCard;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.util.*;

/**
 * The board: its squares in order round from GO, the colour groups, the Chance and Community Chest cards and the GO
 * salary, loaded from a JSON file (MonopolyParameters.boardFile). See data/monopoly/ukBoard.json. Immutable, and
 * shared by all states.
 */
public class MonopolyBoard {

    public final String fileName;
    private final String currency;
    private final int goSalary;
    private final List<MonopolyGroup> groups = new ArrayList<>();
    private final List<MonopolySquare> squares = new ArrayList<>();
    private final Map<String, MonopolySquare> byName = new HashMap<>();
    private final List<MonopolyCard> chanceCards = new ArrayList<>();
    private final List<MonopolyCard> communityChestCards = new ArrayList<>();
    private final MonopolySquare jail;

    public MonopolyBoard(String fileName) {
        this.fileName = fileName;
        JSONObject json = JSONUtils.loadJSONFile(fileName);
        currency = (String) json.get("currency");
        goSalary = intValue(json, "goSalary");

        Map<String, MonopolyGroup> groupByName = new HashMap<>();
        for (Object o : (JSONArray) json.get("groups")) {
            JSONObject g = (JSONObject) o;
            MonopolyGroup group = new MonopolyGroup(groups.size(), (String) g.get("name"), (String) g.get("colour"));
            groups.add(group);
            groupByName.put(group.name(), group);
        }

        MonopolySquare jailSquare = null;
        for (Object o : (JSONArray) json.get("squares")) {
            JSONObject s = (JSONObject) o;
            String name = (String) s.get("name");
            SquareType type = SquareType.valueOf((String) s.get("type"));
            MonopolyGroup group = null;
            if (type == SquareType.STREET) {
                group = groupByName.get((String) s.get("group"));
                if (group == null)
                    throw new IllegalArgumentException(fileName + ": " + name + " is in unknown group " + s.get("group"));
            }
            JSONArray rentList = (JSONArray) s.get("rents");
            int[] rents = rentList == null ? new int[0]
                    : rentList.stream().mapToInt(r -> ((Long) r).intValue()).toArray();
            MonopolySquare square = new MonopolySquare(squares.size(), name, type, group, intValue(s, "price"), rents,
                    intValue(s, "houseCost"), intValue(s, "mortgage"), intValue(s, "tax"));
            if (byName.put(name, square) != null)
                throw new IllegalArgumentException(fileName + ": two squares are called " + name);
            squares.add(square);
            if (type == SquareType.JAIL)
                jailSquare = square;
        }
        if (jailSquare == null)
            throw new IllegalArgumentException(fileName + ": no JAIL square");
        jail = jailSquare;

        loadCards(json, "chance", MonopolyCard.Pile.CHANCE, chanceCards);
        loadCards(json, "communityChest", MonopolyCard.Pile.COMMUNITY_CHEST, communityChestCards);
    }

    private void loadCards(JSONObject json, String key, MonopolyCard.Pile pile, List<MonopolyCard> cards) {
        for (Object o : (JSONArray) json.get(key)) {
            JSONObject c = (JSONObject) o;
            String target = (String) c.get("target");
            if (target != null && !byName.containsKey(target))
                throw new IllegalArgumentException(fileName + ": a card names unknown square " + target);
            cards.add(new MonopolyCard(pile, (String) c.get("text"),
                    MonopolyCard.Effect.valueOf((String) c.get("effect")),
                    intValue(c, "amount"), intValue(c, "hotelAmount"), target));
        }
    }

    // a missing number is 0
    private static int intValue(JSONObject json, String key) {
        Long value = (Long) json.get(key);
        return value == null ? 0 : value.intValue();
    }

    /**
     * The currency symbol for display.
     */
    public String currency() {
        return currency;
    }

    public int goSalary() {
        return goSalary;
    }

    /**
     * All the squares, in order round the board from GO.
     */
    public List<MonopolySquare> squares() {
        return Collections.unmodifiableList(squares);
    }

    public int nSquares() {
        return squares.size();
    }

    public MonopolySquare square(int index) {
        return squares.get(index);
    }

    public MonopolySquare square(String name) {
        MonopolySquare s = byName.get(name);
        if (s == null)
            throw new IllegalArgumentException("No square " + name + " in " + fileName);
        return s;
    }

    public List<MonopolyGroup> groups() {
        return Collections.unmodifiableList(groups);
    }

    /**
     * The streets of the colour group, in board order.
     */
    public List<MonopolySquare> streets(MonopolyGroup group) {
        return squares.stream().filter(s -> group.equals(s.group())).toList();
    }

    /**
     * The squares of the given type, in board order.
     */
    public List<MonopolySquare> squares(SquareType type) {
        return squares.stream().filter(s -> s.type() == type).toList();
    }

    /**
     * The square where tokens in Jail (and Just Visiting) stand.
     */
    public MonopolySquare jail() {
        return jail;
    }

    /**
     * The Chance cards, in the order of the file.
     */
    public List<MonopolyCard> chanceCards() {
        return Collections.unmodifiableList(chanceCards);
    }

    /**
     * The Community Chest cards, in the order of the file.
     */
    public List<MonopolyCard> communityChestCards() {
        return Collections.unmodifiableList(communityChestCards);
    }
}
