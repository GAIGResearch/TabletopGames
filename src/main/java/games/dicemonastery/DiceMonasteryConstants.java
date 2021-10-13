package games.dicemonastery;

import core.interfaces.IGamePhase;

import java.awt.*;
import java.util.Arrays;

import static games.dicemonastery.DiceMonasteryConstants.InkColour.*;

public class DiceMonasteryConstants {

    public static Color[] playerColours = {Color.GREEN, Color.RED, Color.BLUE, Color.ORANGE};

    public enum ActionArea {
        MEADOW(1), KITCHEN(2), WORKSHOP(3),
        GATEHOUSE(1), LIBRARY(3), CHAPEL(1),
        DORMITORY(0), STOREROOM(0), SUPPLY(0),
        PILGRIMAGE(0), RETIRED(0), GRAVEYARD(0);
        public final int dieMinimum;

        ActionArea(int dieMinimum) {
            this.dieMinimum = dieMinimum;
        }

        public ActionArea next() {
            switch (this) {
                case MEADOW:
                    return KITCHEN;
                case KITCHEN:
                    return WORKSHOP;
                case WORKSHOP:
                    return GATEHOUSE;
                case GATEHOUSE:
                    return LIBRARY;
                case LIBRARY:
                    return CHAPEL;
                case CHAPEL:
                    return MEADOW;
                default:
                    throw new AssertionError("Should not be processing " + this);
            }
        }

    }

    public enum InkColour {
        GREEN, BLUE, RED, PURPLE, NONE;
    }

    public enum Resource {
        GRAIN, HONEY, WAX, SKEP, BREAD, CALF_SKIN, VELLUM, CANDLE,
        SHILLINGS, PRAYER,
        BEER, PROTO_BEER_1, PROTO_BEER_2, MEAD, PROTO_MEAD_1, PROTO_MEAD_2,
        PALE_GREEN_INK(GREEN, 0, false), PALE_BLUE_INK(BLUE, 0, false), PALE_RED_INK(RED, 0, false),
        PALE_GREEN_PIGMENT(GREEN, false, PALE_GREEN_INK),
        PALE_BLUE_PIGMENT(BLUE, false, PALE_BLUE_INK),
        PALE_RED_PIGMENT(RED, false, PALE_RED_INK),
        VIVID_GREEN_INK(GREEN, 1, true), VIVID_BLUE_INK(BLUE, 2, true),
        VIVID_RED_INK(RED, 1, true), VIVID_PURPLE_INK(PURPLE, 2, true),
        VIVID_GREEN_PIGMENT(GREEN, true, VIVID_GREEN_INK),
        VIVID_BLUE_PIGMENT(BLUE, true, VIVID_BLUE_INK),
        VIVID_RED_PIGMENT(RED, true, VIVID_RED_INK),
        VIVID_PURPLE_PIGMENT(PURPLE, true, VIVID_PURPLE_INK);


        public final boolean isInk;
        public final boolean isPigment;
        public final boolean isVivid;
        public final int vpBonus;
        public final InkColour colour;
        public final Resource processedTo;

        Resource() {
            this(false, false, NONE, false, 0, null);
        }

        Resource(InkColour colour, int vp, boolean vivid) {
            this(true, false, colour, vivid, vp, null);
        }

        Resource(InkColour colour, boolean vivid, Resource processedTo) {
            this(false, true, colour, vivid, 0, processedTo);
        }

        Resource(boolean ink, boolean pigment, InkColour colour, boolean vivid, int vp, Resource processedTo) {
            isInk = ink;
            isPigment = pigment;
            this.colour = colour;
            isVivid = vivid;
            vpBonus = vp;
            this.processedTo = processedTo;
        }
    }

    public enum Phase implements IGamePhase {
        PLACE_MONKS, USE_MONKS, BID, SACRIFICE
    }

    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER;

        public Season next() {
            switch (this) {
                case SPRING:
                    return SUMMER;
                case SUMMER:
                    return AUTUMN;
                case AUTUMN:
                    return WINTER;
                case WINTER:
                    return SPRING;
                default:
                    throw new AssertionError("Should not be processing " + this);
            }
        }
    }

    public final static int[] RETIREMENT_REWARDS = {5, 4, 4, 3, 3, 2, 2, 2};

    // rows are number of players; columns are ordinal position in bidding
    public final static int[][] VIKING_REWARDS = {
            {0, 0, 0, 0},
            {2, 0, 0, 0},
            {4, 2, 0, 0},
            {6, 4, 2, 0}
    };

    public enum BONUS_TOKEN {
        PROMOTION(48), DEVOTION(24), PRESTIGE(12), DONATION(12);

        private final int number;

        BONUS_TOKEN(int number) {
            this.number = number;
        }

        public double getChance() {
            return number / (double) Arrays.stream(BONUS_TOKEN.values()).mapToInt(t -> t.number).sum();
        }
    }

}
