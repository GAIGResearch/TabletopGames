package games.descent2e.concepts;

import core.CoreConstants;
import core.components.Component;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import games.descent2e.components.tokens.DToken;
import org.json.simple.JSONObject;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Objects;

public class CountGameFeature {
    CountType countingWhat;
    String figureNameContains;

    public int count(DescentGameState gs) {
        ArrayList<Figure> figures = new ArrayList<>();
        for (Component c: gs.getAllComponents().getComponents()) {
            if (c instanceof Figure && c.getComponentName().contains(figureNameContains)) {
                if (c instanceof Monster) {
                    // We only want to count the Monsters that have been spawned on the map
                    // Not the ones held in reserve to copy in for reinforcements
                    if (gs.getOriginalMonsters().stream().flatMap(x -> x.stream()).anyMatch(x -> x == c)
                            && !gs.getMonsters().stream().flatMap(x -> x.stream()).anyMatch(x -> x == c)) continue;
                }
                figures.add((Figure) c);
            }
        }
        if (countingWhat == CountType.Attribute) {
            int sum = 0;
            for (Figure f: figures) {
                sum += f.getAttributeValue(countingWhat.attributeType);
            }
            return sum;
        } else if (countingWhat == CountType.NFiguresAlive) {
            int count = 0;
            for (Figure f : figures) {
                if (!f.getAttribute(Figure.Attribute.Health).isMinimum()) count++;
            }
            return count;
        } else if (countingWhat == CountType.Token) {
            int count = 0;
            for (Component c: gs.getAllComponents().getComponents()) {
                if (c.getType() == CoreConstants.ComponentType.TOKEN) {
                    DToken d = (DToken) c;
                    if (d.getOwnerId() != -1 && d.getDescentTokenType() == countingWhat.tokenType) {
                        count++;
                    }
                }
            }
            return count;
        }
        return -1;
    }

    public static CountGameFeature parse(JSONObject jsonObject) {
        CountGameFeature cgf = new CountGameFeature();
        cgf.countingWhat = CountType.valueOf((String) jsonObject.get("type"));
        if (cgf.countingWhat == CountType.Attribute) cgf.countingWhat.attributeType = Figure.Attribute.valueOf((String) jsonObject.get("attribute"));
        else if (cgf.countingWhat == CountType.Token) cgf.countingWhat.tokenType = DescentTypes.DescentToken.valueOf((String) jsonObject.get("token"));
        cgf.figureNameContains = (String) jsonObject.get("figureNameContains");
        return cgf;
    }

    public CountGameFeature copy() {
        CountGameFeature cgf = new CountGameFeature();
        cgf.countingWhat = countingWhat;
        cgf.figureNameContains = figureNameContains;
        return cgf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountGameFeature that = (CountGameFeature) o;
        return countingWhat == that.countingWhat && Objects.equals(figureNameContains, that.figureNameContains);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countingWhat, figureNameContains);
    }

    enum CountType {
        NFiguresAlive,  // how many of the figures are alive
        Attribute,  // sum of values of specific attribute
        Token;

        Figure.Attribute attributeType;
        DescentTypes.DescentToken tokenType;

        @Override
        public String toString() {
            switch(this) {
                case NFiguresAlive:
                    return "N";
                case Attribute:
                    return attributeType.name();
                case Token:
                    return tokenType.name();
            }
            return null;
        }
    }
}
