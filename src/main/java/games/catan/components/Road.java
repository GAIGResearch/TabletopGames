package games.catan.components;

public class Road {
    private int owner;

    public Road(int owner){
        this.owner = owner;
    }

    public int getOwner(){
        return owner;
    }

    public void setOwner(int playerID){
        this.owner = playerID;
    }
}
