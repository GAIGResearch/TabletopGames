package games.catan.components;

import games.catan.CatanParameters;

import java.util.Objects;

import static games.catan.CatanParameters.HarborTypes.*;

public class Settlement implements Copiable {
    private int type; // settlement = 1, city = 2
    private int owner;
    private static int counter = 0;
    private int id;
    private CatanParameters.HarborTypes harbour = NONE;

    public Settlement(int owner){
        this.owner = owner;
        this.type = 1;
        this.id = counter++;
    }
    public Settlement(int owner, int id){
        this.owner = owner;
        this.type = 1;
        this.id = id;
    }

    public boolean upgrade(){
        if (this.type ==1){
            this.type = 2;
            return true;
        }
        return false;
    }

    public int getOwner(){
        return owner;
    }

    public void setOwner(int playerID){
        this.owner = playerID;
    }

    public int getType(){
        return type;
    }

    public int getID(){
        return id;
    }

    public CatanParameters.HarborTypes getHarbour() {
        return harbour;
    }

    public void setHarbour(CatanParameters.HarborTypes harbour) {
        this.harbour = harbour;
    }

    public Settlement copy(){
        Settlement copy = new Settlement(owner, id);
        copy.type = this.type;
        copy.harbour = this.harbour;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Settlement){
            Settlement otherAction = (Settlement)obj;
            return id == otherAction.id && type == otherAction.type && harbour == otherAction.harbour &&
                    owner == otherAction.owner;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public int extendedHashCode() {
        return Objects.hash(id, type, harbour, owner);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return this.copy();
    }
}
