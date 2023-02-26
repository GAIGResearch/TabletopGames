package games.coltexpress;

import core.actions.AbstractAction;
import games.coltexpress.actions.roundcardevents.*;
import games.coltexpress.cards.RoundCard;
import utilities.Group;
import utilities.Pair;

import java.awt.*;

public class ColtExpressTypes {

    // Character types implemented
    public enum CharacterType {
        Ghost (Color.white, "Ghost is one stealthy bandit. During your first turn of each Round, you can play your Action card face-down on the common deck. If you choose to draw three cards instead of playing an Action card during the first turn, you cannot use Ghost's special ability later in this Round."),
        Cheyenne (new Color(66, 175, 94), "Cheyenne is an outstanding pickpocket. When punching a Bandit, you can take the Purse he has just lost. If he has lost a Jewel or a Strongbox (your choice), the Loot just falls on the floor (as usual)."),
        Django (new Color(43, 43, 43), "Django's shots are so powerful that they knock the other bandits back. When shooting a Bandit, make him move one Car in the direction of fire, bearing in mind that Bandits can never leave the train."),
        Tuco (new Color(225, 7, 20), "Tuco's shots are not stopped by the roof. You cannot be the target of a Fire action or a Punch action if there is another Bandit who can be targeted, too."),
        Doc (new Color(25, 183, 199), "Doc is the smartest Bandit of the party. At the beginning of each Round, draw seven cards instead of six."),
        Belle (new Color(233, 13, 130), "Belle's beauty is her best weapon. You can shoot a Bandit who is on the same Car as you are, on the other level, through the roof of your Car.");

        private final Color color;
        private final String power;

        CharacterType(Color color, String power) {
            this.color = color;
            this.power = power;
        }

        public Color getColor() {
            return color;
        }

        public String getPower() {
            return power;
        }
    }

    // Configurations of end cards, pairs contain:
    // a - turn type sequence
    // b - end card event
    public enum EndRoundCard {
        MarshallsRevenge("Marshall's Revenge",  new Pair(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.NormalTurn,
                        RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn},
                new EndCardMarshallsRevenge())),
        Hostage("Hostage", new Pair(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.NormalTurn,
                        RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn},
                new EndCardHostage())),
        PickPocket("Pick Pocket", new Pair(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.NormalTurn,
                RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn},
                new EndCardPickPocket()));

        private String key;
        private Pair<RoundCard.TurnType[], RoundEvent> content;
        EndRoundCard(String key, Pair<RoundCard.TurnType[], RoundEvent> content) {
            this.key = key;
            this.content = content;
        }

        public String getKey() {
            return key;
        }

        public RoundCard.TurnType[] getTurnTypeSequence() {
            return content.a;
        }

        public RoundEvent getEndCardEvent() {
            return content.b;
        }
    }

    // Configurations of normal round cards, groups contain:
    // - a: turn type sequence if nPlayers <= 4
    // - b: turn type sequence if nPlayers > 4
    // - c: end card event (null if none)
    public enum RegularRoundCard {
        AngryMarshall("Angry Marshall", new Group(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.ReverseTurn},
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.NormalTurn, RoundCard.TurnType.ReverseTurn},
                new RoundCardAngryMarshall())),
        Braking("Braking", new Group(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn},
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.HiddenTurn},
                new RoundCardBraking())),
        Bridge("Bridge", new Group(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.DoubleTurn, RoundCard.TurnType.NormalTurn},
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.DoubleTurn},
                null)),
        PassengerRebellion("Passenger Rebellion", new Group(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn, RoundCard.TurnType.NormalTurn},
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn, RoundCard.TurnType.ReverseTurn},
                new RoundCardPassengerRebellion())),
        SwivelArm("Swivel Arm", new Group(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn, RoundCard.TurnType.NormalTurn},
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn},
                new RoundCardSwivelArm())),
        TakeItAll("Take It All", new Group(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.DoubleTurn, RoundCard.TurnType.ReverseTurn},
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.DoubleTurn, RoundCard.TurnType.ReverseTurn},
                new RoundCardTakeItAll())),
        Tunnel("Tunnel", new Group(
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn},
                new RoundCard.TurnType[] {RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn, RoundCard.TurnType.NormalTurn, RoundCard.TurnType.HiddenTurn},
                null));

        private String key;
        private Group<RoundCard.TurnType[], RoundCard.TurnType[], RoundEvent> content;
        RegularRoundCard(String key, Group<RoundCard.TurnType[], RoundCard.TurnType[], RoundEvent> content) {
            this.key = key;
            this.content = content;
        }

        public String getKey() {
            return key;
        }

        public RoundCard.TurnType[] getTurnTypeSequence(int nPlayers) {
            if (nPlayers <= 4)
                return content.a;
            else return content.b;
        }

        public RoundEvent getEndCardEvent() {
            return content.c;
        }
    }

    // Loot types available for this game.
    public enum LootType {
        Purse,
        Jewel,
        Strongbox
    }
}
