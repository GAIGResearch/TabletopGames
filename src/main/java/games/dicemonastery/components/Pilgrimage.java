package games.dicemonastery.components;

import core.CoreConstants;
import core.components.Component;
import core.properties.PropertyInt;
import core.properties.PropertyIntArray;
import core.properties.PropertyString;
import evaluation.metrics.Event;
import games.dicemonastery.DiceMonasteryConstants.Resource;
import games.dicemonastery.DiceMonasteryGameState;
import utilities.Hash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.SHILLINGS;

public class Pilgrimage extends Component {

    static int mHash = Hash.GetInstance().hash("multiplicity");
    static int destinationHash = Hash.GetInstance().hash("destination");
    static int rewardHash = Hash.GetInstance().hash("reward");
    static int durationHash = Hash.GetInstance().hash("duration");
    static int minPietyHash = Hash.GetInstance().hash("minPiety");
    static int costHash = Hash.GetInstance().hash("cost");

    public final String destination;
    public final int minPiety;
    public final int cost;
    public final int[] vpPerStep;
    public final Resource finalReward;

    int player = -1;
    boolean active;
    int pilgrimId = -1;
    int progress = -1;


    public Pilgrimage(String destination, int minPiety, int cost, String reward, int[] duration) {
        super(CoreConstants.ComponentType.CARD, "Pilgrimage to " + destination);
        this.destination = destination;
        this.minPiety = minPiety;
        this.cost = cost;
        this.vpPerStep = duration;
        if (!reward.isEmpty())
            this.finalReward = Resource.valueOf(reward);
        else
            this.finalReward = null;

    }

    private Pilgrimage(Pilgrimage copy) {
        super(CoreConstants.ComponentType.CARD, copy.componentName, copy.componentID);
        this.destination = copy.destination;
        this.minPiety = copy.minPiety;
        this.cost = copy.cost;
        this.vpPerStep = copy.vpPerStep;
        this.finalReward = copy.finalReward;
        this.progress = copy.progress;
        this.pilgrimId = copy.pilgrimId;
        this.active = copy.active;
        this.player = copy.player;
    }

    public void startPilgrimage(Monk monk, DiceMonasteryGameState state) {
        pilgrimId = monk.getComponentID();
        player = monk.getOwnerId();
        state.addResource(player, SHILLINGS, -cost);
        state.moveMonk(pilgrimId, GATEHOUSE, PILGRIMAGE);
        progress = 0;
        state.addVP(vpPerStep[0], player);
        active = true;
    }

    public void advance(DiceMonasteryGameState state) {
        // first check that pilgrim has not been promoted
        if (!active) {
            throw new AssertionError("Should not be trying to advance an inactive Pilgrimage");
        }
        if (state.getMonkLocation(pilgrimId) != PILGRIMAGE) {
            active = false;
            return;
        }
        progress++;
        state.addVP(vpPerStep[progress], player);
        if (progress == vpPerStep.length - 1) {
            state.logEvent(Event.GameEvent.GAME_EVENT, () -> String.format("Monk reaches %s and gains %s", destination, finalReward));

            state.addResource(player, finalReward, 1);

            state.logEvent(Event.GameEvent.GAME_EVENT, () -> String.format("Monk returns from %s and is promoted", destination));
            Monk pilgrim = state.getMonkById(pilgrimId);
            state.moveMonk(pilgrimId, PILGRIMAGE, DORMITORY);
            pilgrim.promote(state);
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public Pilgrimage copy() {
        // if finished, then is immutable
        if (pilgrimId > -1 && !active)
            return this;
        return new Pilgrimage(this);
    }

    @Override
    public String toString() {
        return String.format("Pilgrimage to %s for %s (%d, %d)",
                destination,
                finalReward,
                pilgrimId, progress);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pilgrimage) {
            Pilgrimage other = (Pilgrimage) o;
            return other.pilgrimId == pilgrimId && other.active == active && other.destination.equals(destination) &&
                    other.minPiety == minPiety && other.cost == cost && Arrays.equals(other.vpPerStep, vpPerStep) &&
                    other.finalReward == finalReward &&
                    other.progress == progress && other.player == player;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pilgrimId, active, player, progress, destination, finalReward.ordinal(), minPiety, cost) + Arrays.hashCode(vpPerStep) * 7;
    }

    public static List<Pilgrimage> create(Component c) {
        int multiplicity = ((PropertyInt) c.getProperty(mHash)).value;
        String destination = ((PropertyString) c.getProperty(destinationHash)).value;
        String reward = ((PropertyString) c.getProperty(rewardHash)).value;
        int[] duration = ((PropertyIntArray) c.getProperty(durationHash)).getValues();
        int minPiety = ((PropertyInt) c.getProperty(minPietyHash)).value;
        int cost = ((PropertyInt) c.getProperty(costHash)).value;
        List<Pilgrimage> retValue = new ArrayList<>();
        for (int i = 0; i < multiplicity; i++)
            retValue.add(new Pilgrimage(destination, minPiety, cost, reward, duration));
        return retValue;
    }

}
