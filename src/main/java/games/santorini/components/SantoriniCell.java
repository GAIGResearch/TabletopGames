package games.santorini.components;

import core.components.Component;
import utilities.Utils;

public class SantoriniCell extends Component {
    private int level;
    private int playerIn;

    public SantoriniCell() {
        super(Utils.ComponentType.TOKEN);
        level    = 0;  // Empty
        playerIn = -1; // None
    }

    public SantoriniCell(int ID){
        super(Utils.ComponentType.TOKEN, ID);
    }

    @Override
    public Component copy() {
        SantoriniCell sc = new SantoriniCell(componentID);
        sc.level = level;
        sc.playerIn = playerIn;
        return sc;
    }

    @Override
    public String toString() {
        String s = "[" + String.valueOf(level) + " ";
        if (isPlayer())
            s += String.valueOf(playerIn) + "]";
        else
            s += " ]";
        return s;
    }

    public int getLevel()    { return level;    }
    public int getPlayerIn() { return playerIn; }

    public void setLevel   (int l) { level = l;   }
    public void setPlayerIn(int p) { playerIn = p;}

    public void setNonPlayerIn() { playerIn = -1; }

    public boolean isPlayer() { return playerIn != -1; }
}

