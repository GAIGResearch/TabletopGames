package games.virus;

import core.AbstractParameters;
import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.GameType;
import games.virus.cards.*;
import games.virus.components.VirusBody;
import games.virus.components.VirusOrgan;

import java.util.*;


public class VirusGameState extends AbstractGameState implements IPrintable {
    List<VirusBody>       playerBodies;   // Each player has a body
    List<Deck<VirusCard>> playerDecks;    // Each player has a deck with 3 cards
    Deck<VirusCard>       drawDeck;       // The deck with the not yet played cards, It is not visible for any player
    Deck<VirusCard>       discardDeck;    // The deck with already played cards. It is visible for all players

    public VirusGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Virus;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerBodies);
            addAll(playerDecks);
            add(drawDeck);
            add(discardDeck);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        VirusGameState vgs = new VirusGameState(gameParameters.copy(), getNPlayers());
        vgs.drawDeck = drawDeck.copy();
        vgs.discardDeck = discardDeck.copy();
        vgs.playerDecks = new ArrayList<>();
        vgs.playerBodies = new ArrayList<>();
        for (int i = 0; i < getNPlayers(); i++) {
            vgs.playerDecks.add(playerDecks.get(i).copy());
            vgs.playerBodies.add((VirusBody) playerBodies.get(i).copy());
        }
        if (getCoreGameParameters().partialObservable && playerId != -1) {
            // Draw deck and opponent hand cards are hidden. Shuffle all together and deal random cards for opponents.
            for (int i = 0; i < getNPlayers(); i++) {
                if (playerId != i) {
                    vgs.drawDeck.add(vgs.playerDecks.get(i));
                    vgs.playerDecks.get(i).clear();
                }
            }
            vgs.drawDeck.shuffle(redeterminisationRnd);
            for (int i = 0; i < getNPlayers(); i++) {
                if (playerId != i) {
                    for (int j = 0; j < playerDecks.get(i).getSize(); j++) {
                        vgs.playerDecks.get(i).add(vgs.drawDeck.draw());
                    }
                }
            }
        }
        return vgs;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new VirusHeuristic().evaluateState(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return playerBodies.get(playerId).organs.values().stream().filter(VirusOrgan::isHealthy).count();
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirusGameState)) return false;
        VirusGameState that = (VirusGameState) o;
        return Objects.equals(playerBodies, that.playerBodies) &&
                Objects.equals(playerDecks, that.playerDecks) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(discardDeck, that.discardDeck);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerBodies, playerDecks, drawDeck, discardDeck);
    }

    public Deck<VirusCard> getDiscardDeck() {
        return discardDeck;
    }

    public Deck<VirusCard> getDrawDeck() {
        return drawDeck;
    }

    public List<Deck<VirusCard>> getPlayerDecks() {
        return playerDecks;
    }

    public String toString() {
        return gameParameters.hashCode() + "|" +
                gameStatus.hashCode() + "|" +
                gamePhase.hashCode() + "|" +
                Arrays.hashCode(playerResults) + "|*|" +
                playerBodies.hashCode() + "|" +
                playerDecks.hashCode() + "|" +
                drawDeck.hashCode() + "|" +
                discardDeck.hashCode() + "|";
    }
    @Override
    public void printToConsole() {
        int nPlayers = getNPlayers();

        System.out.println("----------------------------------------------------");

        for (int i=0; i<nPlayers; i++) {
            if (i == getCurrentPlayer()) System.out.print(">>> ");
            System.out.println("Player " + i + "    -> Body: " + playerBodies.get(i).toString());

            if (i == getCurrentPlayer()) System.out.print(">>> ");
            System.out.println("Player " + i + "    -> Player Hand (" + playerDecks.get(i).getComponentID() + "): " + playerDecks.get(i).toString());
        }

        System.out.println();
        System.out.println("Draw deck (" + drawDeck.getComponentID() + "): " + drawDeck.toString());
        System.out.println("Discard (" + discardDeck.getComponentID() + "): " + discardDeck.toString());

        System.out.println("----------------------------------------------------");
    }
}
