package games.descent2e.pcg;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

public class GenerateBoardsGUI {

    int firstMin = 0;
    int firstMax = 1000;

    int secondMin = 0;
    int secondMax = 400;

    int offspringMin = 0;
    int offspringMax = 50;

    int infeasibleMin = 0;
    int infeasibleMax = 100;

    int FIRSTLOOP = firstMax / 2;
    int GENERATIONLOOP = secondMax / 2;
    int OFFSPRING = 10;
    int INFEASIBLE = 30;

    int defaultFirst = FIRSTLOOP;
    int defaultSecond = GENERATIONLOOP;
    int defaultOffspring = OFFSPRING;
    int defaultInfeasible = INFEASIBLE;

    int sizeMin = 100;
    int sizeMax = 300;

    int groupMin = 2;
    int groupMax = 10;

    float healthMin = 2f;
    float healthMax = 20f;

    int heightMin = 6;
    int heightMax = 30;

    int widthMin = 6;
    int widthMax = 30;

    int IDEAL_SIZE = 166;
    int IDEAL_GROUP = 5;
    float IDEAL_HEALTH = 5.872f;
    int IDEAL_HEIGHT = 18; // 572 / 32
    int IDEAL_WIDTH = 15; // 493 / 32

    int defaultSize = IDEAL_SIZE;
    int defaultGroups = IDEAL_GROUP;
    float defaultHealth = IDEAL_HEALTH;
    int defaultHeight = IDEAL_HEIGHT;
    int defaultWidth = IDEAL_WIDTH;
    boolean defaultIdeals = true;

    final JFrame mainWindow = new JFrame("Descent (Second Edition) Procedurally Generated Board Creator");
    JPanel mainPanel;

    TotalLabel totalGenerated = new TotalLabel();
    JSlider firstLoopSlide;
    JSlider secondLoopSlide;
    JSlider offspringLoopSlide;
    JSlider infeasibleSlide;
    JSlider sizeSlide;
    JSlider groupsSlide;
    JSlider healthSlide;
    JSlider heightSlide;
    JSlider widthSlide;

    JButton create = null;
    JTextArea generationText = null;
    JScrollPane outputScroll = null;

    int minWeight = 0;
    int maxWeight = 1000;

    float W_SIZE = 1f;
    float W_GROUPS = 1f;
    float W_HEALTH = 1f;
    float W_HEIGHT = 0f;
    float W_WIDTH = 0f;

    JSlider weightSizeSlide;
    JSlider weightGroupsSlide;
    JSlider weightHealthSlide;
    JSlider weightHeightSlide;
    JSlider weightWidthSlide;

    public GenerateBoardsGUI() {
    }

