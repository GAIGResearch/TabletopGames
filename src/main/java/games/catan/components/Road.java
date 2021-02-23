package games.catan.components;

public class Road {
    private int owner;
    private int x;
    private int y;
    private int edge;

    public Road(int x, int y, int edge, int owner){
        this.x = x;
        this.y = y;
        this.edge = edge;
        this.owner = owner;
    }

    public int getOwner(){
        return owner;
    }

    public void setOwner(int playerID){
        this.owner = playerID;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getEdge() {
        return edge;
    }

    public Road copy(){
        return new Road(x, y , edge, owner);
    }
}
