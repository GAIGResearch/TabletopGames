package games.sirius;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import games.GameType;

import java.util.*;

import static games.sirius.SiriusConstants.SiriusCardType.AMMONIA;
import static games.sirius.SiriusConstants.SiriusCardType.CONTRABAND;
import static java.util.stream.Collectors.toList;

public class SiriusGameState extends AbstractGameState {
    Deck<SiriusCard> ammoniaDeck;
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
        retValue.addAll(moons);
        for (PlayerArea pa : playerAreas)
            retValue.add(pa.deck);
        return retValue;
    }

    @Override
    protected SiriusGameState _copy(int playerId) {
        SiriusGameState retValue = new SiriusGameState(gameParameters.copy(), getNPlayers());
        retValue.ammoniaDeck = ammoniaDeck.copy();
        retValue.playerAreas = playerAreas.stream().map(PlayerArea::copy).collect(toList());
        retValue.moons = moons.stream().map(Moon::copy).collect(toList());
        retValue.playerLocations = playerLocations.clone();
        retValue.moveSelected = moveSelected.clone();
        if (playerId != -1) {
            // we only know our card choice
            Arrays.fill(retValue.moveSelected, -1);
            retValue.moveSelected[playerId] = moveSelected[playerId];

            // Now gather up all unknown cards for reshuffling
            // Unknown Ammonia cards are the draw pile, unseen moon piles, and other player hands
            Deck<SiriusCard> unseenAmmonia = retValue.ammoniaDeck;
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId)
                    unseenAmmonia.add(retValue.getPlayerHand(i));
            }
            for (int m = 0; m < moons.size(); m++) {
                Moon moon = retValue.getMoon(m);
                for (int i = 0; i < moon.deck.getSize(); i++) {
                    if (!moon.deck.getVisibilityForPlayer(i, playerId)) {
                        unseenAmmonia.add(moon.deck.get(i));  // we leave the card in place in the Moon deck
                    }
                }
            }
            // shuffle the unknown cards
            unseenAmmonia.shuffle(retValue.rnd);

            // and put the shuffled cards in place
            for (int p = 0; p < getNPlayers(); p++) {
                if (p != playerId) {
                    Deck<SiriusCard> playerHand = retValue.getPlayerHand(p);
                    for (int i = 0; i < retValue.playerAreas.get(p).deck.getSize(); i++)
                        playerHand.setComponent(i, ammoniaDeck.draw());
                }
            }
            for (int m = 0; m < moons.size(); m++) {
                Moon moon = retValue.getMoon(m);
                for (int i = 0; i < moon.deck.getSize(); i++) {
                    if (!moon.deck.getVisibilityForPlayer(i, playerId)) {
                        moon.deck.setComponent(i, ammoniaDeck.draw());
                    }
                }
            }
            retValue.ammoniaDeck = unseenAmmonia;
        }
        retValue.ammoniaTrack = ammoniaTrack;
        retValue.contrabandTrack = contrabandTrack;
        retValue.ammoniaMedals = new HashMap<>(ammoniaMedals);
        retValue.contrabandMedals = new HashMap<>(contrabandMedals);
        return retValue;
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
        ammoniaDeck = new Deck<>("ammoniaDeck", -1, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
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

    public Medal[] getMedals(SiriusConstants.SiriusCardType track) {
        switch (track) {
            case AMMONIA:
                return ammoniaMedals.clone();
            case CONTRABAND:
                return contrabandMedals.clone();
            default:
                throw new IllegalArgumentException("No medals for " + track);
        }
    }

    public void sellCard(SiriusCard card) {
        PlayerArea pa = playerAreas.get(getCurrentPlayer());
        pa.soldCards.add(card);
        pa.deck.remove(card);
        if (card.cardType == AMMONIA) {
            for (int i = 0; i < card.value; i++) {
                if (ammoniaTrack < ammoniaMedals.length - 1) {
                    ammoniaTrack++;
                    if (ammoniaMedals[ammoniaTrack] != null) {
                        pa.medals.add(ammoniaMedals[ammoniaTrack]);
                        ammoniaMedals[ammoniaTrack] = null;
                    }
                }
            }
        } else if (card.cardType == CONTRABAND) {
            for (int i = 0; i < card.value; i++) {
                if (contrabandTrack < contrabandMedals.length) {
                    contrabandTrack++;
                    if (contrabandMedals[contrabandTrack] != null) {
                        pa.medals.add(contrabandMedals[contrabandTrack]);
                        contrabandMedals[contrabandTrack] = null;
                    }
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
                ammoniaTrack, contrabandTrack);
        return retValue + 31 * Arrays.hashCode(playerResults)
                + 31 * 31 * Arrays.hashCode(ammoniaMedals)
                + 31 * 31 * 31 * Arrays.hashCode(contrabandMedals)
                - 255 * Arrays.hashCode(playerLocations)
                - 31 * 255 * Arrays.hashCode(moveSelected);
    }

}
