package games.dicemonastery.components;

import core.components.Component;
import core.properties.PropertyInt;
import core.properties.PropertyIntArray;
import core.properties.PropertyString;
import games.dicemonastery.DiceMonasteryConstants.Resource;
import utilities.Hash;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarketCard extends Component {

    final static int calfHash = Hash.GetInstance().hash("calfSkin");
    final static int grainHash = Hash.GetInstance().hash("grain");
    final static int beerHash = Hash.GetInstance().hash("beer");
    final static int meadHash = Hash.GetInstance().hash("mead");
    final static int candleHash = Hash.GetInstance().hash("candle");
    final static int inkHash = Hash.GetInstance().hash("ink");
    final static int inkTypeHash = Hash.GetInstance().hash("inkType");

    public final int calf_skin;
    public final int grain;
    public final int beer;
    public final int mead;
    public final int candle;
    public final Resource inkType;
    public final int inkPrice;

    public static MarketCard create(Component c) {
        int calf = ((PropertyInt) c.getProperty(calfHash)).value;
        int grain = ((PropertyInt) c.getProperty(grainHash)).value;
        int beer = ((PropertyInt) c.getProperty(beerHash)).value;
        int mead = ((PropertyInt) c.getProperty(meadHash)).value;
        int candle = ((PropertyInt) c.getProperty(candleHash)).value;
        int ink = ((PropertyInt) c.getProperty(inkHash)).value;
        String inkName = ((PropertyString) c.getProperty(inkTypeHash)).value;
        Resource inkType = inkName.isEmpty() ? null : Resource.valueOf(inkName);
        return new MarketCard(calf, grain, beer, mead, candle, ink, inkType);
    }

    private MarketCard(int calf, int grain, int beer, int mead, int candle, int inkPrice, Resource inkType) {
        super(Utils.ComponentType.CARD, "Market Card");
        this.calf_skin = calf;
        this.grain = grain;
        this.beer = beer;
        this.mead = mead;
        this.candle = candle;
        this.inkType = inkType;
        this.inkPrice = inkPrice;
    }

    @Override
    public Component copy() {
        return this;  // immutable
    }

    @Override
    public String toString() {
        return String.format("CalfSkin: %d, Grain: %d, %s: %d, Beer: %d, Mead: %d, Candle: %d",
                calf_skin, grain, inkType, inkPrice, beer, mead, candle);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MarketCard) {
            MarketCard other = (MarketCard) o;
            return other.inkPrice == inkPrice && other.inkType == inkType &&
                    other.calf_skin == calf_skin && other.grain == grain &&
                    other.beer == beer && other.mead == mead && other.candle == candle;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(beer, mead, candle, calf_skin, grain, inkPrice, inkType);
    }
}
