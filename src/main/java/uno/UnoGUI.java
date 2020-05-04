package uno;

import core.GUI;
import core.Game;

import javax.swing.*;
import java.awt.*;

public class UnoGUI extends GUI {
    public UnoGUI(Game game) {

        JPanel player1Panel= new JPanel();
        JPanel player2Panel = new JPanel();
        JPanel player3Panel = new JPanel();
        JPanel player4Panel = new JPanel();
        JPanel commonPanel = new JPanel();

        getContentPane().add(player1Panel, BorderLayout.SOUTH);
        getContentPane().add(player2Panel, BorderLayout.EAST);
        getContentPane().add(player3Panel, BorderLayout.NORTH);
        getContentPane().add(player4Panel, BorderLayout.WEST);
        getContentPane().add(commonPanel, BorderLayout.CENTER);

        this.setSize(500,500);
        this.setVisible(true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

    }
}

