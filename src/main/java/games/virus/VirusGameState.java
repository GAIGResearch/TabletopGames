package games.virus;

import core.AbstractGameParameters;
import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.virus.cards.*;
import games.virus.components.VirusBody;

import java.util.ArrayList;
import java.util.List;

public class VirusGameState extends AbstractGameState implements IPrintable {
    List<VirusBody>       playerBodies;   // Each player has a body
    List<Deck<VirusCard>> playerDecks;    // Each player has a deck withh 3 cards
    Deck<VirusCard>       drawDeck;       // The deck with the not yet played cards, It is not visible for any player
    Deck<VirusCard>       discardDeck;    // The deck with already played cards. It is visible for all players

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
        // TODO: partial observability
        VirusGameState vgs = new VirusGameState(gameParameters.copy(), getNPlayers());
        vgs.drawDeck = drawDeck.copy();
        vgs.discardDeck = discardDeck.copy();
        vgs.playerDecks = new ArrayList<>();
        vgs.playerBodies = new ArrayList<>();
        for (int i = 0; i < getNPlayers(); i++) {
            vgs.playerDecks.add(playerDecks.get(i).copy());
            vgs.playerBodies.add((VirusBody) playerBodies.get(i).copy());
        }
        return vgs;
    }

    @Override
    protected double _getScore(int playerId) {
        return new VirusHeuristic().evaluateState(this, playerId);
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        // TODO:
        return null;
    }

    @Override
    protected void _reset() {
        playerBodies = new ArrayList<>();
        playerDecks = new ArrayList<>();
        drawDeck = null;
        discardDeck = null;
    }

    public VirusGameState(AbstractGameParameters gameParameters, int nPlayers) {
        super(gameParameters, new VirusTurnOrder(nPlayers));
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

    public List<VirusBody> getPlayerBodies() {
        return playerBodies;
    }

    @Override
    public void printToConsole() {
        int nPlayers = playerBodies.size();
        String[] stringStr = new String[nPlayers+3];

        stringStr[0] = "----------------------------------------------------";

        for (int i=0; i<nPlayers; i++)
            stringStr[i+1] = "Player " + i + "    -> Body: " + playerBodies.get(i).toString();

        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand -> ");

        for (VirusCard card : playerDecks.get(getCurrentPlayer()).getComponents()) {
            sb.append(card.toString());
            sb.append(" ");
        }
        stringStr[nPlayers+1] = sb.toString();
        stringStr[nPlayers+2] = "----------------------------------------------------";

        for (String s : stringStr){
            System.out.println(s);
        }
    }
}
