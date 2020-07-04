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
        Ghost (Color.white),
        Cheyenne (new Color(66, 175, 94)),
        Django (new Color(43, 43, 43)),
        Tuco (new Color(225, 7, 20)),
        Doc (new Color(25, 183, 199)),
        Belle (new Color(233, 13, 130)),
        Marshal(new Color(242, 189, 24));

        private Color color;

        CharacterType(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    // Configurations of end cards, pairs contain:
    // a - turn type sequence
    // b - end card event
    public enum EndRoundCard {
        MarshallsRevenge("Marhsalls Revenge",  new Pair(
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
        private Pair<RoundCard.TurnType[], AbstractAction> content;
        EndRoundCard(String key, Pair<RoundCard.TurnType[], AbstractAction> content) {
            this.key = key;
            this.content = content;
        }

        public String getKey() {
            return key;
        }

        public RoundCard.TurnType[] getTurnTypeSequence() {
            return content.a;
        }

        public AbstractAction getEndCardEvent() {
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
        private Group<RoundCard.TurnType[], RoundCard.TurnType[], AbstractAction> content;
        RegularRoundCard(String key, Group<RoundCard.TurnType[], RoundCard.TurnType[], AbstractAction> content) {
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

        public AbstractAction getEndCardEvent() {
            return content.c;
        }
    }

    // Loot types available for this game.
    public enum LootType {
        Purse,
        Jewel,
        Strongbox,
        Unknown
    }
}
