package uno;

import core.GameParameters;

public class UnoParameters extends GameParameters {
        public int max_cards = 108;
        public int initial_cards = 7;

        @Override
        public GameParameters copy() {
                UnoParameters upCopy = new UnoParameters();

                upCopy.max_cards = max_cards;
                upCopy.initial_cards = initial_cards;
                return upCopy;
        }
}
