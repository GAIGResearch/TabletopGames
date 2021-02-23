package games.catan.components;

public class Settlement {
    private int type; // settlement = 1, city = 2
    private int owner;
    private int x;
    private int y;
    private int vertex;

    public Settlement(int x, int y, int vertex, int owner){
        this.x = x;
        this.y = y;
        this.vertex = vertex;
        this.owner = owner;
        this.type = 1;
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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getVertex() {
        return vertex;
    }

    public Settlement copy(){
        Settlement copy = new Settlement(x, y, vertex, owner);
        copy.type = this.type;
        return copy;
    }
}
