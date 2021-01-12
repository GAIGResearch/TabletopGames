package games.diamant.components;

import core.components.Component;
import utilities.Utils;

// Gems on the hand of the player
public class DiamantHand extends Component {
    private int nGems;

    public DiamantHand() {
        super(Utils.ComponentType.TOKEN);
        nGems = 0;
    }

    public DiamantHand(int ID) {
        super(Utils.ComponentType.TOKEN, ID);
        nGems = 0;
    }

    @Override
    public Component copy() {
        DiamantHand dh = new DiamantHand(componentID);
        dh.nGems = nGems;
        return dh;
    }

    @Override
    public String toString() {
        return String.valueOf(nGems);
    }

    public int  GetNumberGems()      { return nGems;}
    public void SetNumberGems(int n) { nGems = n;   }
    public void AddGems(int n)       { nGems += n;  }
}

