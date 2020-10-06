package games.dominion;

public class DominionPlayerState {

    // encapsulation of the state for a specific player
    int playerID;
    int buys;
    int actions;
    int treasure;

    public DominionPlayerState(int playerID) {
        this.playerID = playerID;
        endOfTurnCleanUp();
    }

    public void endOfTurnCleanUp() {
        buys = 1;
        actions = 1;
        treasure = 0;
    }

    public int currentBuys() {
        return buys;
    }

    public int currentActions() {
        return actions;
    }

    public int availableSpend() {
        return treasure;
    }

    public void changeActions(int delta) {
        actions += delta;
    }

    public void changeBuys(int delta) {
        buys += delta;
    }

    public void changeSpend(int delta) {
        treasure += delta;
    }

    public DominionPlayerState copy() {
        DominionPlayerState retValue = new DominionPlayerState(playerID);
        retValue.buys = buys;
        retValue.actions = actions;
        retValue.treasure = treasure;
        return retValue;
    }


}
