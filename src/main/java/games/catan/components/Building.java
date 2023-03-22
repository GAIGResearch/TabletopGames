package games.catan.components;

import core.components.BoardNode;
import games.catan.CatanParameters;

import java.util.Objects;

import static games.catan.CatanConstants.HEX_SIDES;

public class Building extends BoardNode {
    public enum Type {
        Settlement,
        City
    }
    private Type type;
    private CatanParameters.Resource harbour;

    public Building(int owner){
        super(HEX_SIDES, "Settlement");
        setOwnerId(owner);
        this.type = Type.Settlement;
    }
    public Building(int owner, int id){
        super(HEX_SIDES, "Settlement", id);
        setOwnerId(owner);
        this.type = Type.Settlement;
    }

    public boolean upgrade(){
        if (this.type == Type.Settlement){
            this.type = Type.City;
            return true;
        }
        return false;
    }

    public Type getBuildingType(){
        return type;
    }

    public CatanParameters.Resource getHarbour() {
        return harbour;
    }
    public void setHarbour(CatanParameters.Resource harbour) {
        this.harbour = harbour;
    }

    public Building copy(){
        Building copy = new Building(ownerId, componentID);
        copy.type = this.type;
        copy.harbour = this.harbour;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Building)) return false;
        if (!super.equals(o)) return false;
        Building that = (Building) o;
        return type == that.type && harbour == that.harbour;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, harbour);
    }
}
