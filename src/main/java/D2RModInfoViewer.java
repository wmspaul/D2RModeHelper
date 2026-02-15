import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class D2RModInfoViewer {
    private static final String d2rmmLocation = "D:\\Diablo II Resurrected\\mods\\D2RMM\\D2RMM.mpq\\data\\global\\excel";
    private static JList<String> fileList;
    private static JTable table;
    private static File[] txtFiles;
    private static final int MIN_COLUMN_WIDTH = 100;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI(d2rmmLocation));
    }

    private static void createAndShowGUI(String baseDir) {
        JFrame frame = new JFrame("D2R Excel Browser (Fixed Width)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);

        // Left navigation panel
        DefaultListModel<String> listModel = new DefaultListModel<>();

        File folder = new File(baseDir);
        if (!folder.exists() || !folder.isDirectory()) {
            JOptionPane.showMessageDialog(null, "Invalid folder: " + baseDir, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        txtFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (txtFiles == null || txtFiles.length == 0) {
            JOptionPane.showMessageDialog(null, "No .txt files found in: " + baseDir, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (File f : txtFiles) {
            String name = f.getName();
            if (name.toLowerCase().endsWith(".txt")) {
                name = name.substring(0, name.length() - 4); // remove .txt
            }
            listModel.addElement(name);
        }

        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setSelectedIndex(0);

        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setPreferredSize(new Dimension(200, 0));

        // Right content panel
        table = new JTable();
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // <-- Disable auto resizing

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, tableScroll);
        splitPane.setDividerLocation(200);
        splitPane.setOneTouchExpandable(true);

        frame.getContentPane().add(splitPane, BorderLayout.CENTER);

        // Load initial selection
        loadTableFromFile(txtFiles[0], table);

        // Selection listener
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = fileList.getSelectedIndex();
                if (idx >= 0 && idx < txtFiles.length) {
                    loadTableFromFile(txtFiles[idx], table);
                }
            }
        });

        frame.setVisible(true);
    }

    private static void loadTableFromFile(File file, JTable table) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            if (lines.isEmpty()) return;

            // Header
            String[] headers = lines.get(0).split("\t");
            DefaultTableModel model = new DefaultTableModel(headers, 0);

            // Data rows (skip header + MaxLvl row if present)
            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] cells = line.split("\t");
                model.addRow(cells);
            }

            table.setModel(model);

            // Set minimum width for all columns
            for (int i = 0; i < table.getColumnCount(); i++) {
                TableColumn column = table.getColumnModel().getColumn(i);
                column.setMinWidth(MIN_COLUMN_WIDTH);
                column.setPreferredWidth(MIN_COLUMN_WIDTH); // <-- ensures scroll works
            }

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to read file: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

