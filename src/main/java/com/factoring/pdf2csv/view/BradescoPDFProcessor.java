package com.factoring.pdf2csv.view;


import org.apache.pdfbox.Loader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BradescoPDFProcessor extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Titulo> data;

    public BradescoPDFProcessor() {
        data = new ArrayList<>();

        setTitle("Bradesco PDF Processor");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new String[]{"Vencimento", "Documento", "Sacado", "Valor Nota", "Juros", "Tarifa", "IOF", "Valor Líquido"}, 0);
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel();
        JButton uploadButton = new JButton("Upload PDF");
        uploadButton.addActionListener(new UploadActionListener());
        panel.add(uploadButton);

        JButton sendButton = new JButton("Send Data");
        sendButton.addActionListener(new SendDataActionListener());
        panel.add(sendButton);

        add(panel, BorderLayout.SOUTH);
    }

    private class UploadActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            int result = fileChooser.showOpenDialog(BradescoPDFProcessor.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] files = fileChooser.getSelectedFiles();
                for (File file : files) {
                    try {
                        List<Titulo> extractedData = extractDataFromPDF(file);
                        for (Titulo titulo : extractedData) {
                            tableModel.addRow(new Object[]{
                                titulo.getVencimento(),
                                titulo.getDocumento(),
                                titulo.getSacado(),
                                titulo.getValorNota(),
                                titulo.getJuros(),
                                titulo.getTarifa(),
                                titulo.getIof(),
                                titulo.getValorLiquido()
                            });
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(BradescoPDFProcessor.this, "Error processing file: " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    private class SendDataActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selectedRows = table.getSelectedRows();
            List<Titulo> selectedData = new ArrayList<>();
            for (int rowIndex : selectedRows) {
                String vencimento = (String) tableModel.getValueAt(rowIndex, 0);
                String documento = (String) tableModel.getValueAt(rowIndex, 1);
                String sacado = (String) tableModel.getValueAt(rowIndex, 2);
                BigDecimal valorNota = new BigDecimal(tableModel.getValueAt(rowIndex, 3).toString());
                BigDecimal juros = new BigDecimal(tableModel.getValueAt(rowIndex, 4).toString());
                BigDecimal tarifa = new BigDecimal(tableModel.getValueAt(rowIndex, 5).toString());
                BigDecimal iof = new BigDecimal(tableModel.getValueAt(rowIndex, 6).toString());
                BigDecimal valorLiquido = new BigDecimal(tableModel.getValueAt(rowIndex, 7).toString());

                Titulo titulo = new Titulo(vencimento, documento, sacado, valorNota, juros, tarifa, iof, valorLiquido);
                selectedData.add(titulo);
            }
            sendDataToCaller(selectedData);
        }
    }

    private List<Titulo> extractDataFromPDF(File file) throws IOException {
        List<Titulo> extractedData = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);

            // Process the text to extract relevant data
            String[] lines = text.split("\n");
            Titulo titulo = null;
            for (String line : lines) {
                if (line.startsWith("Vencimento")) {
                    if (titulo != null) {
                        extractedData.add(titulo);
                    }
                    String[] parts = line.split("\\s+", 5);
                    String vencimento = parts[1];
                    String parcela = parts[2];
                    String documento = parts[3];
                    String sacado = parts.length > 4 ? parts[4] : "";
                    titulo = new Titulo(vencimento, documento, sacado, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
                } else if (line.startsWith("Nosso Número")) {
                    // Skip this line
                } else if (line.startsWith("Valor da Nota")) {
                    String valorNotaStr = line.split(": R\\$ ")[1];
                    BigDecimal valorNota = new BigDecimal(valorNotaStr.replace(",", "."));
                    titulo = new Titulo(titulo.getVencimento(), titulo.getDocumento(), titulo.getSacado(), valorNota, titulo.getJuros(), titulo.getTarifa(), titulo.getIof(), titulo.getValorLiquido());
                } else if (line.startsWith("Juros")) {
                    String jurosStr = line.split(": R\\$ ")[1];
                    BigDecimal juros = new BigDecimal(jurosStr.replace(",", "."));
                    titulo = new Titulo(titulo.getVencimento(), titulo.getDocumento(), titulo.getSacado(), titulo.getValorNota(), juros, titulo.getTarifa(), titulo.getIof(), titulo.getValorLiquido());
                } else if (line.startsWith("Tarifa")) {
                    String tarifaStr = line.split(": R\\$ ")[1];
                    BigDecimal tarifa = new BigDecimal(tarifaStr.replace(",", "."));
                    titulo = new Titulo(titulo.getVencimento(), titulo.getDocumento(), titulo.getSacado(), titulo.getValorNota(), titulo.getJuros(), tarifa, titulo.getIof(), titulo.getValorLiquido());
                } else if (line.startsWith("IOF")) {
                    String iofStr = line.split(": R\\$ ")[1];
                    BigDecimal iof = new BigDecimal(iofStr.replace(",", "."));
                    titulo = new Titulo(titulo.getVencimento(), titulo.getDocumento(), titulo.getSacado(), titulo.getValorNota(), titulo.getJuros(), titulo.getTarifa(), iof, titulo.getValorLiquido());
                } else if (line.startsWith("Valor Líquido")) {
                    String valorLiquidoStr = line.split(": R\\$ ")[1];
                    BigDecimal valorLiquido = new BigDecimal(valorLiquidoStr.replace(",", "."));
                    titulo = new Titulo(titulo.getVencimento(), titulo.getDocumento(), titulo.getSacado(), titulo.getValorNota(), titulo.getJuros(), titulo.getTarifa(), titulo.getIof(), valorLiquido);
                }
            }
            if (titulo != null) {
                extractedData.add(titulo);
            }
        }
        return extractedData;
    }

    private void sendDataToCaller(List<Titulo> selectedData) {
        // Aqui você pode implementar o envio dos dados para a classe chamadora
        // Este é apenas um exemplo de como você pode fazer isso
        for (Titulo titulo : selectedData) {
            System.out.println("Enviando dados: " + titulo.getVencimento() + ", " + titulo.getDocumento() + ", " + titulo.getSacado() + ", " + titulo.getValorNota() + ", " + titulo.getJuros() + ", " + titulo.getTarifa() + ", " + titulo.getIof() + ", " + titulo.getValorLiquido());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BradescoPDFProcessor frame = new BradescoPDFProcessor();
            frame.setVisible(true);
        });
    }
}

class Titulo {
    private String vencimento;
    private String documento;
    private String sacado;
    private BigDecimal valorNota;
    private BigDecimal juros;
    private BigDecimal tarifa;
    private BigDecimal iof;
    private BigDecimal valorLiquido;

    // Construtor, getters e setters
    public Titulo(String vencimento, String documento, String sacado, BigDecimal valorNota, BigDecimal juros, BigDecimal tarifa, BigDecimal iof, BigDecimal valorLiquido) {
        this.vencimento = vencimento;
        this.documento = documento;
        this.sacado = sacado;
        this.valorNota = valorNota;
        this.juros = juros;
        this.tarifa = tarifa;
        this.iof = iof;
        this.valorLiquido = valorLiquido;
    }

    public String getVencimento() {
        return vencimento;
    }

    public String getDocumento() {
        return documento;
    }

    public String getSacado() {
        return sacado;
    }

    public BigDecimal getValorNota() {
        return valorNota;
    }

    public BigDecimal getJuros() {
        return juros;
    }

    public BigDecimal getTarifa() {
        return tarifa;
    }

    public BigDecimal getIof() {
        return iof;
    }

    public BigDecimal getValorLiquido() {
        return valorLiquido;
    }
}
