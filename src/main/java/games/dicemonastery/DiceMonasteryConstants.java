package games.dicemonastery;

import core.interfaces.IGamePhase;

import java.awt.*;
import java.util.Arrays;

import static games.dicemonastery.DiceMonasteryConstants.InkColour.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class DiceMonasteryConstants {

    public static Color[] playerColours = {Color.GREEN, Color.RED, Color.BLUE, Color.ORANGE};

    public enum ActionArea {
        MEADOW(1), KITCHEN(2), WORKSHOP(3),
        GATEHOUSE(1), LIBRARY(4), CHAPEL(1),
        DORMITORY(0), STOREROOM(0), SUPPLY(0),
        RETIRED(0), GRAVEYARD(0);

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
        GRAIN, HONEY, WAX, SKEP, BREAD, CALF_SKIN, VELLUM, CANDLE, BERRIES,
        SHILLINGS, PRAYER,
        BEER, PROTO_BEER_1, PROTO_BEER_2, MEAD, PROTO_MEAD_1, PROTO_MEAD_2,
        PALE_GREEN_INK(GREEN, false), PALE_BLUE_INK(BLUE,  false), PALE_RED_INK(RED,  false),
        PALE_GREEN_PIGMENT(GREEN, false, PALE_GREEN_INK),
        PALE_BLUE_PIGMENT(BLUE, false, PALE_BLUE_INK),
        PALE_RED_PIGMENT(RED, false, PALE_RED_INK),
        VIVID_GREEN_INK(GREEN, true), VIVID_BLUE_INK(BLUE, true),
        VIVID_RED_INK(RED, true), VIVID_PURPLE_INK(PURPLE, true),
        VIVID_GREEN_PIGMENT(GREEN, true, VIVID_GREEN_INK),
        VIVID_BLUE_PIGMENT(BLUE, true, VIVID_BLUE_INK),
        VIVID_RED_PIGMENT(RED, true, VIVID_RED_INK),
        VIVID_PURPLE_PIGMENT(PURPLE, true, VIVID_PURPLE_INK);


        public final boolean isInk;
        public final boolean isPigment;
        public final boolean isVivid;
        public final InkColour colour;
        public final Resource processedTo;

        Resource() {
            this(false, false, NONE, false, null);
        }

        Resource(InkColour colour, boolean vivid) {
            this(true, false, colour, vivid, null);
        }

        Resource(InkColour colour, boolean vivid, Resource processedTo) {
            this(false, true, colour, vivid, processedTo);
        }

        Resource(boolean ink, boolean pigment, InkColour colour, boolean vivid, Resource processedTo) {
            isInk = ink;
            isPigment = pigment;
            this.colour = colour;
            isVivid = vivid;
            this.processedTo = processedTo;
        }
    }

    public enum Phase implements IGamePhase {
        PLACE_MONKS, USE_MONKS
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

    public final static Resource[][] gospelInks = {
            {VIVID_GREEN_INK, VIVID_BLUE_INK},
            {VIVID_RED_INK, VIVID_BLUE_INK},
            {VIVID_RED_INK, VIVID_PURPLE_INK},
            {VIVID_GREEN_INK, VIVID_PURPLE_INK}
    };

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

    public final static int[] PSALM_REWARDS = {4, 3, 3, 2, 2, 2};
    public final static int[] EPISTLE_REWARDS = {5, 4, 4, 3, 3};
    public final static int[] LITURGY_REWARDS = {6, 5, 5, 4};
    public final static int GOSPEL_REWARD = 8;

    public enum ILLUMINATED_TEXT {
        PSALM(3, 1, 1, 1, new Resource[]{}, PSALM_REWARDS),
        EPISTLE(4, 2, 1, 2, new Resource[]{}, EPISTLE_REWARDS),
        LITURGY(5, 3, 2, 2, new Resource[]{}, LITURGY_REWARDS),
        GOSPEL_MARK(6, 2, 2, 2, new Resource[]{VIVID_GREEN_INK, VIVID_BLUE_INK}, new int[]{GOSPEL_REWARD}),
        GOSPEL_LUKE(6, 2, 2, 2, new Resource[]{VIVID_RED_INK, VIVID_BLUE_INK}, new int[]{GOSPEL_REWARD}),
        GOSPEL_JOHN(6, 2, 2, 2, new Resource[]{VIVID_RED_INK, VIVID_PURPLE_INK}, new int[]{GOSPEL_REWARD}),
        GOSPEL_MATHEW(6, 2, 2, 2, new Resource[]{VIVID_GREEN_INK, VIVID_PURPLE_INK}, new int[]{GOSPEL_REWARD});

        public final int ap;
        public final int differentInks;
        public final int vellum;
        public final int candles;
        public final Resource[] specialInks;
        public final int[] rewards;

        ILLUMINATED_TEXT(int ap, int inks, int vellum, int candles, Resource[] specialInks, int[] rewards) {
            this.ap = ap;
            this.differentInks = inks;
            this.vellum = vellum;
            this.candles = candles;
            this.specialInks = specialInks;
            this.rewards = rewards;
        }
    }

    public enum TREASURE {
        CAPE(4, 1, 1),
        ROBE(8, 2, 1),
        CANDLESTICK(12, 3, 1),
        LECTERN(16, 4, 1),
        CHALICE(20, 5, 1),
        MITRE(24, 6, 1),
        PLATE(28, 7, 1),
        ALTAR_CROSS(32, 8, 1);

        public final int cost;
        public final int vp;
        public final int limit;

        TREASURE(int cost, int vp, int limit) {
            this.cost = cost;
            this.vp = vp;
            this.limit  = limit;
        }

    }

}
