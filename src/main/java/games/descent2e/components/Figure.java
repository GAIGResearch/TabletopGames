package games.descent2e.components;

import core.components.Token;
import core.properties.PropertyInt;
import games.descent2e.DescentTypes;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;
import utilities.Vector2D;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static games.descent2e.DescentConstants.*;

// TODO: figure out how to do ability/heroic-feat
public class Figure extends Token {
    int xp;
    int movePoints;
    int hp;  // TODO: reset this every quest to max HP

    int nActionsExecuted;
    Vector2D location;
    Pair<Integer,Integer> size;

    Set<DescentTypes.DescentCondition> conditions;  // TODO: clear every quest + when figure exhausted?

    public Figure(String name) {
        super(name);
        xp = 0;
        size = new Pair<>(1,1);
        conditions = new HashSet<>();
    }

    protected Figure(String name, int ID) {
        super(name, ID);
    }

    public void resetRound() {
        if (getProperty(movementHash) != null) {
            this.movePoints = ((PropertyInt) getProperty(movementHash)).value;
        } else {
            this.movePoints = 0;
        }
        this.nActionsExecuted = 0;
    }

    public int getXP() {
        return xp;
    }

    public void setXP(int xp) {
        this.xp = xp;
    }

    public int getMovePoints() {
        return movePoints;
    }

    public void setMovePoints(int movePoints) {
        this.movePoints = movePoints;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public Vector2D getLocation() {
        return location;
    }

    public void setLocation(Vector2D location) {
        this.location = location;
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

    @Override
    public Figure copy() {
        Figure copy = new Figure(componentName, componentID);
        copyComponentTo(copy);
        return copy;
    }

    public void copyComponentTo(Figure copyTo) {
        super.copyComponentTo(copyTo);
        copyTo.xp = xp;
        copyTo.tokenType = tokenType;
        copyTo.movePoints = movePoints;
        copyTo.hp = hp;
        if (location != null) {
            copyTo.location = location.copy();
        }
        copyTo.nActionsExecuted = nActionsExecuted;
        copyTo.size = size.copy();
        copyTo.conditions = new HashSet<>(conditions);
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
        this.movePoints = ((PropertyInt)getProperty(movementHash)).value;
        this.hp = ((PropertyInt)getProperty(healthHash)).value;
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
