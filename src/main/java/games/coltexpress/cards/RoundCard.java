package games.coltexpress.cards;

import core.components.Card;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.actions.roundcardevents.RoundEvent;

import java.util.Arrays;
import java.util.Objects;

public class RoundCard extends Card {
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
    protected RoundEvent endRoundCardEvent;

    public RoundCard(String name, TurnType[] turnTypes, RoundEvent endRoundCardEvent) {
        super(name);
        this.turnTypes = turnTypes;
        this.endRoundCardEvent = endRoundCardEvent;
    }

    public RoundCard(String name, TurnType[] turnTypes, RoundEvent endRoundCardEvent, int ID) {
        super(name, ID);
        this.turnTypes = turnTypes;
        this.endRoundCardEvent = endRoundCardEvent;
    }

    public final void endRoundCardEvent(ColtExpressGameState gameState) {
        if (endRoundCardEvent != null) {
            endRoundCardEvent.execute(gameState);
        }
    }

    public RoundEvent getEndRoundCardEvent() {
        return endRoundCardEvent;
    }

    public TurnType[] getTurnTypes() {
        return turnTypes;
    }

    public String toString(){
        return Arrays.toString(turnTypes);
    }

    @Override
    public Card copy() {
        return new RoundCard(componentName, turnTypes.clone(), (endRoundCardEvent != null? (RoundEvent) endRoundCardEvent.copy() : null), componentID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RoundCard roundCard = (RoundCard) o;
        return Arrays.equals(turnTypes, roundCard.turnTypes) &&
                Objects.equals(endRoundCardEvent, roundCard.endRoundCardEvent);
    }

}
