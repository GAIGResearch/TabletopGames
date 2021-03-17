package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

import java.util.Objects;

public class ResourceIncGenRequirement implements Requirement<TMGameState> {

    final TMTypes.Resource resource;

    public ResourceIncGenRequirement(TMTypes.Resource resource) {
        this.resource = resource;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        // Check if this resource was increased for current player in this generation
        return gs.getPlayerResourceIncreaseGen()[gs.getCurrentPlayer()].get(resource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceIncGenRequirement)) return false;
        ResourceIncGenRequirement that = (ResourceIncGenRequirement) o;
        return resource == that.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource);
    }

    public ResourceIncGenRequirement copy() {
        return this;
    }
}
