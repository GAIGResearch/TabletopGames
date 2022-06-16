package games.descent2e.concepts;

import core.components.Component;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;
import org.json.simple.JSONObject;
import utilities.Utils;

import java.util.ArrayList;

public class CountGameOver extends GameOverCondition {
    // IMPORTANT: All values are effectively final and should not be changed after parsing initialisation
    CountType countType;
    ComparisonType comparisonType;
    int target;
    String figureNameContains;
    Utils.GameResult resultOverlord, resultHeroes;

    @Override
    public Utils.GameResult test(DescentGameState gs) {
        int count = count(gs);
        if (comparisonType.compare(count, target)) {
            return endGame(gs);
        }
        return Utils.GameResult.GAME_ONGOING;
    }

    private int count(DescentGameState gs) {
        ArrayList<Figure> figures = new ArrayList<>();
        for (Component c: gs.getAllComponents().getComponents()) {
            if (c instanceof Figure && c.getComponentName().contains(figureNameContains)) {
                figures.add((Figure) c);
            }
        }
        if (countType == CountType.Attribute) {
            int sum = 0;
            for (Figure f: figures) {
                sum += f.getAttributeValue(countType.attribute);
            }
            return sum;
        } else if (countType == CountType.NFiguresAlive) {
            int count = 0;
            for (Figure f : figures) {
                if (!f.getAttribute(Figure.Attribute.Health).isMinimum()) count++;
            }
            return count;
        }
        return -1;
    }

    @Override
    public void parse(JSONObject jsonObject) {
        countType = CountType.valueOf((String) jsonObject.get("type"));
        if (countType == CountType.Attribute) countType.attribute = Figure.Attribute.valueOf((String) jsonObject.get("attribute"));
        figureNameContains = (String) jsonObject.get("figureNameContains");
        target = (int)(long)jsonObject.get("target");
        comparisonType = ComparisonType.valueOf((String) jsonObject.get("comparison-type"));
        resultHeroes = Utils.GameResult.valueOf((String) jsonObject.get("result-heroes"));
        resultOverlord = Utils.GameResult.valueOf((String) jsonObject.get("result-overlord"));
    }

    @Override
    public String toString() {
        return "Count " + countType.toString() + " " + figureNameContains + " " + comparisonType.toString() + target
                + "? " + "Heroes: " + resultHeroes + "; Overlord: " + resultOverlord;
    }

    @Override
    public String getString(DescentGameState gs) {
        int count = count(gs);
        return "Count " + countType.toString() + " " + figureNameContains + ": " + count + comparisonType.toString() + target
                + "? " + "Heroes: " + resultHeroes + "; Overlord: " + resultOverlord;
    }

    private Utils.GameResult endGame(DescentGameState gs) {
        gs.setGameStatus(Utils.GameResult.GAME_END);
        for (int i = 0; i < gs.getNPlayers(); i++) {
            if (gs.getOverlordPlayer() == i) gs.setPlayerResult(resultOverlord, i);
            else gs.setPlayerResult(resultHeroes, i);
        }
        return Utils.GameResult.GAME_END;
    }


    enum CountType {
        NFiguresAlive,  // how many of the figures are alive
        Attribute;  // sum of values of specific attribute
        Figure.Attribute attribute;

        @Override
        public String toString() {
            switch(this) {
                case NFiguresAlive:
                    return "N";
                case Attribute:
                    return attribute.name();
            }
            return null;
        }
    }

    enum ComparisonType {
        More,
        Less,
        Equal;
        public boolean compare(int a, int b) {
            switch (this) {
                case More:
                    return a > b;
                case Less:
                    return a < b;
                case Equal:
                    return a == b;
            }
            return false;
        }

        @Override
        public String toString() {
            switch(this) {
                case More:
                    return ">";
                case Less:
                    return "<";
                case Equal:
                    return "=";
            }
            return null;
        }
    }
}
