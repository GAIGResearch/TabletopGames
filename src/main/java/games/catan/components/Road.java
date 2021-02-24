package games.catan.components;

public class Road {
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
}