    public void load() {
        mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainWindow.setSize(1300, 700);
        mainWindow.setResizable(false);
        mainWindow.setLocationRelativeTo(null);
        mainWindow.setLayout(new GridLayout(0, 1,5, 5));

        mainPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        mainPanel.setBackground(Color.CYAN);

        mainWindow.add(mainPanel);

        JPanel generatorControls = new JPanel(new GridLayout(0, 1, 5, 5));

        JPanel firstloopcontainer = new JPanel(new FlowLayout());
        JLabel floop = new JLabel("Initialisation Loop:");

        NumberField floopcount = new NumberField(FIRSTLOOP, Category.firstLoop, firstMin, firstMax);
        floopcount.addPropertyChangeListener(floopcount);
        floopcount.setColumns(4);
        floopcount.setMargin(new Insets(5, 10, 5, 10));
        floopcount.setMaximumSize(new Dimension(80, 50));

        firstLoopSlide = new JSlider(JSlider.HORIZONTAL, firstMin, firstMax, FIRSTLOOP);
        firstLoopSlide.addChangeListener(new SliderListener(floopcount, Category.firstLoop, firstMin, firstMax));
        firstLoopSlide.setMajorTickSpacing(200);
        firstLoopSlide.setMinorTickSpacing(50);
        firstLoopSlide.setPaintTicks(true);
        firstLoopSlide.setPaintLabels(true);
        floopcount.setSlider(firstLoopSlide);

        firstloopcontainer.add(floop);
        firstloopcontainer.add(floopcount);
        firstloopcontainer.add(firstLoopSlide);
        generatorControls.add(firstloopcontainer);

        JPanel secondloopcontainer = new JPanel(new FlowLayout());
        JLabel sloop = new JLabel("Generation Loop:");

        NumberField sloopcount = new NumberField(GENERATIONLOOP, Category.secondLoop, secondMin, secondMax);
        sloopcount.addPropertyChangeListener(sloopcount);
        sloopcount.setColumns(3);
        sloopcount.setMargin(new Insets(5, 10, 5, 10));
        sloopcount.setMaximumSize(new Dimension(80, 50));

        secondLoopSlide = new JSlider(JSlider.HORIZONTAL, secondMin, secondMax, GENERATIONLOOP);
        secondLoopSlide.addChangeListener(new SliderListener(sloopcount, Category.secondLoop, secondMin, secondMax));
        secondLoopSlide.setMajorTickSpacing(100);
        secondLoopSlide.setMinorTickSpacing(20);
        secondLoopSlide.setPaintTicks(true);
        secondLoopSlide.setPaintLabels(true);
        sloopcount.setSlider(secondLoopSlide);

        secondloopcontainer.add(sloop);
        secondloopcontainer.add(sloopcount);
        secondloopcontainer.add(secondLoopSlide);
        generatorControls.add(secondloopcontainer);

        JPanel offspringloopcontainer = new JPanel(new FlowLayout());
        JLabel oloop = new JLabel("Offspring Per Generation:");

        NumberField oloopcount = new NumberField(OFFSPRING, Category.offspring, offspringMin, offspringMax);
        oloopcount.addPropertyChangeListener(oloopcount);
        oloopcount.setColumns(2);
        oloopcount.setMargin(new Insets(5, 10, 5, 10));
        oloopcount.setMaximumSize(new Dimension(80, 50));

        offspringLoopSlide = new JSlider(JSlider.HORIZONTAL, offspringMin, offspringMax, OFFSPRING);
        offspringLoopSlide.addChangeListener(new SliderListener(oloopcount, Category.offspring, offspringMin, offspringMax));
        offspringLoopSlide.setMajorTickSpacing(5);
        offspringLoopSlide.setMinorTickSpacing(1);
        offspringLoopSlide.setPaintTicks(true);
        offspringLoopSlide.setPaintLabels(true);
        oloopcount.setSlider(offspringLoopSlide);

        offspringloopcontainer.add(oloop);
        offspringloopcontainer.add(oloopcount);
        offspringloopcontainer.add(offspringLoopSlide);
        generatorControls.add(offspringloopcontainer);

        JPanel infeasiblecontainer = new JPanel(new FlowLayout());
        JLabel infeasibleLabel = new JLabel("Probability of Infeasible Parents:");

        NumberField infeasiblecount = new NumberField(INFEASIBLE, Category.infeasible, infeasibleMin, infeasibleMax);
        infeasiblecount.addPropertyChangeListener(infeasiblecount);
        infeasiblecount.setColumns(3);
        infeasiblecount.setMargin(new Insets(5, 10, 5, 10));
        infeasiblecount.setMaximumSize(new Dimension(80, 50));

        infeasibleSlide = new JSlider(JSlider.HORIZONTAL, infeasibleMin, infeasibleMax, INFEASIBLE);
        infeasibleSlide.addChangeListener(new SliderListener(infeasiblecount, Category.infeasible, infeasibleMin, infeasibleMax));
        infeasibleSlide.setMajorTickSpacing(10);
        infeasibleSlide.setMinorTickSpacing(5);
        infeasibleSlide.setPaintTicks(true);
        infeasibleSlide.setPaintLabels(true);
        infeasiblecount.setSlider(infeasibleSlide);

        infeasiblecontainer.add(infeasibleLabel);
        infeasiblecontainer.add(infeasiblecount);
        infeasiblecontainer.add(infeasibleSlide);
        generatorControls.add(infeasiblecontainer);

        JPanel totalHolder = new JPanel(new FlowLayout());
        totalGenerated.updateText();
        totalHolder.add(totalGenerated);
        generatorControls.add(totalHolder);

        Border blackline = BorderFactory.createLineBorder(Color.black);
        TitledBorder generatorSettings = new TitledBorder(blackline, "Generator Settings");
        generatorSettings.setTitleJustification(TitledBorder.CENTER);
        generatorControls.setBorder(generatorSettings);

        JPanel resetHolder = new JPanel(new FlowLayout());
        JButton resetGeneration = new JButton("Reset to Defaults");

        resetGeneration.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                firstLoopSlide.setValue(defaultFirst);
                secondLoopSlide.setValue(defaultSecond);
                offspringLoopSlide.setValue(defaultOffspring);
                infeasibleSlide.setValue(defaultInfeasible);
                totalGenerated.updateText();
            }
        });

        resetHolder.add(resetGeneration);
        generatorControls.add(resetHolder);

        mainPanel.add(generatorControls);

        JPanel fitnessControls = new JPanel(new GridLayout(0, 1, 5, 5));

        JPanel idealSizeContainer = new JPanel(new FlowLayout());
        JLabel idealSizeLabel = new JLabel("Target Ideal Size:");

        NumberField sizecount = new NumberField(IDEAL_SIZE, Category.idealSize, sizeMin, sizeMax);
        sizecount.addPropertyChangeListener(sizecount);
        sizecount.setColumns(3);
        sizecount.setMargin(new Insets(5, 10, 5, 10));
        sizecount.setMaximumSize(new Dimension(80, 50));

        sizeSlide = new JSlider(JSlider.HORIZONTAL, sizeMin, sizeMax, IDEAL_SIZE);
        sizeSlide.addChangeListener(new SliderListener(sizecount, Category.idealSize, sizeMin, sizeMax));
        sizeSlide.setMajorTickSpacing(50);
        sizeSlide.setMinorTickSpacing(10);
        sizeSlide.setPaintTicks(true);
        sizeSlide.setPaintLabels(true);
        sizecount.setSlider(sizeSlide);

        idealSizeContainer.add(idealSizeLabel);
        idealSizeContainer.add(sizecount);
        idealSizeContainer.add(sizeSlide);
        fitnessControls.add(idealSizeContainer);

        JPanel idealGroupContainer = new JPanel(new FlowLayout());
        JLabel idealGroupLabel = new JLabel("Target Ideal Monster Groups:");

        NumberField groupcount = new NumberField(IDEAL_GROUP, Category.idealGroups, groupMin, groupMax);
        groupcount.addPropertyChangeListener(groupcount);
        groupcount.setColumns(3);
        groupcount.setMargin(new Insets(5, 10, 5, 10));
        groupcount.setMaximumSize(new Dimension(80, 50));

        groupsSlide = new JSlider(JSlider.HORIZONTAL, groupMin, groupMax, IDEAL_GROUP);
        groupsSlide.addChangeListener(new SliderListener(groupcount, Category.idealGroups, groupMin, groupMax));
        groupsSlide.setMajorTickSpacing(1);
        groupsSlide.setPaintTicks(true);
        groupsSlide.setPaintLabels(true);
        groupcount.setSlider(groupsSlide);

        idealGroupContainer.add(idealGroupLabel);
        idealGroupContainer.add(groupcount);
        idealGroupContainer.add(groupsSlide);
        fitnessControls.add(idealGroupContainer);

        JPanel idealHealthContainer = new JPanel(new FlowLayout());
        JLabel idealHealthLabel = new JLabel("Target Ideal Average Monster Health:");

        NumberField healthcount = new NumberField(IDEAL_HEALTH, Category.idealHealth, healthMin, healthMax);
        healthcount.addPropertyChangeListener(healthcount);
        healthcount.setColumns(5);
        healthcount.setMargin(new Insets(5, 10, 5, 10));
        healthcount.setMaximumSize(new Dimension(80, 50));

        healthSlide = new JSlider(JSlider.HORIZONTAL, (int) healthMin * 1000, (int) healthMax * 1000, (int) IDEAL_HEALTH * 1000);
        healthSlide.addChangeListener(new SliderListener(healthcount, Category.idealHealth, healthMin * 1000, healthMax * 1000));
        healthSlide.setMajorTickSpacing(6000);
        healthSlide.setMinorTickSpacing(1000);
        healthSlide.setPaintTicks(true);
        healthSlide.setPaintLabels(true);
        healthcount.setSlider(healthSlide);

        idealHealthContainer.add(idealHealthLabel);
        idealHealthContainer.add(healthcount);
        idealHealthContainer.add(healthSlide);
        fitnessControls.add(idealHealthContainer);

        JPanel idealHeightContainer = new JPanel(new FlowLayout());
        JLabel idealHeightLabel = new JLabel("Target Ideal Board Height:");

        NumberField heightcount = new NumberField(IDEAL_HEIGHT, Category.idealHeight, heightMin, heightMax);
        heightcount.addPropertyChangeListener(heightcount);
        heightcount.setColumns(2);
        heightcount.setMargin(new Insets(5, 10, 5, 10));
        heightcount.setMaximumSize(new Dimension(80, 50));

        heightSlide = new JSlider(JSlider.HORIZONTAL, heightMin, heightMax, IDEAL_HEIGHT);
        heightSlide.addChangeListener(new SliderListener(heightcount, Category.idealHeight, heightMin, heightMax));
        heightSlide.setMajorTickSpacing(6);
        heightSlide.setMinorTickSpacing(1);
        heightSlide.setPaintTicks(true);
        heightSlide.setPaintLabels(true);
        heightcount.setSlider(heightSlide);

        idealHeightContainer.add(idealHeightLabel);
        idealHeightContainer.add(heightcount);
        idealHeightContainer.add(heightSlide);
        fitnessControls.add(idealHeightContainer);

        JPanel idealWidthContainer = new JPanel(new FlowLayout());
        JLabel idealWidthLabel = new JLabel("Target Ideal Board Width:");

        NumberField widthcount = new NumberField(IDEAL_WIDTH, Category.idealWidth, widthMin, widthMax);
        widthcount.addPropertyChangeListener(widthcount);
        widthcount.setColumns(2);
        widthcount.setMargin(new Insets(5, 10, 5, 10));
        widthcount.setMaximumSize(new Dimension(80, 50));

        widthSlide = new JSlider(JSlider.HORIZONTAL, widthMin, widthMax, IDEAL_WIDTH);
        widthSlide.addChangeListener(new SliderListener(widthcount, Category.idealWidth, widthMin, widthMax));
        widthSlide.setMajorTickSpacing(6);
        widthSlide.setMinorTickSpacing(1);
        widthSlide.setPaintTicks(true);
        widthSlide.setPaintLabels(true);
        widthcount.setSlider(widthSlide);

        idealWidthContainer.add(idealWidthLabel);
        idealWidthContainer.add(widthcount);
        idealWidthContainer.add(widthSlide);
        fitnessControls.add(idealWidthContainer);

        JPanel defaults = new JPanel(new FlowLayout());
        JLabel defaultLabel = new JLabel("Use Default Ideal Variables");
        JCheckBox useDefaults = new JCheckBox();
        JLabel defaultList = new JLabel("(" + defaultSize + " Spaces, " + defaultGroups + " Groups, " + defaultHealth + " Average Health, " + defaultHeight + "x" + defaultWidth + " Board Size)");
        useDefaults.setSelected(false);
        defaultIdeals = false;
        useDefaults.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean result = !useDefaults.isSelected();
                defaultIdeals = !result;
                sizeSlide.setEnabled(result);
                groupsSlide.setEnabled(result);
                healthSlide.setEnabled(result);
                heightSlide.setEnabled(result);
                widthSlide.setEnabled(result);

                sizecount.setEnabled(result);
                groupcount.setEnabled(result);
                healthcount.setEnabled(result);
                heightcount.setEnabled(result);
                widthcount.setEnabled(result);
            }
        });
        defaults.add(defaultLabel);
        defaults.add(useDefaults);
        defaults.add(defaultList);
        fitnessControls.add(defaults);

        TitledBorder fitnessSettings = new TitledBorder(blackline, "Ideal Fitness Variable Settings");
        fitnessSettings.setTitleJustification(TitledBorder.CENTER);
        fitnessControls.setBorder(fitnessSettings);
        mainPanel.add(fitnessControls);

        JPanel weightControls = new JPanel(new GridLayout(0, 1, 5, 5));

        JPanel weightSizeContainer = new JPanel(new FlowLayout());
        JLabel weightSizeLabel = new JLabel("Ideal Size Weight:");
        JLabel weightPercent = new JLabel("%");

        NumberField weightsizecount = new NumberField((W_SIZE * 100), Category.weightSize, minWeight, maxWeight);
        weightsizecount.addPropertyChangeListener(weightsizecount);
        weightsizecount.setColumns(4);
        weightsizecount.setMargin(new Insets(5, 10, 5, 10));
        weightsizecount.setMaximumSize(new Dimension(80, 50));

        weightSizeSlide = new JSlider(JSlider.HORIZONTAL, minWeight, maxWeight, (int) (W_SIZE * 100));
        weightSizeSlide.addChangeListener(new SliderListener(weightsizecount, Category.weightSize, minWeight, maxWeight));
        weightSizeSlide.setMajorTickSpacing(200);
        weightSizeSlide.setMinorTickSpacing(50);
        weightSizeSlide.setPaintTicks(true);
        weightSizeSlide.setPaintLabels(true);
        weightsizecount.setSlider(weightSizeSlide);

        weightSizeContainer.add(weightSizeLabel);
        weightSizeContainer.add(weightsizecount);
        weightSizeContainer.add(weightPercent);
        weightSizeContainer.add(weightSizeSlide);
        weightControls.add(weightSizeContainer);

        JPanel weightHealthContainer = new JPanel(new FlowLayout());
        JLabel weightHealthLabel = new JLabel("Ideal Monster Health Weight:");
        JLabel weightPercent2 = new JLabel("%");

        NumberField weighthealthcount = new NumberField((W_HEALTH * 100), Category.weightHealth, minWeight, maxWeight);
        weighthealthcount.addPropertyChangeListener(weighthealthcount);
        weighthealthcount.setColumns(4);
        weighthealthcount.setMargin(new Insets(5, 10, 5, 10));
        weighthealthcount.setMaximumSize(new Dimension(80, 50));

        weightHealthSlide = new JSlider(JSlider.HORIZONTAL, minWeight, maxWeight, (int) (W_HEALTH * 100));
        weightHealthSlide.addChangeListener(new SliderListener(weighthealthcount, Category.weightHealth, minWeight, maxWeight));
        weightHealthSlide.setMajorTickSpacing(200);
        weightHealthSlide.setMinorTickSpacing(50);
        weightHealthSlide.setPaintTicks(true);
        weightHealthSlide.setPaintLabels(true);
        weighthealthcount.setSlider(weightHealthSlide);

        weightHealthContainer.add(weightHealthLabel);
        weightHealthContainer.add(weighthealthcount);
        weightHealthContainer.add(weightPercent2);
        weightHealthContainer.add(weightHealthSlide);
        weightControls.add(weightHealthContainer);

        JPanel weightGroupsContainer = new JPanel(new FlowLayout());
        JLabel weightGroupsLabel = new JLabel("Ideal Monster Groups Weight:");
        JLabel weightPercent3 = new JLabel("%");

        NumberField weightgroupcount = new NumberField((W_GROUPS * 100), Category.weightGroups, minWeight, maxWeight);
        weightgroupcount.addPropertyChangeListener(weightgroupcount);
        weightgroupcount.setColumns(4);
        weightgroupcount.setMargin(new Insets(5, 10, 5, 10));
        weightgroupcount.setMaximumSize(new Dimension(80, 50));

        weightGroupsSlide = new JSlider(JSlider.HORIZONTAL, minWeight, maxWeight, (int) (W_GROUPS * 100));
        weightGroupsSlide.addChangeListener(new SliderListener(weightgroupcount, Category.weightGroups, minWeight, maxWeight));
        weightGroupsSlide.setMajorTickSpacing(200);
        weightGroupsSlide.setMinorTickSpacing(50);
        weightGroupsSlide.setPaintTicks(true);
        weightGroupsSlide.setPaintLabels(true);
        weightgroupcount.setSlider(weightGroupsSlide);

        weightGroupsContainer.add(weightGroupsLabel);
        weightGroupsContainer.add(weightgroupcount);
        weightGroupsContainer.add(weightPercent3);
        weightGroupsContainer.add(weightGroupsSlide);
        weightControls.add(weightGroupsContainer);

        JPanel weightHeightContainer = new JPanel(new FlowLayout());
        JLabel weightHeightLabel = new JLabel("Ideal Board Height Weight:");
        JLabel weightPercent4 = new JLabel("%");

        NumberField weightheightcount = new NumberField((W_HEIGHT * 100), Category.weightHeight, minWeight, maxWeight);
        weightheightcount.addPropertyChangeListener(weightheightcount);
        weightheightcount.setColumns(4);
        weightheightcount.setMargin(new Insets(5, 10, 5, 10));
        weightheightcount.setMaximumSize(new Dimension(80, 50));

        weightHeightSlide = new JSlider(JSlider.HORIZONTAL, minWeight, maxWeight, (int) (W_HEIGHT * 100));
        weightHeightSlide.addChangeListener(new SliderListener(weightheightcount, Category.weightHeight, minWeight, maxWeight));
        weightHeightSlide.setMajorTickSpacing(200);
        weightHeightSlide.setMinorTickSpacing(50);
        weightHeightSlide.setPaintTicks(true);
        weightHeightSlide.setPaintLabels(true);
        weightheightcount.setSlider(weightHeightSlide);

        weightHeightContainer.add(weightHeightLabel);
        weightHeightContainer.add(weightheightcount);
        weightHeightContainer.add(weightPercent4);
        weightHeightContainer.add(weightHeightSlide);
        weightControls.add(weightHeightContainer);

        JPanel weightWidthContainer = new JPanel(new FlowLayout());
        JLabel weightWidthLabel = new JLabel("Ideal Board Height Weight:");
        JLabel weightPercent5 = new JLabel("%");

        NumberField weightwidthcount = new NumberField((W_HEIGHT * 100), Category.weightWidth, minWeight, maxWeight);
        weightwidthcount.addPropertyChangeListener(weightwidthcount);
        weightwidthcount.setColumns(4);
        weightwidthcount.setMargin(new Insets(5, 10, 5, 10));
        weightwidthcount.setMaximumSize(new Dimension(80, 50));

        weightWidthSlide = new JSlider(JSlider.HORIZONTAL, minWeight, maxWeight, (int) (W_WIDTH * 100));
        weightWidthSlide.addChangeListener(new SliderListener(weightwidthcount, Category.weightWidth, minWeight, maxWeight));
        weightWidthSlide.setMajorTickSpacing(200);
        weightWidthSlide.setMinorTickSpacing(50);
        weightWidthSlide.setPaintTicks(true);
        weightWidthSlide.setPaintLabels(true);
        weightwidthcount.setSlider(weightWidthSlide);

        weightWidthContainer.add(weightWidthLabel);
        weightWidthContainer.add(weightwidthcount);
        weightWidthContainer.add(weightPercent5);
        weightWidthContainer.add(weightWidthSlide);
        weightControls.add(weightWidthContainer);

        JPanel resetWeightHolder = new JPanel(new FlowLayout());
        JButton resetWeights = new JButton("Reset to Defaults");
        resetWeights.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                weightSizeSlide.setValue(100);
                weightGroupsSlide.setValue(100);
                weightHeightSlide.setValue(100);
                weightHeightSlide.setValue(0);
                weightWidthSlide.setValue(0);
            }
        });
        resetWeightHolder.add(resetWeights);
        weightControls.add(resetWeightHolder);

        TitledBorder weightSettings = new TitledBorder(blackline, "Fitness Function Weight Values");
        weightSettings.setTitleJustification(TitledBorder.CENTER);
        weightControls.setBorder(weightSettings);
        mainPanel.add(weightControls);

        JPanel outputHolder = new JPanel();
        outputHolder.setLayout(new BoxLayout(outputHolder, BoxLayout.Y_AXIS));
        create = new JButton("Generate!");
        JPanel buttonHolder = new JPanel(new FlowLayout());
        buttonHolder.setPreferredSize(new Dimension(450, 30));
        buttonHolder.add(create);
        TitledBorder buttonSettings = new TitledBorder(blackline, "Board Generation");
        buttonSettings.setTitleJustification(TitledBorder.CENTER);
        buttonHolder.setBorder(buttonSettings);
        outputHolder.add(buttonHolder);


        generationText = new JTextArea(20, 50);
        generationText.setLayout(new BoxLayout(generationText, BoxLayout.Y_AXIS));
        TitledBorder generationOutput = new TitledBorder("Output");
        generationOutput.setTitleJustification(TitledBorder.CENTER);
        generationText.setBorder(generationOutput);
        outputScroll = new JScrollPane(generationText);
        outputScroll.setPreferredSize(new Dimension(450, 230));
        outputHolder.add(outputScroll);

        mainPanel.add(outputHolder);
        create.setEnabled(true);

        GenerateBoardsGUI gui = this;
        System.setOut(new PrintStream(new CustomOutputStream(generationText)));

        create.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(create.isEnabled()) {
                    create.setEnabled(false);
                    generationText.setText("");
                    generationText.revalidate();
                    generationText.repaint();
                    try {
                        print("Generating " + (FIRSTLOOP + (GENERATIONLOOP * OFFSPRING)) + " Boards, with " + INFEASIBLE +"% chance of Infeasible Pool Parents!");
                    } catch (InterruptedException | InvocationTargetException ex) {
                        throw new RuntimeException(ex);
                    }
                    CreateOffspring co = new CreateOffspring(FIRSTLOOP, GENERATIONLOOP, OFFSPRING, INFEASIBLE);
                    co.setGUI(gui);

                    if (defaultIdeals)
                        co.setIdeals(defaultSize, defaultGroups, defaultHealth, defaultHeight, defaultWidth);
                    else
                        co.setIdeals(IDEAL_SIZE, IDEAL_GROUP, IDEAL_HEALTH, IDEAL_HEIGHT, IDEAL_WIDTH);
                    co.setWeights(W_SIZE, W_GROUPS, W_HEALTH, W_HEIGHT, W_WIDTH);

                    try {
                        co.begin();
                    } catch (IOException | InterruptedException | InvocationTargetException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        mainWindow.setVisible(true);
    }

    public Button makeButton(String label) {
        Button button = new Button(label);
        button.setFocusable(false);
        button.setFont(new Font("Arial", Font.PLAIN, 10));
        return button;
    }

    void finished() {
        if (create != null)
            create.setEnabled(true);
    }

    void print(String string) throws InterruptedException, InvocationTargetException {
        //String text = generationText.getText();
        //generationText.setText(text + "\n" + string);
        //generationText.revalidate();
        System.out.println(string);
        Rectangle rect = new Rectangle(generationText.getX(), generationText.getY(), generationText.getWidth(), generationText.getHeight());
        generationText.paintImmediately(rect);
        generationText.update(generationText.getGraphics());
    }

    class TotalLabel extends JLabel {
        public void updateText() {
            int total = FIRSTLOOP + (GENERATIONLOOP * OFFSPRING);
            setText("Generating " + total + " new Descent Quests with " + INFEASIBLE + "% likelihood of drawing parents from the Infeasible pool.");
        }
    }

    class NumberField extends JFormattedTextField implements PropertyChangeListener {
        private JSlider slider;
        private final Category category;
        private final double min;
        private final double max;

        public NumberField(int value, Category category, double min, double max) {
            super(value);
            this.category = category;
            this.min = min;
            this.max = max;
        }

        public NumberField(float value, Category category, double min, double max) {
            super(value);
            this.category = category;
            this.min = min;
            this.max = max;
        }

        public void setSlider(JSlider slider){
            this.slider = slider;
        }
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            float value = ((Number) this.getValue()).floatValue();
            float result = (float) Math.min(Math.max(value, min), max);
            switch (category) {
                case firstLoop -> {
                    FIRSTLOOP = (int) Math.max(result, 4);
                    this.setValue(FIRSTLOOP);
                    if (slider != null)
                        slider.setValue(FIRSTLOOP);
                }
                case secondLoop -> {
                    GENERATIONLOOP = (int) result;
                    this.setValue(GENERATIONLOOP);
                    if (slider != null)
                        slider.setValue(GENERATIONLOOP);
                }
                case offspring -> {
                    OFFSPRING = (int) result;
                    this.setValue(OFFSPRING);
                    if (slider != null)
                        slider.setValue(OFFSPRING);
                }
                case infeasible -> {
                    INFEASIBLE = (int) result;
                    this.setValue(INFEASIBLE);
                    if (slider != null)
                        slider.setValue(INFEASIBLE);
                }
                case idealSize -> {
                    IDEAL_SIZE = (int) result;
                    this.setValue(IDEAL_SIZE);
                    if (slider != null)
                        slider.setValue(IDEAL_SIZE);
                }
                case idealGroups -> {
                    IDEAL_GROUP = (int) result;
                    this.setValue(IDEAL_GROUP);
                    if (slider != null)
                        slider.setValue(IDEAL_GROUP);
                }
                case idealHealth -> {
                    IDEAL_HEALTH = result;
                    this.setValue(IDEAL_HEALTH);
                    if (slider != null)
                        slider.setValue((int) (IDEAL_HEALTH * 1000));
                }
                case idealHeight -> {
                    IDEAL_HEIGHT = (int) result;
                    this.setValue(IDEAL_HEIGHT);
                    if (slider != null)
                        slider.setValue(IDEAL_HEIGHT);
                }
                case idealWidth -> {
                    IDEAL_WIDTH = (int) result;
                    this.setValue(IDEAL_WIDTH);
                    if (slider != null)
                        slider.setValue(IDEAL_WIDTH);
                }
                case weightHealth -> {
                    W_HEALTH = result / 100;
                    this.setValue(result);
                    if (slider != null)
                        slider.setValue((int) (W_HEALTH * 100));
                }
                case weightGroups -> {
                    W_GROUPS = result / 100;
                    this.setValue(result);
                    if (slider != null)
                        slider.setValue((int) (W_GROUPS * 100));
                }
                case weightSize -> {
                    W_SIZE = result / 100;
                    this.setValue(result);
                    if (slider != null)
                        slider.setValue((int) (W_SIZE * 100));
                }
                case weightHeight -> {
                    W_HEIGHT = result / 100;
                    this.setValue(result);
                    if (slider != null)
                        slider.setValue((int) (W_HEIGHT * 100));
                }
                case weightWidth -> {
                    W_WIDTH = result / 100;
                    this.setValue(result);
                    if (slider != null)
                        slider.setValue((int) (W_WIDTH * 100));
                }
            }
            totalGenerated.updateText();
        }
    }

    class SliderListener implements ChangeListener {
        private final Category category;
        private final JFormattedTextField text;
        private final double min;
        private final double max;
        public SliderListener(JFormattedTextField text, Category category, double min, double max) {
            this.text = text;
            this.category = category;
            this.min = min;
            this.max = max;
        }

        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                float value = source.getValue();
                float result = (float) Math.max(min, Math.min(value, max));
                switch (category) {
                    case firstLoop -> {
                        FIRSTLOOP = (int) Math.max(result, 4);
                        text.setValue(FIRSTLOOP);
                        source.setValue(FIRSTLOOP);
                    }
                    case secondLoop -> {
                        GENERATIONLOOP = (int) result;
                        text.setValue(GENERATIONLOOP);
                        source.setValue(GENERATIONLOOP);
                    }
                    case offspring -> {
                        OFFSPRING = (int) result;
                        text.setValue(OFFSPRING);
                        source.setValue(OFFSPRING);
                    }
                    case infeasible -> {
                        INFEASIBLE = (int) result;
                        text.setValue(INFEASIBLE);
                        source.setValue(INFEASIBLE);
                    }
                    case idealSize -> {
                        IDEAL_SIZE = (int) result;
                        text.setValue(IDEAL_SIZE);
                        source.setValue(IDEAL_SIZE);
                    }
                    case idealGroups -> {
                        IDEAL_GROUP = (int) result;
                        text.setValue(IDEAL_GROUP);
                        source.setValue(IDEAL_GROUP);
                    }
                    case idealHealth -> {
                        IDEAL_HEALTH = result / 1000;
                        text.setValue(IDEAL_HEALTH);
                        source.setValue((int) (IDEAL_HEALTH * 1000));
                    }
                    case idealHeight -> {
                        IDEAL_HEIGHT = (int) result;
                        text.setValue(IDEAL_HEIGHT);
                        source.setValue(IDEAL_HEIGHT);
                    }
                    case idealWidth -> {
                        IDEAL_WIDTH = (int) result;
                        text.setValue(IDEAL_WIDTH);
                        source.setValue(IDEAL_WIDTH);
                    }
                    case weightHealth -> {
                        W_HEALTH = result / 100;
                        text.setValue(result);
                        source.setValue((int) (W_HEALTH * 100));
                    }
                    case weightSize -> {
                        W_SIZE = result / 100;
                        text.setValue(result);
                        source.setValue((int) (W_SIZE * 100));
                    }
                    case weightGroups -> {
                        W_GROUPS = result / 100;
                        text.setValue(result);
                        source.setValue((int) (W_GROUPS * 100));
                    }
                    case weightHeight -> {
                        W_HEIGHT = result / 100;
                        text.setValue(result);
                        source.setValue((int) (W_HEIGHT * 100));
                    }
                    case weightWidth -> {
                        W_WIDTH = result / 100;
                        text.setValue(result);
                        source.setValue((int) (W_WIDTH * 100));
                    }
                }
                totalGenerated.updateText();
            }
        }
    }

    public class CustomOutputStream extends OutputStream {
        private final JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) throws IOException {
            // redirects data to the text area
            textArea.append(String.valueOf((char)b));
            // scrolls the text area to the end of data
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    enum Category {
        firstLoop,
        secondLoop,
        offspring,
        infeasible,
        idealHealth,
        idealSize,
        idealGroups,
        idealHeight,
        idealWidth,
        weightHealth,
        weightSize,
        weightGroups,
        weightHeight,
        weightWidth
    }
}
