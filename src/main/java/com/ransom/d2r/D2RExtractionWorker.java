package com.ransom.d2r;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class D2RExtractionWorker extends SwingWorker<Void, String> {

    private final D2RExtractionManager manager;
    private final JProgressBar progressBar;
    private final JTextArea logArea;
    private final JButton cancelButton;
    private final JButton startButton;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public D2RExtractionWorker(D2RExtractionManager manager,
                               JProgressBar progressBar,
                               JTextArea logArea,
                               JButton startButton,
                               JButton cancelButton) {
        this.manager = manager;
        this.progressBar = progressBar;
        this.logArea = logArea;
        this.startButton = startButton;
        this.cancelButton = cancelButton;
    }

    public void requestCancel() {
        cancelled.set(true);
    }

    @Override
    protected Void doInBackground() throws Exception {

        manager.extractExcelWithProgress((percent, message) -> {
            publish(message);
            setProgress(percent);
            System.out.println("Progress: " + getProgress());
        }, cancelled);

        return null;
    }

    @Override
    protected void process(List<String> chunks) {
        for (String msg : chunks) {
            logArea.append(msg + "\n");
        }
        progressBar.setValue(getProgress());
    }

    @Override
    protected void done() {
        cancelButton.setEnabled(false);
        startButton.setEnabled(true);

        try {
            get();
            JOptionPane.showMessageDialog(null,
                    "Extraction completed successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    e.getCause().getMessage(),
                    "Extraction Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}