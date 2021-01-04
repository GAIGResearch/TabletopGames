package games.santorini.components;

import core.components.Component;
import utilities.Utils;

public class PlayerPosition extends Component {

    int row;
    int col;

    public PlayerPosition(int row, int col)
    {
        super(Utils.ComponentType.TOKEN);
        this.row = row;
        this.col = col;
    }

    public PlayerPosition(int ID){
        super(Utils.ComponentType.TOKEN, ID);
    }

    @Override
    public Component copy() {
        return new PlayerPosition(row, col);
    }

    int getRow() { return row; }
    int getCol() { return col; }
    void setRow(int r) { row = r; }
    void setCol(int c) { col = c; }

    @Override
    public String toString() {
        return row + " " + col;
    }
}
