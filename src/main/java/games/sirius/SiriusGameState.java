package games.sirius;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IComponentContainer;
import games.GameType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static games.sirius.SiriusConstants.MoonType.MINING;
import static games.sirius.SiriusConstants.MoonType.PROCESSING;
import static games.sirius.SiriusConstants.SiriusCardType.AMMONIA;
import static games.sirius.SiriusConstants.SiriusCardType.CONTRABAND;
import static java.util.stream.Collectors.toList;

public class SiriusGameState extends AbstractGameState {
    Deck<SiriusCard> ammoniaDeck;
    Deck<SiriusCard> contrabandDeck;
    List<PlayerArea> playerAreas;
    List<Moon> moons;
    Random rnd;
    int[] playerLocations;
    int[] moveSelected;
    Map<Integer, Medal> ammoniaMedals;
    Map<Integer, Medal> contrabandMedals;
    int ammoniaTrack;
    int contrabandTrack;

    public SiriusGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new SiriusTurnOrder(nPlayers), GameType.Sirius);
        rnd = new Random(gameParameters.getRandomSeed());
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> retValue = new ArrayList<>();
        retValue.add(ammoniaDeck);
        retValue.add(contrabandDeck);
        retValue.addAll(moons);
        for (PlayerArea pa : playerAreas) {
            retValue.add(pa.deck);
            retValue.add(pa.soldCards);
        }
        return retValue;
    }

    @Override
    protected SiriusGameState _copy(int playerId) {
        SiriusGameState retValue = new SiriusGameState(gameParameters.copy(), getNPlayers());
        retValue.ammoniaDeck = ammoniaDeck.copy();
        retValue.contrabandDeck = contrabandDeck.copy();
        retValue.playerAreas = playerAreas.stream().map(PlayerArea::copy).collect(toList());
        retValue.moons = moons.stream().map(Moon::copy).collect(toList());
        retValue.playerLocations = playerLocations.clone();
        retValue.moveSelected = moveSelected.clone();
        if (playerId != -1) {
            // we only know our card choice
            Arrays.fill(retValue.moveSelected, -1);
            retValue.moveSelected[playerId] = moveSelected[playerId];

            List<Deck<SiriusCard>> ammoniaDecks = new ArrayList<>();
            ammoniaDecks.add(retValue.ammoniaDeck);
            ammoniaDecks.addAll(moons.stream().filter(m -> m.moonType == MINING).map(Moon::getDeck).collect(toList()));
            ammoniaDecks.addAll(IntStream.range(0, getNPlayers()).filter(i -> i != playerId).mapToObj(retValue::getPlayerHand).collect(toList()));
            reshuffle(playerId, ammoniaDecks, c -> c.cardType == AMMONIA);

            List<Deck<SiriusCard>> contrabandDecks = new ArrayList<>();
            ammoniaDecks.add(retValue.contrabandDeck);
            ammoniaDecks.addAll(moons.stream().filter(m -> m.moonType == PROCESSING).map(Moon::getDeck).collect(toList()));
            ammoniaDecks.addAll(IntStream.range(0, getNPlayers()).filter(i -> i != playerId).mapToObj(retValue::getPlayerHand).collect(toList()));
            reshuffle(playerId, contrabandDecks, c -> c.cardType == CONTRABAND);
        }
        retValue.ammoniaTrack = ammoniaTrack;
        retValue.contrabandTrack = contrabandTrack;
        retValue.ammoniaMedals = new HashMap<>(ammoniaMedals);
        retValue.contrabandMedals = new HashMap<>(contrabandMedals);
        return retValue;
    }

    // Reshuffles all cards across the list of decks that meet the lambda predicate, and are not visible to player
    private void reshuffle(int player, List<Deck<SiriusCard>> decks, Predicate<SiriusCard> lambda) {
        // Now gather up all unknown cards for reshuffling
        // Unknown Ammonia cards are the draw pile, unseen moon piles, and other player hands

        int totalDeckSizePre = decks.stream().mapToInt(IComponentContainer::getSize).sum();

        Deck<SiriusCard> allCards = new Deck<>("temp", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

        // The fully observable decks care now filtered to remove any that are visible to us
        for (Deck<SiriusCard> d : decks) {
            if (d instanceof PartialObservableDeck) {
                PartialObservableDeck<SiriusCard> pod = (PartialObservableDeck<SiriusCard>) d;
                for (int i = 0; i < pod.getSize(); i++) {
                    if (!pod.getVisibilityForPlayer(i, player) && lambda.test(pod.get(i)))
                        allCards.add(pod.get(i));
                }
            } else {
                switch (d.getVisibilityMode()) {
                    case VISIBLE_TO_ALL:
                        // don't shuffle
                        break;
                    case VISIBLE_TO_OWNER:
                        if (d.getOwnerId() == player)
                            break;
                    case HIDDEN_TO_ALL:
                        allCards.add(d.stream().filter(lambda).collect(toList()));
                        break;
                    case FIRST_VISIBLE_TO_ALL:
                        Deck<SiriusCard> temp = d.copy();
                        temp.draw();
                        allCards.add(temp.stream().filter(lambda).collect(toList()));
                        break;
                    case LAST_VISIBLE_TO_ALL:
                        throw new AssertionError("Not supported : LAST_VISIBLE_TO_ALL");
                    case MIXED_VISIBILITY:
                        throw new AssertionError("Not supported : MIXED_VISIBILITTY");
                }
            }
        }
        allCards.shuffle(rnd);

        // and put the shuffled cards in place
        for (Deck<SiriusCard> d : decks) {
            if (d instanceof PartialObservableDeck) {
                PartialObservableDeck<SiriusCard> pod = (PartialObservableDeck<SiriusCard>) d;
                for (int i = 0; i < pod.getSize(); i++) {
                    if (!pod.getVisibilityForPlayer(i, player) && lambda.test(pod.get(i)))
                        pod.setComponent(i, allCards.draw());
                }
            } else {
                switch (d.getVisibilityMode()) {
                    case VISIBLE_TO_ALL:
                        break;
                    case VISIBLE_TO_OWNER:
                        if (d.getOwnerId() == player)
                            break;
                    case HIDDEN_TO_ALL:
                        for (int i = 0; i < d.getSize(); i++)
                            if (lambda.test(d.get(i)))
                                d.setComponent(i, allCards.draw());
                        break;
                    case FIRST_VISIBLE_TO_ALL:
                        for (int i = 1; i < d.getSize(); i++)
                            if (lambda.test(d.get(i)))
                                d.setComponent(i, allCards.draw());
                        break;
                    case LAST_VISIBLE_TO_ALL:
                        throw new AssertionError("Not supported : LAST_VISIBLE_TO_ALL");
                    case MIXED_VISIBILITY:
                        throw new AssertionError("Not supported : MIXED_VISIBILITTY");
                }
            }
        }

        int totalDeckSizePost = decks.stream().mapToInt(IComponentContainer::getSize).sum();

        // sanity checks
        if (totalDeckSizePost != totalDeckSizePre || allCards.getSize() > 0) {
            throw new AssertionError("Problem with shuffling");
        }
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId) / 25.0;
    }

    @Override
    public double getGameScore(int playerId) {
        return playerAreas.get(playerId).soldCards.getSize() +
                playerAreas.get(playerId).medals.stream().mapToInt(m -> m.value).sum();
    }

    @Override
    protected void _reset() {
        ammoniaDeck = new Deck<>("ammoniaDeck", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        contrabandDeck = new Deck<>("contrabandDeck", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        moons = new ArrayList<>();
        ammoniaTrack = 0;
        contrabandTrack = 0;
        ammoniaMedals = new HashMap<>();
        contrabandMedals = new HashMap<>();
    }

    // This marks a decision as having been made, but does not yet implement this decision
    public void chooseMoveCard(int moon) {
        moveSelected[getCurrentPlayer()] = moon;
    }

    // for testing
    public int[] getMoveSelected() {
        return moveSelected.clone();
    }

    protected boolean allMovesSelected() {
        for (int b : moveSelected) if (b == -1) return false;
        return true;
    }

    protected void applyChosenMoves() {
        for (int i = 0; i < moveSelected.length; i++) {
            movePlayerTo(i, moveSelected[i]);
        }
    }

    public void movePlayerTo(int player, int moon) {
        if (moon > moons.size())
            throw new IllegalArgumentException(String.format("Cannot move to Moon %d as there are only %d", moon, moons.size()));
        playerLocations[player] = moon;
    }

    public int getLocationIndex(int player) {
        return playerLocations[player];
    }

    public Moon getMoon(int index) {
        return moons.get(index);
    }

    public List<Moon> getAllMoons() {
        return moons;
    }

    public void addCardToHand(int player, SiriusCard card) {
        playerAreas.get(player).deck.add(card);
    }

    public Deck<SiriusCard> getPlayerHand(int player) {
        return playerAreas.get(player).deck;
    }

    public int getTrackPosition(SiriusConstants.SiriusCardType track) {
        switch (track) {
            case AMMONIA:
                return ammoniaTrack;
            case CONTRABAND:
                return contrabandTrack;
            default:
                throw new IllegalArgumentException("No track for " + track);
        }
    }

    public Map<Integer, Medal> getMedals(SiriusConstants.SiriusCardType track) {
        switch (track) {
            case AMMONIA:
                return ammoniaMedals;
            case CONTRABAND:
                return contrabandMedals;
            default:
                throw new IllegalArgumentException("No medals for " + track);
        }
    }

    public Deck<SiriusCard> getDeck(SiriusConstants.SiriusCardType type) {
        switch (type) {
            case AMMONIA:
                return ammoniaDeck;
            case CONTRABAND:
                return contrabandDeck;
            case SMUGGLER:
            case FAVOUR:
        }
        throw new AssertionError("Not yet implemented");
    }

    public void sellCard(SiriusCard card, int amount) {
        PlayerArea pa = playerAreas.get(getCurrentPlayer());
        pa.soldCards.add(card);
        pa.deck.remove(card);
        if (card.cardType == AMMONIA) {
            for (int i = 0; i < amount; i++) {
                ammoniaTrack++;
                if (ammoniaMedals.containsKey(ammoniaTrack)) {
                    pa.medals.add(ammoniaMedals.remove(ammoniaTrack));
                }
            }
        } else if (card.cardType == CONTRABAND) {
            for (int i = 0; i < amount; i++) {
                contrabandTrack++;
                if (contrabandMedals.containsKey(contrabandTrack)) {
                    pa.medals.add(contrabandMedals.remove(contrabandTrack));
                }
            }
        }
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof SiriusGameState) {
            return o.hashCode() == hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int retValue = Objects.hash(playerAreas, turnOrder, gamePhase, moons, ammoniaDeck, gameParameters, gameStatus,
                ammoniaTrack, contrabandTrack, ammoniaMedals, contrabandMedals);
        return retValue + 31 * Arrays.hashCode(playerResults)
                - 255 * Arrays.hashCode(playerLocations)
                - 31 * 255 * Arrays.hashCode(moveSelected);
    }

}
