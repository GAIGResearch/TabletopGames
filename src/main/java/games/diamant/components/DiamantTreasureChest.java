package games.diamant.components;

import core.components.Component;
import utilities.Utils;

// Gems on the treasure test of the player
public class DiamantTreasureChest extends Component {
    private int nGems;

    public DiamantTreasureChest() {
        super(Utils.ComponentType.TOKEN);
        nGems = 0;
    }

    public DiamantTreasureChest(int ID) {
        super(Utils.ComponentType.TOKEN, ID);
        nGems = 0;
    }

    @Override
    public Component copy() {
        DiamantTreasureChest dc = new DiamantTreasureChest(componentID);
        dc.nGems = nGems;
        return dc;
    }

    @Override
    public String toString() {
        return String.valueOf(nGems);
    }

    public int  GetNumberGems()      { return nGems;}
    public void SetNumberGems(int n) { nGems = n;   }
    public void AddGems(int n)       { nGems += n;  }
}

