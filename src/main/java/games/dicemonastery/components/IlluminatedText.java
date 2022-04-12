package games.dicemonastery.components;

import core.components.Component;
import core.properties.PropertyInt;
import core.properties.PropertyIntArray;
import core.properties.PropertyString;
import core.properties.PropertyStringArray;
import games.dicemonastery.DiceMonasteryConstants.Resource;
import utilities.Hash;
import utilities.Utils;

import java.util.Arrays;
import java.util.Objects;

public class IlluminatedText extends Component {

    final static int nameHash = Hash.GetInstance().hash("name");
    final static int rewardHash = Hash.GetInstance().hash("rewards");
    final static int pietyHash = Hash.GetInstance().hash("minPiety");
    final static int vellumHash = Hash.GetInstance().hash("vellum");
    final static int candleHash = Hash.GetInstance().hash("candles");
    final static int inkHash = Hash.GetInstance().hash("inks");
    final static int specialInkHash = Hash.GetInstance().hash("specialInks");

    public final int[] rewards;
    public final int minPiety;
    public final int vellum;
    public final int candles;
    public final int inks;
    public final Resource[] specialInks;

    public static IlluminatedText create(Component c) {
        String name = ((PropertyString) c.getProperty(nameHash)).value;
        int[] rewards = ((PropertyIntArray) c.getProperty(rewardHash)).getValues();
        int minPiety = ((PropertyInt) c.getProperty(pietyHash)).value;
        int vellum = ((PropertyInt) c.getProperty(vellumHash)).value;
        int candles = ((PropertyInt) c.getProperty(candleHash)).value;
        int inks = ((PropertyInt) c.getProperty(inkHash)).value;
        String[] specialInkNames = ((PropertyStringArray) c.getProperty(specialInkHash)).getValues();
        Resource[] specialInks = specialInkNames.length == 0 ? new Resource[0] : Arrays.stream(specialInkNames).map(Resource::valueOf).toArray(Resource[]::new);
        return new IlluminatedText(name, rewards, minPiety, vellum, candles, inks, specialInks);
    }

    private IlluminatedText(String name, int[] rewards, int minPiety, int vellum, int candles, int inks, Resource[] specialInks) {
        super(Utils.ComponentType.CARD, name);
        this.rewards = rewards;
        this.minPiety = minPiety;
        this.vellum = vellum;
        this.candles = candles;
        this.inks = inks;
        this.specialInks = specialInks;
    }

    @Override
    public IlluminatedText copy() {
        return this;
        // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IlluminatedText) {
            IlluminatedText other = (IlluminatedText) o;
            return other.componentName.equals(componentName) && other.minPiety == minPiety &&
                    other.vellum == vellum && other.candles == candles && other.inks == inks &&
                    Arrays.equals(other.specialInks, specialInks) && Arrays.equals(other.rewards, rewards);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // we exclude special inks from this as these are uniquely determined from the name
        return Objects.hash(componentName, minPiety, vellum, candles, inks) + 319 * Arrays.hashCode(rewards);
    }
}
