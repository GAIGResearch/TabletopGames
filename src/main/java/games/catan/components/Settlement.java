package games.catan.components;

public class Settlement {
    private int type; // settlement = 1, city = 2
    private int owner;
    private static int counter = 0;
    private int id;

    public Settlement(int owner){
        this.owner = owner;
        this.type = 1;
        this.id = counter++;
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


    public Settlement copy(){
        Settlement copy = new Settlement(owner);
        copy.type = this.type;
        copy.id = id;
        return copy;
    }
}
