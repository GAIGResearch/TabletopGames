package games.sirius;

import core.*;
import core.components.*;
import core.interfaces.IComponentContainer;
import games.GameType;

import java.util.*;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusCardType.*;
import static java.util.stream.Collectors.toList;

public class SiriusGameState extends AbstractGameState {
    Deck<SiriusCard> ammoniaDeck;
    Deck<SiriusCard> contrabandDeck;
    Deck<SiriusCard> smugglerDeck;
    Deck<SiriusCard> ammoniaDiscardDeck;
    Deck<SiriusCard> contrabandDiscardDeck;
    Deck<SiriusCard> smugglerDiscardDeck;
    Deck<SiriusCard> favourDeck;
    List<PlayerArea> playerAreas;
    List<Moon> moons;
    Random rnd;
    int[] playerLocations;
    int[] moveSelected;
    int medalCount;
    int ammoniaTrack;
    int contrabandTrack;
    int corruptionTrack;

    int[] playerByRank; // this is indexed by rank and holds the current player at that rank
    int[] nextPlayer; // this is indexed by player and indicates the next player in rank. It is updated at the start of each round.
    Map<String, List<Boolean>> actionsTakenByPlayers; // used to track is certain action types have been taken (when these are only usable once)


    @Override
    public GameType _getGameType() {
        return GameType.Sirius;
    }

