package games.catan.components;

import core.components.BoardNodeWithEdges;
import games.catan.CatanParameters;

import java.util.Objects;

public class Building extends BoardNodeWithEdges {
    public enum Type {
        Settlement,
        City
    }
    private Type type;
    private CatanParameters.Resource harbour;

    public Building(){
        this(-1);
    }
    public Building(int owner){
        super();
        setOwnerId(owner);
        this.type = Type.Settlement;
    }
    public Building(int owner, int id){
        super(owner, id);
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
    public void setBuildingType(Type type) {
        this.type = type;
    }

    public CatanParameters.Resource getHarbour() {
        return harbour;
    }
    public void setHarbour(CatanParameters.Resource harbour) {
        this.harbour = harbour;
    }

    public Building copy(){  // Super copies of neighbour nodes outside
        Building copy = new Building(ownerId, componentID);
        copy.harbour = harbour;
        copy.type = type;
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

    @Override
    public String toString() {
        return type + (harbour != null? " (H: " + harbour + ")" : "") + " ns:" + neighbourEdgeMapping.size();
    }
}
