package games.descent2e.concepts;

import core.components.Component;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.tokens.DToken;
import org.json.simple.JSONObject;
import utilities.Utils;

import java.util.ArrayList;

public class CountGameFeature {
    CountType countingWhat;
    String figureNameContains;

    public int count(DescentGameState gs) {
        ArrayList<Figure> figures = new ArrayList<>();
        for (Component c: gs.getAllComponents().getComponents()) {
            if (c instanceof Figure && c.getComponentName().contains(figureNameContains)) {
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
                if (c.getType() == Utils.ComponentType.TOKEN) {
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
