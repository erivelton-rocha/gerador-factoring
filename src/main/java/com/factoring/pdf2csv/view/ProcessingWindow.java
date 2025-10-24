package com.factoring.pdf2csv.view;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ProcessingWindow {
    private JFrame frame;
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JProgressBar progressBar;

    public ProcessingWindow() {
        frame = new JFrame("Processamento de Arquivo");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Adicionar a barra de progresso ao rodapé
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        frame.add(progressBar, BorderLayout.SOUTH);

        // Centraliza o JFrame na tela
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void appendText(String text) {
        textArea.append(text + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    public void setProgress(int value) {
        progressBar.setValue(value);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Erro", JOptionPane.ERROR_MESSAGE);
        close(); // Fechar a janela após a confirmação do erro
    }

    public void close() {
        frame.dispose();
    }
}
