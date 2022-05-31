package games.descent2e.components;

import core.components.Counter;
import core.components.Token;
import core.properties.PropertyInt;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;
import utilities.Vector2D;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static games.descent2e.components.Figure.Attribute.*;

// TODO: figure out how to do ability/heroic-feat
public class Figure extends Token {

    public enum Attribute {
        MovePoints,
        Health,
        XP,
        Fatigue,
        Might,
        Willpower,
        Knowledge,
        Awareness
    }

    HashMap<Attribute, Counter> attributes;


    int nActionsExecuted;

    Vector2D position;
    Pair<Integer,Integer> size;

    Set<DescentTypes.DescentCondition> conditions;  // TODO: clear every quest + when figure exhausted?
    ArrayList<DescentAction> abilities;  // TODO track exhausted etc.

    public Figure(String name) {
        super(name);
        size = new Pair<>(1,1);
        conditions = new HashSet<>();
        attributes = new HashMap<>();
        attributes.put(XP, new Counter(0, 0, -1, "XP"));
        abilities = new ArrayList<>();
    }

    protected Figure(String name, int ID) {
        super(name, ID);
    }

    public void resetRound() {
        this.attributes.get(MovePoints).setToMax();
        this.nActionsExecuted = 0;
    }

    public Counter getAttribute(Attribute attribute) {
        return attributes.get(attribute);
    }
    public int getAttributeValue(Attribute a) {
        return attributes.get(a).getValue();
    }
    public int getAttributeMax(Attribute a) {
        return attributes.get(a).getMaximum();
    }
    public int getAttributeMin(Attribute a) {
        return attributes.get(a).getMinimum();
    }
    public void setAttribute(Attribute a, Counter c) {
        attributes.put(a, c);
    }
    public void setAttribute(Attribute a, int value) {
        attributes.get(a).setValue(value);
    }
    public void incrementAttribute(Attribute a, int increment) {
        attributes.get(a).increment(increment);
    }
    public void setAttributeToMax(Attribute a) {
        attributes.get(a).setToMax();
    }
    public void setAttributeToMin(Attribute a) {
        attributes.get(a).setToMin();
    }

    public Vector2D getPosition() {
        return position;
    }

    public void setPosition(Vector2D position) {
        this.position = position;
    }

    public int getNActionsExecuted() {
        return nActionsExecuted;
    }

    public void setNActionsExecuted(int nActionsExecuted) {
        this.nActionsExecuted = nActionsExecuted;
    }

    public void setSize(int width, int height) {
        this.size = new Pair<>(width, height);
    }

    public Pair<Integer, Integer> getSize() {
        return size;
    }

    public Set<DescentTypes.DescentCondition> getConditions() {
        return conditions;
    }

    public void addCondition(DescentTypes.DescentCondition condition) {
        conditions.add(condition);
    }

    public void removeCondition(DescentTypes.DescentCondition condition) {
        conditions.remove(condition);
    }

    public boolean hasCondition(DescentTypes.DescentCondition condition) {
        return conditions.contains(condition);
    }

    public void addAbility(DescentAction ability) {
        this.abilities.add(ability);
    }
    public void removeAbility(DescentAction ability) {
        this.abilities.remove(ability);
    }
    public ArrayList<DescentAction> getAbilities() {
        return abilities;
    }

    @Override
    public Figure copy() {
        Figure copy = new Figure(componentName, componentID);
        copyComponentTo(copy);
        return copy;
    }

    public void copyComponentTo(Figure copyTo) {
        super.copyComponentTo(copyTo);
        copyTo.tokenType = tokenType;
        copyTo.attributes = new HashMap<>();
        for (Map.Entry<Attribute, Counter> e: attributes.entrySet()) {
            copyTo.attributes.put(e.getKey(), e.getValue().copy());
        }
        if (position != null) {
            copyTo.position = position.copy();
        }
        copyTo.nActionsExecuted = nActionsExecuted;
        copyTo.size = size.copy();
        copyTo.conditions = new HashSet<>(conditions);
        copyTo.abilities = new ArrayList<>();
        if (abilities != null) {
            for (DescentAction ability : abilities) {
                copyTo.abilities.add(ability.copy());
            }
        }
    }

    /**
     * Creates a Token objects from a JSON object.
     * @param figure - JSON to parse into Figure object.
     */
    protected void loadFigure(JSONObject figure) {
        this.componentName = (String) figure.get("id");
        this.tokenType = (String) ( (JSONArray) figure.get("type")).get(1);
        // TODO: custom load of figure properties
        parseComponent(this, figure);

        for (Attribute a: Attribute.values()) {
            PropertyInt prop = ((PropertyInt)getProperty(a.name()));
            if (prop != null) {
                int max = prop.value;
                this.attributes.put(a, new Counter(max, 0, max, a.name()));
            }
        }
        this.setAttribute(MovePoints, 0);
    }

    /**
     * Loads all figures from a JSON file.
     * @param filename - path to file.
     * @return - List of Figure objects.
     */
    public static List<Figure> loadFigures(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Figure> figures = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Figure newFigure = new Figure("");
                newFigure.loadFigure((JSONObject) o);
                figures.add(newFigure);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return figures;
    }
}
