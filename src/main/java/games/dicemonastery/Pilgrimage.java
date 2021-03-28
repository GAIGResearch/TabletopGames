package games.dicemonastery;

import core.components.Card;

public class Pilgrimage extends Card {

    public enum DESTINATION {
        SANTIAGO(false), ROME(false), JERUSALEM(true), ALEXANDRIA(true);

        public int minPiety;
        public int cost;
        public int[] vpPerStep;
        public DiceMonasteryConstants.Resource finalReward;

        DESTINATION(boolean longPilgrimage) {
            if (longPilgrimage) {
                minPiety = 5;
                cost = 6;
                vpPerStep = new int[]{1, 1, 2};
            } else {
                minPiety = 3;
                cost = 3;
                vpPerStep = new int[]{1, 2};
            }

        }
    }

}
