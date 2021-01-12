package games.dominion.gui;

import games.dominion.*;
import games.dominion.cards.*;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import static java.util.stream.Collectors.*;

public class DominionMarketView extends JComponent {

    JTable dataDisplay;
    JScrollPane scrollPane;
    String[] columnNames = {"CardType", "Number"};
    Object[][] cardData;
    List<CardType> cards;

    public DominionMarketView(DominionGameState state) {
        cards = state.cardsIncludedInGame().stream().sorted((c1, c2) -> c2.cost - c1.cost).collect(toList());
        cardData = new Object[cards.size()][2];
        update(state);

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dataDisplay = new JTable(cardData, columnNames);
        dataDisplay.setFillsViewportHeight(true);
        dataDisplay.getColumnModel().getColumn(0).setPreferredWidth(40);
        dataDisplay.getColumnModel().getColumn(1).setPreferredWidth(20);
        scrollPane = new JScrollPane(dataDisplay);
        scrollPane.setPreferredSize(new Dimension(220, 220));
        this.add(scrollPane, BorderLayout.CENTER);
    }

    public void update(DominionGameState state) {
        for (int i = 0; i < cards.size(); i++) {
            cardData[i][0] = cards.get(i).name();
            cardData[i][1] = state.cardsOfType(cards.get(i), -1, DominionConstants.DeckType.SUPPLY);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(250, 250);
    }


}
