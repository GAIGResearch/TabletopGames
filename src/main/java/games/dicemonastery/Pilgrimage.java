package games.dicemonastery;

import core.components.Component;
import games.dicemonastery.DiceMonasteryConstants.Resource;
import utilities.Utils;

import java.util.Objects;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.TREASURE.RELIC;

public class Pilgrimage extends Component {

    public enum DESTINATION {
        SANTIAGO(false, VIVID_RED_PIGMENT), ROME(false, VIVID_GREEN_PIGMENT),
        JERUSALEM(true, VIVID_BLUE_PIGMENT), ALEXANDRIA(true, VIVID_PURPLE_PIGMENT);

        public int minPiety;
        public int cost;
        public int[] vpPerStep;
        public Resource finalReward;

        DESTINATION(boolean longPilgrimage, Resource reward) {
            if (longPilgrimage) {
                minPiety = 5;
                cost = 6;
                vpPerStep = new int[]{1, 1, 2, 0};
            } else {
                minPiety = 3;
                cost = 3;
                vpPerStep = new int[]{1, 2, 0};
            }
            finalReward = reward;
        }
    }


    final boolean isRelic;
    final DESTINATION destination;

    int player = -1;
    boolean active;
    int pilgrimId = -1;
    int progress = -1;


    public Pilgrimage(DESTINATION destination, boolean isRelic) {
        super(Utils.ComponentType.CARD, "Pilgrimage to " + destination.name());
        this.destination = destination;
        this.isRelic = isRelic;
    }

    private Pilgrimage(Pilgrimage copy) {
        super(Utils.ComponentType.CARD, copy.componentName, copy.componentID);
        this.progress = copy.progress;
        this.pilgrimId = copy.pilgrimId;
        this.destination = copy.destination;
        this.active = copy.active;
        this.player = copy.player;
        this.isRelic = copy.isRelic;
    }

    public void startPilgrimage(Monk monk, DiceMonasteryGameState state) {
        pilgrimId = monk.getComponentID();
        player = monk.getOwnerId();
        state.addResource(player, SHILLINGS, -destination.cost);
        state.moveMonk(pilgrimId, GATEHOUSE, PILGRIMAGE);
        progress = 0;
        state.addVP(destination.vpPerStep[0], player);
        active = true;
    }

    public void advance(DiceMonasteryGameState state) {
        // first check that pilgrim has not been promoted
        if (state.getMonkLocation(pilgrimId) != PILGRIMAGE) {
            active = false;
            return;
        }
        progress++;
        state.addVP(destination.vpPerStep[progress], player);
        if (progress == destination.vpPerStep.length - 2) {
            DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder) state.getTurnOrder();
            dmto.logEvent(String.format("Monk reaches %s and gains %s", destination, isRelic ? "RELIC" : destination.finalReward), state);

            if (isRelic) {
                state.acquireTreasure(RELIC, player);
            } else {
                state.addResource(player, destination.finalReward, 1);
            }
        } else if (progress == destination.vpPerStep.length - 1)  {
            DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder) state.getTurnOrder();
            dmto.logEvent(String.format("Monk returns from %s and is promoted", destination), state);
            Monk pilgrim  = state.getMonkById(pilgrimId);
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
                isRelic ? "RELIC" : destination.finalReward,
                pilgrimId, progress);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pilgrimage) {
            Pilgrimage other = (Pilgrimage) o;
            return other.pilgrimId == pilgrimId && other.active == active && other.destination == destination &&
                    other.progress == progress && other.player == player;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pilgrimId, active, player, progress, destination) + super.hashCode() * 31;
    }

}