    public SiriusGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        rnd = new Random(gameParameters.getRandomSeed());
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> retValue = new ArrayList<>();
        retValue.add(ammoniaDeck);
        retValue.add(contrabandDeck);
        retValue.add(smugglerDeck);
        retValue.add(favourDeck);
        retValue.addAll(moons);
        retValue.add(ammoniaDiscardDeck);
        retValue.add(contrabandDiscardDeck);
        retValue.add(smugglerDiscardDeck);
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
        retValue.smugglerDeck = smugglerDeck.copy();
        retValue.ammoniaDiscardDeck = ammoniaDiscardDeck.copy();
        retValue.contrabandDiscardDeck = contrabandDiscardDeck.copy();
        retValue.smugglerDiscardDeck = smugglerDiscardDeck.copy();
        retValue.favourDeck = favourDeck.copy();
        retValue.playerAreas = playerAreas.stream().map(PlayerArea::copy).collect(toList());
        retValue.moons = new ArrayList<>();
        for (Moon m : moons) {
            retValue.moons.add(m.copy());
        }
        retValue.playerLocations = playerLocations.clone();
        retValue.moveSelected = moveSelected.clone();
        if (playerId > -1) {
            // we only know our card choice
            Arrays.fill(retValue.moveSelected, -1);
            retValue.moveSelected[playerId] = moveSelected[playerId];

            List<Deck<SiriusCard>> ammoniaDecks = new ArrayList<>();
            ammoniaDecks.add(retValue.ammoniaDeck);
            ammoniaDecks.addAll(retValue.moons.stream().filter(m -> m.moonType == MINING).map(Moon::getDeck).collect(toList()));
            // player hands (cargo + crew) are face up
            //      ammoniaDecks.addAll(IntStream.range(0, getNPlayers()).filter(i -> i != playerId).mapToObj(retValue::getPlayerHand).collect(toList()));
            reshuffle(playerId, ammoniaDecks, c -> c.cardType == AMMONIA);

            List<Deck<SiriusCard>> contrabandDecks = new ArrayList<>();
            contrabandDecks.add(retValue.contrabandDeck);
            contrabandDecks.addAll(retValue.moons.stream().filter(m -> m.moonType == PROCESSING).map(Moon::getDeck).collect(toList()));
            //     contrabandDecks.addAll(IntStream.range(0, getNPlayers()).filter(i -> i != playerId).mapToObj(retValue::getPlayerHand).collect(toList()));
            reshuffle(playerId, contrabandDecks, c -> c.cardType == CONTRABAND);
        }
        retValue.ammoniaTrack = ammoniaTrack;
        retValue.contrabandTrack = contrabandTrack;
        retValue.corruptionTrack = corruptionTrack;
        retValue.medalCount = medalCount;
        retValue.nextPlayer = nextPlayer.clone();
        retValue.actionsTakenByPlayers = new HashMap<>();
        for (String key : actionsTakenByPlayers.keySet()) {
            retValue.actionsTakenByPlayers.put(key, new ArrayList<>(actionsTakenByPlayers.get(key)));
        }
        retValue.playerByRank = playerByRank.clone();
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
        SiriusParameters parameters = (SiriusParameters) gameParameters;
        long cartels = moons.stream().filter(m -> m.cartelPlayer == playerId).count();
        return cartels * parameters.pointsPerCartel + playerAreas.get(playerId).soldCards.getSize() +
                playerAreas.get(playerId).medals.stream().mapToInt(m -> m.value).sum();
    }

    @Override
    public double getTiebreak(int player) {
        PlayerArea pa = playerAreas.get(player);
        int medals = pa.medals.size();
        int soldCards = pa.soldCards.getSize();
        int rank = getRank(player);
        return medals * 10000 + soldCards * 100 - rank;
    }

    protected void _reset() {
        ammoniaDeck = new Deck<>("ammoniaDeck", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        contrabandDeck = new Deck<>("contrabandDeck", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        smugglerDeck = new Deck<>("smugglerDeck", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        ammoniaDiscardDeck = new Deck<>("ammoniaDiscards", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        contrabandDiscardDeck = new Deck<>("contrabandDiscards", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        smugglerDiscardDeck = new Deck<>("smugglerDiscards", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        favourDeck = new Deck<>("favourDeck", -1, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        moons = new ArrayList<>();
        playerAreas = new ArrayList<>();
        ammoniaTrack = 0;
        contrabandTrack = 0;
        corruptionTrack = 0;
        nextPlayer = new int[nPlayers];
        playerByRank = new int[nPlayers + 1];
        Arrays.setAll(playerByRank, i -> i - 1);
        initialiseActions();
        updatePlayerOrder();
    }


    protected void initialiseActions() {
        actionsTakenByPlayers = new HashMap<>();
        List<Boolean> allFalse = new ArrayList<>(nPlayers);
        for (int i = 0; i < nPlayers; i++) allFalse.add(Boolean.FALSE);
        actionsTakenByPlayers.put("Favour", allFalse);
        actionsTakenByPlayers.put("Sold", new ArrayList<>(allFalse));
        actionsTakenByPlayers.put("Betrayed", new ArrayList<>(allFalse));
    }

    public void updatePlayerOrder() {
        // we set up nextPlayer to point to the next player
        for (int r = 1; r < nPlayers; r++) {
            nextPlayer[playerByRank[r]] = playerByRank[r + 1];
        }
        nextPlayer[playerByRank[nPlayers]] = playerByRank[1];  // we loop back to the start
    }

    // returns the current rank of the player for determining move order (1 is first, and so on)
    public int getRank(int player) {
        for (int r = 1; r <= nPlayers; r++) {
            if (playerByRank[r] == player)
                return r;
        }
        throw new AssertionError("Should be unreachable");
    }

    public void setRank(int player, int rank) {
        int oldRank = getRank(player);
        int inc = oldRank < rank ? 1 : -1;
        int diff = Math.abs(oldRank - rank);
        // we shuffle all the ranks up and move player to rank
        // We have to do this step last so that we have already shuffled out the previous occupant
        for (int i = 0; i <= diff; i++) {
            if (i == diff) {
                playerByRank[rank] = player;
            } else {
                playerByRank[i * inc + oldRank] = playerByRank[(i + 1) * inc + oldRank];
            }
        }
    }

    public int getPlayerAtRank(int rank) {
        return playerByRank[rank];
    }

    protected int getFirstMatchingPlayerFrom(int startFrom, IntPredicate test) {
        // we go through in nextPlayer order from startFrom
        int player = startFrom;
        for (int i = 0; i < nPlayers; i++) {
            if (test.test(player)) return player;
            player = nextPlayer[player];
        }
        return -1;
    }

    public void setActionTaken(String ref, int player) {
        actionsTakenByPlayers.get(ref).set(player, true);
    }
    public boolean getActionTaken(String ref, int player) {
        return actionsTakenByPlayers.get(ref).get(player);
    }

    // This marks a decision as having been made, but does not yet implement this decision
    public void chooseMoveCard(int moon) {
        moveSelected[getCurrentPlayer()] = moon;
    }

    // for testing
    public int[] getMoveSelected() {
        return moveSelected.clone();
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

    public int[] getPlayersAt(int location) {
        List<Integer> players = new ArrayList<>();
        for (int i = 0; i < playerLocations.length; i++) {
            if (playerLocations[i] == location)
                players.add(i);
        }
        int[] retValue = new int[players.size()];
        Arrays.setAll(retValue, players::get);
        return retValue;
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
            case SMUGGLER:
                return corruptionTrack;
            default:
                throw new IllegalArgumentException("No track for " + track);
        }
    }

    public int getMedalsTaken() {
        return medalCount;
    }

    public PlayerArea getPlayerArea(int player) {
        return playerAreas.get(player);
    }

    public void addToDeck(SiriusConstants.SiriusCardType type, boolean discard, List<SiriusCard> cards) {
        Deck<SiriusCard> deck = getDeck(type, discard);
        deck.add(cards);
    }

    public SiriusCard drawFromDeck(SiriusConstants.SiriusCardType type, boolean discardDeck) {
        Deck<SiriusCard> deck = getDeck(type, discardDeck);
        SiriusCard retValue = deck.draw();
        if (!discardDeck && deck.getSize() == 0) {
            if (type == FAVOUR)
                throw new AssertionError("This shouldn't happen- Favour has an unlimited supply");
            // we shuffle the discard deck into this
            Deck<SiriusCard> discards = getDeck(type, true);
            deck.add(discards);
            discards.clear();
            deck.shuffle(rnd);
        }
        return retValue;
    }

    Deck<SiriusCard> getDeck(SiriusConstants.SiriusCardType type, boolean discard) {
        switch (type) {
            case AMMONIA:
                return discard ? ammoniaDiscardDeck : ammoniaDeck;
            case CONTRABAND:
                return discard ? contrabandDiscardDeck : contrabandDeck;
            case FAVOUR:
                return favourDeck;
            case SMUGGLER:
                return discard ? smugglerDiscardDeck : smugglerDeck;
        }
        throw new AssertionError("Not yet implemented");
    }

    public int getDeckSize(SiriusConstants.SiriusCardType type, boolean discard) {
        return getDeck(type, discard).getSize();
    }


    public boolean sellCard(SiriusCard card, int value) {
        int amount = Math.abs(value);
        boolean triggersPolice = false; // returned
        PlayerArea pa = playerAreas.get(getCurrentPlayer());
        pa.soldCards.add(card);
        pa.deck.remove(card);
        SiriusParameters params = (SiriusParameters) gameParameters;
        if (card.cardType == AMMONIA) {
            for (int i = 0; i < amount; i++) {
                ammoniaTrack++;
                if (ammoniaTrack < params.ammoniaTrack.length && params.ammoniaTrack[ammoniaTrack] == 1) {
                    pa.medals.add(new Medal(AMMONIA, params.medalValues[medalCount]));
                    medalCount++;
                }
            }
        } else if (card.cardType == CONTRABAND) {
            for (int i = 0; i < amount; i++) {
                contrabandTrack++;
                if (contrabandTrack < params.contrabandTrack.length && params.contrabandTrack[contrabandTrack] == 1) {
                    pa.medals.add(new Medal(CONTRABAND, params.medalValues[medalCount]));
                    medalCount++;
                }
            }
        } else if (card.cardType == SMUGGLER) {
            for (int i = 0; i < amount; i++) {
                if (value < 0)
                    corruptionTrack--;
                else
                    corruptionTrack++;
                if (corruptionTrack < params.corruptionTrack.length && corruptionTrack >= 0 && params.corruptionTrack[corruptionTrack] == 1) {
                    triggersPolice = true;
                }
                // And make sure we cannot move beyond the limit
                if (corruptionTrack > params.corruptionTrack.length - 1)
                    corruptionTrack = params.corruptionTrack.length - 1;
                if (corruptionTrack < 0) corruptionTrack = 0;
            }
        }
        return triggersPolice;
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
        int retValue = super.hashCode() + 31 * Objects.hash(playerAreas, moons, ammoniaDeck,
                ammoniaDiscardDeck, contrabandDiscardDeck, smugglerDiscardDeck, actionsTakenByPlayers,
                ammoniaTrack, contrabandTrack, medalCount, smugglerDeck, favourDeck);
        return retValue + 31 * Arrays.hashCode(nextPlayer) +  31 * 31 * Arrays.hashCode(playerByRank)
                - 255 * Arrays.hashCode(playerLocations)
                - 31 * 255 * Arrays.hashCode(moveSelected);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int result = Objects.hash(gameParameters);
        sb.append(result).append("|");
        result = Objects.hash(gameStatus);
        sb.append(result).append("|");
        result = Objects.hash(gamePhase);
        sb.append(result).append("|");
        result = Arrays.hashCode(playerResults);
        sb.append(result).append("|*|");
        result = Objects.hash(ammoniaDeck);
        sb.append(result).append("|");
        result = Objects.hash(medalCount);
        sb.append(result).append("|");
        result = Objects.hash(ammoniaTrack);
        sb.append(result).append("|");
        result = Objects.hash(contrabandDeck);
        sb.append(result).append("|");
        result = Objects.hash(smugglerDeck);
        sb.append(result).append("|");
        result = Objects.hash(favourDeck);
        sb.append(result).append("|2|");
        result = Objects.hash(contrabandTrack);
        sb.append(result).append("|3|");
        result = Objects.hash(playerAreas);
        sb.append(result).append("|4|");
        result = Objects.hash(moons);
        sb.append(result).append("|5|");
        result = Arrays.hashCode(moveSelected);
        sb.append(result).append("|6|");
        result = Arrays.hashCode(playerLocations);
        sb.append(result).append("|7|");
        result = Objects.hash(ammoniaDiscardDeck, contrabandDiscardDeck, smugglerDiscardDeck);
        sb.append(result).append("|");
        result =  actionsTakenByPlayers.hashCode();
        sb.append(result).append("|");
        result = Arrays.hashCode(nextPlayer);
        sb.append(result).append("|");
        result = Arrays.hashCode(playerByRank);
        sb.append(result).append("|");
        return sb.toString();
    }

}
