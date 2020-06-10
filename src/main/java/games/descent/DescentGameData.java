package games.descent;

import core.AbstractGameData;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.components.Token;
import java.util.List;


public class DescentGameData extends AbstractGameData {
    List<GridBoard> tiles;
    List<GraphBoard> boardConfigurations;
    List<Token> figures;

    @Override
    public void load(String dataPath) {
        tiles = GridBoard.loadBoards(dataPath + "tiles.json");
        figures = Token.loadTokens(dataPath + "heroes.json");
        boardConfigurations = GraphBoard.loadBoards(dataPath + "boards.json");
    }

    @Override
    public GridBoard findGridBoard(String name) {
        for (GridBoard gb: tiles) {
            if (gb.getComponentName().equalsIgnoreCase(name)) {
                return gb;
            }
        }
        return null;
    }

    @Override
    public GraphBoard findGraphBoard(String name) {
        for (GraphBoard gb: boardConfigurations) {
            if (gb.getComponentName().equalsIgnoreCase(name)) {
                return gb;
            }
        }
        return null;
    }

    @Override
    public Token findToken(String name) {
        for (Token t: figures) {
            if (t.getComponentName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }
}
