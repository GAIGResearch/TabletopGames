package games.catan.components;

import games.catan.actions.BuildRoadByRef;

public class Road implements Copiable {
    private int owner;
    private static int counter = 0;
    private int id;

    public Road(int owner){
        this.owner = owner;
        this.id = counter++;
    }

    public int getOwner(){
        return owner;
    }

    public void setOwner(int playerID){
        this.owner = playerID;
    }

    public int getID() {
        return id;
    }

    public Road copy(){
        Road copy = new Road(owner);
        copy.id = id;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Road){
            Road otherAction = (Road)obj;
            return id == otherAction.id;
        }
        return false;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return this.copy();
    }
}
