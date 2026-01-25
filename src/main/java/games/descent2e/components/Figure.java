package games.descent2e.components;

import core.CoreConstants;
import core.components.Counter;
import core.components.Deck;
import core.components.Token;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.attack.MeleeAttack;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;
import utilities.Vector2D;

import javax.management.ObjectName;
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
        Fatigue,
        XP,
        Gold,
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

    Map<Attribute, Counter> attributes;
    Counter nActionsExecuted;

    // For big monsters, this is the anchor point. Their size and orientation can be used to find all spaces occupied by the figure
    // Note: size remains constant and never changes. For finding spaces when orientation % 2 == 1 for medium monsters,
    // size dimensions should be swapped (in a copy of the pair to leave this the same).
    Vector2D position;
    Pair<Integer,Integer> size;

    Set<DescentTypes.DescentCondition> conditions;  // TODO: clear every quest + when figure exhausted?
    boolean removedConditionThisTurn = false;
    List<AttributeTest> attributeTests;
    List<DescentAction> abilities;  // TODO track exhausted etc.
    MeleeAttack currentAttack;
    boolean hasMoved, hasAttacked, hasRerolled;
    boolean isOffMap, canIgnoreEnemies, extraAction = false;

    Deck<DescentCard> exhausted = new Deck<>("Exhausted", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);;

    List<String> actionsTaken = new ArrayList<>();

    public Figure(String name, int nActionsPossible) {
        super(name);
        size = new Pair<>(1,1);
        conditions = new HashSet<>();
        attributeTests = new ArrayList<>();
        attributes = new HashMap<>();
        attributes.put(XP, new Counter(0, 0, -1, "XP"));
        attributes.put(Gold, new Counter(0, 0, -1, "Gold"));
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
        // We unexhaust any exhausted cards at the start of the turn, refreshing them to use this turn
        refreshAllCards();
        clearCurrentAttack();
        clearActionsTaken();
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

    public void decrementAttribute(Attribute a, int decrement) {
        attributes.get(a).decrement(decrement);
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
    public boolean hasMoved() {
        return hasMoved;
    }
    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }
    public boolean hasAttacked() {
        return hasAttacked;
    }
    public void setHasAttacked(boolean hasAttacked) {
        this.hasAttacked = hasAttacked;
    }
    public boolean hasRerolled() {
        return hasRerolled;
    }
    public void setRerolled(boolean hasRerolled) {
        this.hasRerolled = hasRerolled;
    }
    public boolean isOffMap() {
        return isOffMap;
    }
    public void setOffMap(boolean offMap) {
        isOffMap = offMap;
    }
    public boolean canIgnoreEnemies() {
        return canIgnoreEnemies;
    }
    public void setCanIgnoreEnemies(boolean canIgnoreEnemies) {
        this.canIgnoreEnemies = canIgnoreEnemies;
    }
    public void setUsedExtraAction(boolean extraAction) {
        this.extraAction = extraAction;
    }
    public boolean hasUsedExtraAction() {
        return extraAction;
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
        // A Figure can only be affected by a condition once - they do not stack
        // Therefore we can only add the condition if it is not already on the list
        if (!conditions.contains(condition)) {
            conditions.add(condition);
        }
    }

    public void removeCondition(DescentTypes.DescentCondition condition) {
        if (conditions.contains(condition))
            conditions.remove(condition);
    }
    public void removeAllConditions() {
        if (!conditions.isEmpty()) {
            conditions.clear();
        }
    }

    public boolean hasCondition(DescentTypes.DescentCondition condition) {
        return conditions.contains(condition);
    }

    public boolean hasRemovedConditionThisTurn() {
        return removedConditionThisTurn;
    }

    public void setRemovedConditionThisTurn(boolean removedConditionThisTurn) {
        this.removedConditionThisTurn = removedConditionThisTurn;
    }

    public List<AttributeTest> getAttributeTests() {
        return attributeTests;
    }

    public void addAttributeTest(AttributeTest attributeTest) {
        attributeTests.add(attributeTest);
    }

    public void removeAttributeTest(AttributeTest attributeTest) {
        attributeTests.remove(attributeTest);
    }
    public void removeLastAttributeTest() {
        if (attributeTests.isEmpty()) return;
        attributeTests.remove(attributeTests.size()-1);
    }
    public AttributeTest getLastAttributeTest() {
        if (attributeTests.isEmpty()) return null;
        return attributeTests.get(attributeTests.size()-1);
    }
    public void clearAttributeTest() {
        if (!(attributeTests).isEmpty()) {
            attributeTests.clear();
        }
    }

    public boolean hasAttributeTest(AttributeTest attributeTest) {
        if (attributeTests == null) {
            return false;
        }

        for (AttributeTest test : attributeTests) {
            if (attributeTest.getAttributeTestName().equals(test.getAttributeTestName())) {
                return true;
            }
        }
        return false;
    }

    public void addAbility(DescentAction ability) {
        this.abilities.add(ability);
    }
    public void removeAbility(DescentAction ability) {
        this.abilities.remove(ability);
    }
    public List<DescentAction> getAbilities() {
        return abilities;
    }
    public boolean hasAbility(DescentAction ability) {
        return abilities.contains(ability);
    }
    public DicePool getAttackDice() { return attackDice;}

    public DicePool getDefenceDice() {return defenceDice;}

    public String getName() { return componentName;}
    public MeleeAttack getCurrentAttack() { return currentAttack;}
    public void setCurrentAttack(MeleeAttack currentAttack) { this.currentAttack = currentAttack;}
    public void clearCurrentAttack() { currentAttack = null;}

    public void exhaustCard (DescentCard card)
    {
        exhausted.add(card);
    }
    public void refreshAllCards()
    {
        exhausted.clear();
    }
    public boolean isExhausted(DescentCard card)
    {
        return exhausted.contains(card);
    }
    public void addActionTaken(String action) { actionsTaken.add(action);}
    public List<String> getActionsTaken() { return actionsTaken;}
    public void clearActionsTaken() { actionsTaken.clear();}

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
        copyTo.nActionsExecuted = nActionsExecuted.copy();
        copyTo.size = size.copy();
        copyTo.conditions = new HashSet<>(conditions);
        copyTo.removedConditionThisTurn = removedConditionThisTurn;
        if (attributeTests != null) {
            copyTo.attributeTests = new ArrayList<>();
            for (AttributeTest test : attributeTests) {
                copyTo.attributeTests.add(test.copy());
            }
        }
        if (abilities != null) {
            copyTo.abilities = new ArrayList<>();
            for (DescentAction ability : abilities) {
                copyTo.abilities.add(ability.copy());
            }
        }
        copyTo.attackDice = attackDice.copy();
        copyTo.defenceDice = defenceDice.copy();
        copyTo.hasMoved = hasMoved;
        copyTo.hasAttacked = hasAttacked;
        copyTo.hasRerolled = hasRerolled;
        copyTo.isOffMap = isOffMap;
        copyTo.canIgnoreEnemies = canIgnoreEnemies;
        copyTo.extraAction = extraAction;
        if (currentAttack != null) {
            copyTo.currentAttack = currentAttack.copy();
        }
        copyTo.exhausted = exhausted.copy();
        copyTo.actionsTaken = new ArrayList<>(actionsTaken);
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
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Figure figure = (Figure) o;
        return removedConditionThisTurn == figure.removedConditionThisTurn && hasMoved == figure.hasMoved && hasAttacked == figure.hasAttacked && hasRerolled == figure.hasRerolled && isOffMap == figure.isOffMap && canIgnoreEnemies == figure.canIgnoreEnemies && extraAction == figure.extraAction && Objects.equals(attackDice, figure.attackDice) && Objects.equals(defenceDice, figure.defenceDice) && Objects.equals(attributes, figure.attributes) && Objects.equals(nActionsExecuted, figure.nActionsExecuted) && Objects.equals(position, figure.position) && Objects.equals(size, figure.size) && Objects.equals(conditions, figure.conditions) && Objects.equals(attributeTests, figure.attributeTests) && Objects.equals(abilities, figure.abilities) && Objects.equals(currentAttack, figure.currentAttack) && Objects.equals(exhausted, figure.exhausted) && Objects.equals(actionsTaken, figure.actionsTaken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackDice, defenceDice, attributes, nActionsExecuted, position, size, conditions, removedConditionThisTurn, attributeTests, abilities, currentAttack, hasMoved, hasAttacked, hasRerolled, isOffMap, canIgnoreEnemies, extraAction, exhausted, actionsTaken);
    }

    @Override
    public String toString() {
        if (componentName != null) return componentName;
        return this.getClass().getSimpleName();
    }
}
