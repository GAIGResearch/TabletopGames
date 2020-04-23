import core.AIPlayer;
import core.Runner;
import uno.UnoForwardModel;
import uno.UnoGame;
import uno.UnoGameState;
import uno.UnoParameters;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUno {
    public static void main(String[] args) {

        Runner tabletopEngine = new Runner();

        List<AIPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));

        tabletopEngine.setGame(new UnoGame(), new UnoParameters(), new UnoGameState(), new UnoForwardModel(),
                "data/uno/", players);

//      tabletopEngine.run();
        tabletopEngine.play();

    }
}

/*
19 Cartas azules - un 0 y pares del 1 al 9
19 Cartas verdes - un 0 y pares del 1 al 9
19 Cartas rojas - un 0 y pares del 1 al 9
19 Cartas amarillas - un 0 y pares del 1 al 9
8 cartas especiales ROBA DOS - 2 de cada color
8 cartas especiales CAMBIO DE SENTIDO - 2 de cada color
8 cartas especiales PIERDE EL TURNO - 2 de cada color
4 cartas especiales COMODIN CAMBIO DE COLOR
4 cartas especiales COMODIN CAMBIO DE COLOR ROBA CUATRO
 */
