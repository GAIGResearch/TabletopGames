package games.descent2e.components;

import core.components.Counter;
import core.components.Token;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;
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

import static games.descent2e.DescentConstants.attackHash;
import static games.descent2e.DescentConstants.defenceHash;
import static games.descent2e.components.Figure.Attribute.*;

// TODO: figure out how to do ability/heroic-feat
public class Figure extends Token {

    DicePool attackDice = DicePool.empty;
    DicePool defenceDice = DicePool.empty;

    public enum Attribute {
        MovePoints,
        Health,
        XP,
        Fatigue,
        Might,
        Willpower,
        Knowledge,
        Awareness;
        public boolean isSecondary() {
            switch (this) {
                case MovePoints:
                case Health:
                case XP:
                case Fatigue:
                    return false;
                case Might:
                case Willpower:
                case Knowledge:
                case Awareness:
                    return true;
            }
            return false;
        }
    }

    HashMap<Attribute, Counter> attributes;
    Counter nActionsExecuted;

    // For big monsters, this is the anchor point. Their size and orientation can be used to find all spaces occupied by the figure
    // Note: size remains constant and never changes. For finding spaces when orientation % 2 == 1 for medium monsters,
    // size dimensions should be swapped (in a copy of the pair to leave this the same).
    Vector2D position;
    Pair<Integer,Integer> size;

    Set<DescentTypes.DescentCondition> conditions;  // TODO: clear every quest + when figure exhausted?
    ArrayList<DescentAction> abilities;  // TODO track exhausted etc.

    public Figure(String name, int nActionsPossible) {
        super(name);
        size = new Pair<>(1,1);
        conditions = new HashSet<>();
        attributes = new HashMap<>();
        attributes.put(XP, new Counter(0, 0, -1, "XP"));
        abilities = new ArrayList<>();
        nActionsExecuted = new Counter(0, 0, nActionsPossible, "Actions executed");
    }

    protected Figure(String name, Counter actions, int ID) {
        super(name, ID);
        this.nActionsExecuted = actions;
    }

    public void resetRound() {
        if (this.attributes.containsKey(MovePoints)) {
            // Overlord doesn't have move points
            this.attributes.get(MovePoints).setToMin();
        }
        this.nActionsExecuted.setToMin();
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

    public Counter getNActionsExecuted() {
        return nActionsExecuted;
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

    public DicePool getAttackDice() { return attackDice;}

    public DicePool getDefenceDice() {return defenceDice;}

    @Override
    public Figure copy() {
        Figure copy = new Figure(componentName, nActionsExecuted.copy(), componentID);
        copyComponentTo(copy);
        return copy;
    }

    public Figure copyNewID() {
        Figure copy = new Figure(componentName, nActionsExecuted.getMaximum());
        copyComponentTo(copy);
        copy.nActionsExecuted = nActionsExecuted.copy();
        return copy;
    }

    public void copyComponentTo(Figure copyTo) {
        super.copyComponentTo(copyTo);
        copyTo.tokenType = tokenType;
        copyTo.attributes = new HashMap<>();
        for (Map.Entry<Attribute, Counter> e : attributes.entrySet()) {
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
        copyTo.attackDice = getAttackDice().copy();
        copyTo.defenceDice = getDefenceDice().copy();
    }

    public void loadFigure(JSONObject figure, Set<String> ignoreKeys) {
        if (!ignoreKeys.contains("id")) {
            this.componentName = (String) figure.get("id");
        }
        if (!ignoreKeys.contains("type")) {
            this.tokenType = (String) ((JSONArray) figure.get("type")).get(1);
        }
        // TODO: custom load of figure properties
        parseComponent(this, figure, ignoreKeys);
        if (getProperty(attackHash) != null) {
            String[] attack = ((PropertyStringArray) getProperty(attackHash)).getValues();
            attackDice = DicePool.constructDicePool(attack);
        }
        if (getProperty(defenceHash) != null) {
            String[] defence = ((PropertyStringArray) getProperty(defenceHash)).getValues();
            defenceDice = DicePool.constructDicePool(defence);
        }
        for (Attribute a : Attribute.values()) {
            PropertyInt prop = ((PropertyInt) getProperty(a.name()));
            if (prop != null) {
                int max = prop.value;
                this.attributes.put(a, new Counter(max, 0, max, a.name()));
                if (a == MovePoints || a == Fatigue) this.setAttribute(a, 0);
            }
        }
    }

    /**
     * Creates a Token objects from a JSON object.
     *
     * @param figure - JSON to parse into Figure object.
     */
    public void loadFigure(JSONObject figure) {
        loadFigure(figure, new HashSet<>());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Figure)) return false;
        if (!super.equals(o)) return false;
        Figure figure = (Figure) o;
        return Objects.equals(attackDice, figure.attackDice) && Objects.equals(defenceDice, figure.defenceDice) && Objects.equals(attributes, figure.attributes) && Objects.equals(nActionsExecuted, figure.nActionsExecuted) && Objects.equals(position, figure.position) && Objects.equals(size, figure.size) && Objects.equals(conditions, figure.conditions) && Objects.equals(abilities, figure.abilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackDice, defenceDice, attributes, nActionsExecuted, position, size, conditions, abilities);
    }
}
