package com.ransom.d2r.viewers;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class D2RModRunewordsViewer {
    private static final String d2rmmLocation = "D:\\Diablo II Resurrected\\mods\\D2RMM\\D2RMM.mpq\\data\\global\\excel";
    private static final int MIN_COLUMN_WIDTH = 100;

    private static Map<String, Integer> runeLevels = new HashMap<>();           // misc.txt: code -> level
    private static Map<String, String> itemTypeLookup = new HashMap<>();       // itemtypes.txt: code -> ItemType
    private static Map<String, String> tooltipLookup = new HashMap<>();        // properties.txt: code -> *Tooltip
    private static List<RuneWord> runeWords = new ArrayList<>();
    private static JTable table;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI(d2rmmLocation));
    }


    private static void createAndShowGUI(String baseDir) {
        JFrame frame = new JFrame("D2R RuneWords Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1300, 700);

        File folder = new File(baseDir);
        File runesFile = new File(folder, "runes.txt");
        File miscFile = new File(folder, "misc.txt");
        File itemTypesFile = new File(folder, "itemtypes.txt");
        File propertiesFile = new File(folder, "properties.txt");

        if (!runesFile.exists() || !miscFile.exists() || !itemTypesFile.exists() || !propertiesFile.exists()) {
            JOptionPane.showMessageDialog(null, "One or more required files not found in folder: " + baseDir, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        loadMisc(miscFile);
        loadItemTypes(itemTypesFile);
        loadTooltips(propertiesFile);
        loadRuneWords(runesFile);

        // Gather all unique item types from rune words
        Set<String> allItemTypes = new TreeSet<>();
        for (RuneWord rw : runeWords) {
            allItemTypes.addAll(rw.itemTypes);
        }

        // Left navigation bar
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String type : allItemTypes) listModel.addElement(type);

        JList<String> navList = new JList<>(listModel);
        navList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane navScroll = new JScrollPane(navList);
        navScroll.setPreferredSize(new Dimension(200, 700));

        // Right panel table
        table = new JTable();
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navScroll, tableScroll);
        splitPane.setDividerLocation(200);
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);

        navList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedType = navList.getSelectedValue();
                    if (selectedType != null) {
                        loadTableForItemType(selectedType);
                    }
                }
            }
        });

        // Select first item type by default
        if (!allItemTypes.isEmpty()) navList.setSelectedIndex(0);

        frame.setVisible(true);
    }

    // --- Load misc.txt ---
    private static void loadMisc(File miscFile) {
        try {
            List<String> lines = Files.readAllLines(miscFile.toPath());
            if (lines.isEmpty()) return;
            String[] headers = lines.get(0).split("\t");
            int codeIndex = Arrays.asList(headers).indexOf("code");
            int levelIndex = Arrays.asList(headers).indexOf("level");

            for (int i = 2; i < lines.size(); i++) {
                String[] cells = lines.get(i).split("\t");
                if (cells.length > Math.max(codeIndex, levelIndex)) {
                    try {
                        runeLevels.put(cells[codeIndex], Integer.parseInt(cells[levelIndex]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Load itemtypes.txt ---
    private static void loadItemTypes(File itemTypesFile) {
        try {
            List<String> lines = Files.readAllLines(itemTypesFile.toPath());
            if (lines.isEmpty()) return;
            String[] headers = lines.get(0).split("\t");
            int codeIndex = Arrays.asList(headers).indexOf("Code");
            int typeIndex = Arrays.asList(headers).indexOf("ItemType");

            for (int i = 2; i < lines.size(); i++) {
                String[] cells = lines.get(i).split("\t");
                if (cells.length > Math.max(codeIndex, typeIndex)) {
                    itemTypeLookup.put(cells[codeIndex], cells[typeIndex]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Load properties.txt ---
    private static void loadTooltips(File propertiesFile) {
        try {
            List<String> lines = Files.readAllLines(propertiesFile.toPath());
            if (lines.isEmpty()) return;
            String[] headers = lines.get(0).split("\t");
            int codeIndex = Arrays.asList(headers).indexOf("code");
            int tooltipIndex = Arrays.asList(headers).indexOf("*Tooltip");

            for (int i = 2; i < lines.size(); i++) {
                String[] cells = lines.get(i).split("\t");
                if (cells.length > Math.max(codeIndex, tooltipIndex)) {
                    tooltipLookup.put(cells[codeIndex], cells[tooltipIndex]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Load runes.txt ---
    private static void loadRuneWords(File runesFile) {
        try {
            List<String> lines = Files.readAllLines(runesFile.toPath());
            if (lines.isEmpty()) return;
            String[] headers = lines.get(0).split("\t");

            int runeNameIndex = Arrays.asList(headers).indexOf("*Rune Name");
            int runesUsedIndex = Arrays.asList(headers).indexOf("*RunesUsed");

            int[] runeCodeIndexes = new int[6];
            int[] itypeIndexes = new int[6];
            for (int i = 0; i < 6; i++) {
                runeCodeIndexes[i] = Arrays.asList(headers).indexOf("Rune" + (i + 1));
                itypeIndexes[i] = Arrays.asList(headers).indexOf("itype" + (i + 1));
            }

            int[] bonusCodeIndexes = new int[7];
            int[] bonusMinIndexes = new int[7];
            int[] bonusMaxIndexes = new int[7];
            for (int i = 0; i < 7; i++) {
                bonusCodeIndexes[i] = Arrays.asList(headers).indexOf("T1Code" + (i + 1));
                bonusMinIndexes[i] = Arrays.asList(headers).indexOf("T1Min" + (i + 1));
                bonusMaxIndexes[i] = Arrays.asList(headers).indexOf("T1Max" + (i + 1));
            }

            for (int i = 2; i < lines.size(); i++) {
                String[] cells = lines.get(i).split("\t");
                String name = runeNameIndex >= 0 && runeNameIndex < cells.length ? cells[runeNameIndex] : "Unknown";
                String runesUsed = runesUsedIndex >= 0 && runesUsedIndex < cells.length ? cells[runesUsedIndex] : "";
                if (runesUsed.isEmpty()) continue;

                List<String> runeCodes = new ArrayList<>();
                for (int idx : runeCodeIndexes) {
                    if (idx >= 0 && idx < cells.length && !cells[idx].isEmpty())
                        runeCodes.add(cells[idx]);
                }
                if (runeCodes.isEmpty()) continue; // skip rune words with no runes

                List<String> types = new ArrayList<>();
                for (int idx : itypeIndexes) {
                    if (idx >= 0 && idx < cells.length && !cells[idx].isEmpty()) {
                        String typeName = itemTypeLookup.getOrDefault(cells[idx], cells[idx]);
                        types.add(typeName);
                    }
                }

                int requiredLevel = 0;
                for (String rc : runeCodes) {
                    Integer lvl = runeLevels.get(rc);
                    if (lvl != null && lvl > requiredLevel)
                        requiredLevel = lvl;
                }

                StringBuilder bonuses = new StringBuilder();
                for (int j = 0; j < 7; j++) {
                    String codeStr = (bonusCodeIndexes[j] >= 0 && bonusCodeIndexes[j] < cells.length) ? cells[bonusCodeIndexes[j]] : "";
                    if (codeStr.isEmpty()) continue;
                    String desc = tooltipLookup.getOrDefault(codeStr, codeStr);

                    String min = (bonusMinIndexes[j] >= 0 && bonusMinIndexes[j] < cells.length) ? cells[bonusMinIndexes[j]] : "0";
                    String max = (bonusMaxIndexes[j] >= 0 && bonusMaxIndexes[j] < cells.length) ? cells[bonusMaxIndexes[j]] : "0";

                    bonuses.append(desc).append(": ").append(min).append("-").append(max).append("\n");
                }

                runeWords.add(new RuneWord(name, runesUsed, requiredLevel, types, bonuses.toString()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadTableForItemType(String itemType){
        DefaultTableModel model = new DefaultTableModel(new String[]{"Rune Name","Required Level","Runes Required","Bonuses"},0);
        for(RuneWord rw : runeWords){
            if(rw.itemTypes.contains(itemType)){
                model.addRow(new Object[]{rw.name,rw.requiredLevel,rw.runesUsed,rw.bonuses});
            }
        }
        table.setModel(model);
        for(int i=0;i<table.getColumnCount();i++){
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setMinWidth(MIN_COLUMN_WIDTH);
            if(i==3) col.setPreferredWidth(400);
        }

        // Set multiline renderer for Bonuses column
        table.getColumnModel().getColumn(3).setCellRenderer(new MultiLineCellRenderer());
    }

    private static class RuneWord {
        String name;
        String runesUsed;
        int requiredLevel;
        List<String> itemTypes;
        String bonuses;

        public RuneWord(String name, String runesUsed, int requiredLevel, List<String> itemTypes, String bonuses) {
            this.name = name;
            this.runesUsed = runesUsed;
            this.requiredLevel = requiredLevel;
            this.itemTypes = itemTypes;
            this.bonuses = bonuses;
        }
    }

    private static class MultiLineCellRenderer extends JTextArea implements TableCellRenderer {

        public MultiLineCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText(value == null ? "" : value.toString());
            setFont(table.getFont());
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            // Adjust the row height to fit the text
            int prefHeight = getPreferredSize().height;
            if (table.getRowHeight(row) != prefHeight) {
                table.setRowHeight(row, prefHeight);
            }
            return this;
        }
    }
}