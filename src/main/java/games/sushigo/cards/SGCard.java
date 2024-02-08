package games.sushigo.cards;

import core.components.Card;
import core.components.Counter;
import evaluation.metrics.Event;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;

import java.util.HashSet;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SGCard extends Card {

    public enum SGCardType {
        Maki(new int[]{1, 2, 3}),
        Tempura,
        Sashimi,
        Dumpling,
        SquidNigiri,
        SalmonNigiri,
        EggNigiri,
        Wasabi,
        Chopsticks,
        Pudding(false);

        static {
            Tempura.onReveal = (gs, p) -> {
                // Adds points for pairs
                Counter amount = gs.getPlayedCardTypes(Tempura, p);
                if (amount.getValue() % 2 == 0) {
                    int value = ((SGParameters) gs.getGameParameters()).valueTempuraPair;
                    gs.addPlayerScore(p, value, Tempura);
                }
            };
            Sashimi.onReveal = (gs, p) -> {
                // Adds points for triplets
                Counter amount = gs.getPlayedCardTypes(Sashimi, p);
                if (amount.getValue() % 3 == 0) {
                    int value = ((SGParameters) gs.getGameParameters()).valueSashimiTriple;
                    gs.addPlayerScore(p, value, Sashimi);
                }
            };
            Dumpling.onReveal = (gs, p) -> {
                // Add points depending on how many were collected, parameter array used for increments
                Counter amount = gs.getPlayedCardTypes(Dumpling, p);
                int idx = Math.min(amount.getValue(), ((SGParameters) gs.getGameParameters()).valueDumpling.length) - 1;
                int value = ((SGParameters) gs.getGameParameters()).valueDumpling[idx];
                gs.addPlayerScore(p, value, Dumpling);
            };
            SquidNigiri.onReveal = (gs, p) -> {
                // Gives points, more if played on Wasabi
                int value = ((SGParameters) gs.getGameParameters()).valueSquidNigiri;
                if (gs.getPlayedCardTypes()[p].get(Wasabi).getValue() > 0) {
                    value *= ((SGParameters) gs.getGameParameters()).multiplierWasabi;
                    gs.getPlayedCardTypes()[p].get(Wasabi).decrement(1);
                }
                gs.addPlayerScore(p, value, SquidNigiri);
            };
            SalmonNigiri.onReveal = (gs, p) -> {
                // Gives points, more if played on Wasabi
                int value = ((SGParameters) gs.getGameParameters()).valueSalmonNigiri;
                if (gs.getPlayedCardTypes()[p].get(Wasabi).getValue() > 0) {
                    value *= ((SGParameters) gs.getGameParameters()).multiplierWasabi;
                    gs.getPlayedCardTypes()[p].get(Wasabi).decrement(1);
                }
                gs.addPlayerScore(p, value, SalmonNigiri);
            };
            EggNigiri.onReveal = (gs, p) -> {
                // Gives points, more if played on Wasabi
                int value = ((SGParameters) gs.getGameParameters()).valueEggNigiri;
                if (gs.getPlayedCardTypes()[p].get(Wasabi).getValue() > 0) {
                    value *= ((SGParameters) gs.getGameParameters()).multiplierWasabi;
                    gs.getPlayedCardTypes()[p].get(Wasabi).decrement(1);
                }
                gs.addPlayerScore(p, value, EggNigiri);
            };

            Maki.onRoundEnd = gs -> {
                // Gives points to the player that has the most Maki rolls, and also to second most

                // Calculate who has the most points and who has the second most points
                int most = 0;
                int secondMost = 0;
                HashSet<Integer> mostPlayers = new HashSet<>();
                HashSet<Integer> secondPlayers = new HashSet<>();
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    int nMakiRolls = gs.getPlayedCardTypes()[i].get(Maki).getValue();

                    if (nMakiRolls > most) {
                        secondMost = most;
                        secondPlayers.clear();
                        secondPlayers.addAll(mostPlayers);

                        most = nMakiRolls;
                        mostPlayers.clear();
                        mostPlayers.add(i);
                    } else if (nMakiRolls == most && nMakiRolls != 0) mostPlayers.add(i);
                    else if (nMakiRolls > secondMost) {
                        secondMost = nMakiRolls;
                        secondPlayers.clear();
                        secondPlayers.add(i);
                    } else if (nMakiRolls == secondMost && nMakiRolls != 0) secondPlayers.add(i);
                }

                // Calculate the score each player gets and award the points
                SGParameters parameters = (SGParameters) gs.getGameParameters();
                int mostScore = parameters.valueMakiMost;
                int secondScore = parameters.valueMakiSecond;
                if (!mostPlayers.isEmpty()) {
                    // Best score is split among the tied players with no remainder
                    mostScore /= mostPlayers.size();
                    for (Integer mostPlayer : mostPlayers) {
                        gs.addPlayerScore(mostPlayer, mostScore, Maki);
                        if (gs.getCoreGameParameters().recordEventHistory) {
                            gs.logEvent(Event.GameEvent.GAME_EVENT, "Player " + mostPlayer + " scores " + mostScore + " from Maki rolls (most:" + most + ")");
                        }
                    }
                }
                if (!secondPlayers.isEmpty() && mostPlayers.size() == 1) {
                    // Second-best score is split among the tied players with no remainder, only awarded if no ties for most
                    secondScore /= secondPlayers.size();
                    for (Integer secondPlayer : secondPlayers) {
                        gs.addPlayerScore(secondPlayer, secondScore, Maki);
                        if (gs.getCoreGameParameters().recordEventHistory) {
                            gs.logEvent(Event.GameEvent.GAME_EVENT, "Player " + secondPlayer + " scores " + secondScore + " from Maki rolls (second most:" + secondMost + ")");
                        }
                    }
                }
            };

            Pudding.onGameEnd = gs -> {
                // Gives points at the end for most pudding cards. Points lost for least pudding cards (not in 2-player games)
                SGParameters parameters = (SGParameters) gs.getGameParameters();

                //Calculate who has the most points and who has the least points
                int best = gs.getPlayedCardTypes()[0].get(Pudding).getValue();
                int worst = best;
                HashSet<Integer> mostPlayers = new HashSet<>();
                HashSet<Integer> leastPlayers = new HashSet<>();
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    int nPuddings = gs.getPlayedCardTypes()[i].get(Pudding).getValue();

                    if (nPuddings > best) {
                        best = nPuddings;
                        mostPlayers.clear();
                        mostPlayers.add(i);
                    } else if (nPuddings == best && nPuddings != 0) mostPlayers.add(i);
                    if (nPuddings < worst) {
                        worst = nPuddings;
                        leastPlayers.clear();
                        leastPlayers.add(i);
                    } else if (nPuddings == worst) leastPlayers.add(i);
                }
                if (best > worst) {

                    // Calculate the score each player gets, only if there's a difference in number of puddings, otherwise no one gets points
                    int mostScore = parameters.valuePuddingMost;
                    int leastScore = parameters.valuePuddingLeast;
                    if (!mostPlayers.isEmpty()) {
                        // Best score is split among the tied players with no remainder
                        mostScore /= mostPlayers.size();
                        for (Integer mostPlayer : mostPlayers) {
                            gs.addPlayerScore(mostPlayer, mostScore, Pudding);
                            if (gs.getCoreGameParameters().recordEventHistory) {
                                gs.logEvent(Event.GameEvent.GAME_EVENT, "Player " + mostPlayer + " scores " + mostScore + " from Puddings (most:" + best + ")");
                            }
                        }
                    }
                    if (!leastPlayers.isEmpty() && gs.getNPlayers() > 2) {
                        // Least score is split among the tied players with no remainder, only awarded in games with more than 2 players
                        leastScore /= leastPlayers.size();
                        for (Integer leastPlayer : leastPlayers) {
                            gs.addPlayerScore(leastPlayer, leastScore, Pudding);
                            if (gs.getCoreGameParameters().recordEventHistory) {
                                gs.logEvent(Event.GameEvent.GAME_EVENT, "Player " + leastPlayer + " scores " + leastScore + " from Puddings (least:" + worst + ")");
                            }
                        }
                    }
                }
            };
        }

        private BiConsumer<SGGameState, Integer> onReveal;  // effectively final, should not be modified
        private Consumer<SGGameState> onRoundEnd, onGameEnd;  // effectively final, should not be modified
        private boolean discardedBetweenRounds = true;
        private int[] iconCountVariation = new int[]{1};

        SGCardType() {
        }

        SGCardType(boolean discardedBetweenRounds) {
            this.discardedBetweenRounds = discardedBetweenRounds;
        }

        SGCardType(int[] iconCountVariation) {
            this.iconCountVariation = iconCountVariation;
        }

        public void onReveal(SGGameState gs, int playerId) {
            if (onReveal != null) onReveal.accept(gs, playerId);
        }

        public void onRoundEnd(SGGameState gs) {
            if (onRoundEnd != null) onRoundEnd.accept(gs);
        }

        public void onGameEnd(SGGameState gs) {
            if (onGameEnd != null) onGameEnd.accept(gs);
        }

        public boolean isDiscardedBetweenRounds() {
            return discardedBetweenRounds;
        }

        public int[] getIconCountVariation() {
            return iconCountVariation;
        }
    }

    public final SGCardType type;
    public final int count;  // Number of tokens of this type on the card. 1 by default, could be 1, 2, 3 for Makis

    public SGCard(SGCardType type) {
        super(type.toString());
        this.type = type;
        this.count = 1;
    }

    public SGCard(SGCardType type, int count) {
        super(type.toString());
        this.type = type;
        this.count = count;
    }

    @Override
    public Card copy() {
        return this; // immutable
    }

    @Override
    public String toString() {
        return type.toString() + (count > 1 ? "-" + count : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SGCard)) return false;
        if (!super.equals(o)) return false;
        SGCard sgCard = (SGCard) o;
        return count == sgCard.count && type == sgCard.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, count);
    }
}
