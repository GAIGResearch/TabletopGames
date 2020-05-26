package games.coltexpress.cards;

import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;

import java.util.Arrays;

public class RoundCard {
    /*
    Round Cards
    - - - - - - - - - - - - - - 2-4 players - - - 5-6 players
    1) Angry Marshall - - - - - - O O X R - - - - - O O R
    2) Swivel Arm - - - - - - - - O X O O - - - - - O X O
    3) Braking- - - - - - - - - - O O O O - - - - - O X O O
    4) Take it All- - - - - - - - O X 2 O - - - - - O 2 R
    5) Passenger Rebellion- - - - O O X O O - - - - O X O R
    6) Tunnel - - - - - - - - - - O X O X O - - - - O X O X
    7) Bridge - - - - - - - - - - O 2 O - - - - - - O 2

    Station Cards

    1) Marshall's Revenge - - - O O X O
    2) Hostage- - - - - - - - - O O X O
    3) Pick Pocket- - - - - - - O O X O

    O - normal turn
    X - tunnel turn, face down
    R - reverse turn
    2 - speed up turn

    1) Angry Marshall - The Marshall shoots all bandits on the roof of his car and then moves one car toward the caboose.
    2) Swivel Arm - All bandits on the roof of the train are swept to the caboose.
    3) Braking - All bandits on the roof of the train move one car toward the locomotive.
    4) Take it All - The Marshall drops a second strongbox.
    5) Passenger Rebellion - All bandits in the train receive one Neutral Bullet card.
    6) Tunnel - No special event.
    7) Bridge - No special event.

    1) Marshall's Revenge - All bandits on the roof of the Marshall's car drop their least valuable purse.
    2) Hostage - All bandits in or on the locomotive collect $250 ransom.
    3) Pick Pocket - Any bandit alone in or on a car can pick up a purse if there is one.
    */

    public enum TurnType{
        NormalTurn("O"),
        HiddenTurn("X"),
        DoubleTurn("2"),
        ReverseTurn("R");

        private final String type;
        TurnType(String type){this.type = type; }
        public String toString(){return type; }
    }

    protected TurnType[] turnTypes;
    protected AbstractAction endRoundCardEvent;

    public RoundCard(TurnType[] turnTypes, AbstractAction endRoundCardEvent) {
        this.turnTypes = turnTypes;
        this.endRoundCardEvent = endRoundCardEvent;
    }

    public final void endRoundCardEvent(ColtExpressGameState gameState) {
        if (endRoundCardEvent != null) {
            endRoundCardEvent.execute(gameState);
        }
    }

    public AbstractAction getEndRoundCardEvent() {
        return endRoundCardEvent;
    }

    public TurnType[] getTurnTypes() {
        return turnTypes;
    }

    public String toString(){
        return Arrays.toString(turnTypes);
    }

}
