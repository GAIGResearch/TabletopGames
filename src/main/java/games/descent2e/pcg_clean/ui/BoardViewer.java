package games.descent2e.pcg_clean.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Window and navigation controller for one or more generated boards. */
public final class BoardViewer {
    private final List<BoardViewModel> boards;
    private final JFrame frame = new JFrame("Descent board generator");
    private final BoardCanvas canvas;
    private final JLabel status = new JLabel();
    private final JLabel fitness = new JLabel();
    private final JTextArea problems = new JTextArea(3, 60);
    private final DefaultTableModel fitnessModel = new DefaultTableModel(
            new String[]{"Fitness criterion", "Score", "Weight", "Weighted score"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JButton previous = new JButton("Previous");
    private final JButton next = new JButton("Next");
    private int index;

    public BoardViewer(List<BoardViewModel> boards, TileArtworkProvider artworkProvider) {
        if (boards.isEmpty()) throw new IllegalArgumentException("At least one board is required");
        this.boards = List.copyOf(boards);
        this.canvas = new BoardCanvas(BoardPalette.defaultPalette(), artworkProvider);
        initialiseWindow();
        display(0);
    }

    public static void show(List<BoardViewModel> boards, TileArtworkProvider artworkProvider) {
        Runnable create = () -> new BoardViewer(boards, artworkProvider).frame.setVisible(true);
        if (SwingUtilities.isEventDispatchThread()) create.run();
        else SwingUtilities.invokeLater(create);
    }

    private void initialiseWindow() {
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(controls(), BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setMinimumSize(new Dimension(650, 500));
    }

    private JComponent controls() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT));
        previous.addActionListener(event -> display(index - 1));
        next.addActionListener(event -> display(index + 1));
        JCheckBox graph = new JCheckBox("Show piece graph", true);
        graph.addActionListener(event -> canvas.setShowPieceGraph(graph.isSelected()));
        JCheckBox artwork = new JCheckBox("Show tile artwork", true);
        artwork.addActionListener(event -> canvas.setShowArtwork(artwork.isSelected()));
        navigation.add(previous);
        navigation.add(next);
        navigation.add(graph);
        navigation.add(artwork);
        navigation.add(status);
        fitness.setFont(fitness.getFont().deriveFont(Font.BOLD, 16f));
        fitness.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        navigation.add(fitness);
        problems.setEditable(false);
        problems.setLineWrap(true);
        problems.setWrapStyleWord(true);
        problems.setBackground(panel.getBackground());
        JTable fitnessTable = new JTable(fitnessModel);
        fitnessTable.setFillsViewportHeight(true);
        fitnessTable.setRowSelectionAllowed(false);
        JSplitPane details = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(fitnessTable), new JScrollPane(problems));
        details.setResizeWeight(0.58);
        details.setBorder(BorderFactory.createEmptyBorder());
        panel.add(navigation, BorderLayout.NORTH);
        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    private void display(int requestedIndex) {
        index = Math.max(0, Math.min(boards.size() - 1, requestedIndex));
        BoardViewModel model = boards.get(index);
        canvas.display(model);
        status.setText("%d / %d — Candidate %d — %s — %d violation(s) — %d pieces — %d traversable cells".formatted(
                index + 1, boards.size(), model.candidateId(), model.feasible() ? "feasible" : "infeasible",
                model.violations().size(), model.genome().tiles().size(), model.layout().traversableCellCount()));
        fitness.setText("Fitness: %.6f".formatted(model.fitness()));
        fitness.setForeground(model.feasible() ? new Color(24, 125, 58) : new Color(170, 50, 45));
        updateFitnessTable(model);
        problems.setText(model.violations().isEmpty() ? "All configured constraints satisfied."
                : String.join(System.lineSeparator(), model.violations()));
        previous.setEnabled(index > 0);
        next.setEnabled(index + 1 < boards.size());
    }

    private void updateFitnessTable(BoardViewModel model) {
        fitnessModel.setRowCount(0);
        model.criteria().forEach(criterion -> fitnessModel.addRow(new Object[]{
                criterion.name(), format(criterion.score()), format(criterion.weight()),
                format(criterion.weightedContribution())}));
    }

    private String format(double value) { return "%.6f".formatted(value); }
}
