/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.factoring.pdf2csv.service;

import com.factoring.pdf2csv.view.ProcessingWindow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ANALISTA_SISTEMA
 */
public class BFCService1 extends AbstractBankService {

    /**
     * @param txtFile
     * @param totalValue
     * @param totalPaid
     * @param iofRate
     * @param fee
     * @param bankCode
     * @return
     */
    @Override
    public String extractAndProcess(File txtFile, double totalValue, double totalPaid, double iofRate, double fee, int bankCode) {
        //Iniciar a interface gráfica de processamento
        startProcessingWindow();
        try {
            // Extrair dados do arquivo TXT e gerar um novo arquivo de texto para validação
            File reprocessedFile = new File(txtFile.getParent(), "dados_reprocessados.txt");
            reprocessTxt(txtFile, reprocessedFile);

            // Gerar o arquivo CSV
            File csvFile = new File(txtFile.getParent(), "dados_processados.csv");
            generateCsv(reprocessedFile, csvFile, totalValue, totalPaid, iofRate, fee, bankCode);
            // Fechar a interface gráfica de processamento,
            try {

                Thread.sleep(5000);
                closeProcessingWindow();
            } catch (InterruptedException e) {
                // Trata a exceção caso a thread seja interrompida
                System.out.println("A thread foi interrompida!");
            }
            // Retornar o caminho do arquivo CSV gerado
            return csvFile.getAbsolutePath();

        } catch (IOException e) {
        }
        return null;
    }

    @Override
    public void startProcessingWindow() {
        this.processingWindow = new ProcessingWindow();
    }

    @Override
    public void updateProcessingWindow(String message) {
        if (this.processingWindow != null) {
            this.processingWindow.appendText(message);
        }
    }

    @Override
    public void closeProcessingWindow() {
        if (this.processingWindow != null) {
            this.processingWindow.close();
        }
    }

    public void setProgress(int value) {
        if (this.processingWindow != null) {
            this.processingWindow.setProgress(value);
        }
    }

    private void showError(String message) {
        if (this.processingWindow != null) {
            this.processingWindow.showError(message);
        }
    }

    private void reprocessTxt(File txtFile, File reprocessedFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(reprocessedFile))) {
            List<String[]> data = extractDataFromFile(txtFile);
            for (String[] row : data) {
                writer.println(String.join(" | ", row));
            }
            // Adicionar ponto de depuração
//            System.out.println("Dados reprocessados: " + data.size() + " linhas");
            writer.close();
        }
    }

    private List<String[]> extractDataFromFile(File file) throws IOException {
        List<String[]> data = new ArrayList<>();
        String extractedText = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        List<String> relevantLines = extractRelevantData(extractedText);
        data = reprocessData(relevantLines);
        return data;

    }

    private List<String> extractRelevantData(String text) {
        List<String> relevantLines = new ArrayList<>();
        boolean capture = false;

        String endIndicator = "Total dos Títulos Qtd.";
        // String endIndicator = "BFC Títulos Não Negociados CONTROLE BFC";

        for (String line : text.split("\n")) {

            boolean header1 = line.contains("ID.Título") || line.contains("ID.Titulo");
            boolean header2 = line.contains("Dt.Venc.") || line.contains("DtVenc");
            boolean header3 = line.contains("Sacado");

            if (header1 && header2 && header3) {
                capture = true;
                continue; // Skip the start indicator line
            }
            if (line.isBlank() || line.isEmpty()) {
                continue;
            }


            if (line.contains(endIndicator)) {
                capture = false;
                break; // Stop capturing when end indicator is found
            }
            if (capture) {
                relevantLines.add(line);
            }
        }
        return relevantLines;
    }

    private List<String[]> reprocessData(List<String> lines) {
        List<String[]> data = new ArrayList<>();

        for (String line : lines) {
            // Remove espaços múltiplos e quebra a linha em partes
            String cleanedLine = line.trim().replaceAll("\\s+", " ");
            String[] parts = cleanedLine.split(" ");

            if (parts.length >= 7) { // Verifica se há partes suficientes
                // Captura o valor na posição 3 e remove "R$" e espaços

                String value = parts[3].replace("R$", "").trim();
                String documentParcel = parts[5]; // O documento e a parcela estão na posição 5

                // Separa o documento da parcela
                String document = documentParcel.split("-")[0]; // Documento
                String parcel = documentParcel.split("-").length > 1 ? documentParcel.split("-")[1] : ""; // Parcela, se existir

                data.add(new String[]{
                        document, // Documento
                        parcel,   // Parcela
                        value     // Valor
                });
            } else {
                System.out.println("Linha não possui partes suficientes: " + cleanedLine);
            }
        }

        return data;
    }

    private void generateCsv(File reprocessedFile, File csvFile, double totalValue, double totalPaid, double iofRate, double fee, int bankCode) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {

            // Cabeçalho
            writer.println("EMPRESA;DOCUMENTO;SERIE;PARCELA;NOSSO_NUMERO;CONTRATO;CODIGO_BANCO;VALOR_BRUTO;IOF;JUROS;TARIFA;VALOR_LIQUIDO");

            List<String> lines = readAllLinesWithDifferentEncodings(reprocessedFile);
            List<String[]> data = new ArrayList<>();

            // Pegar os valores da tela
            BigDecimal ValorTotalBruto = BigDecimal.valueOf(totalValue);
            BigDecimal ValorTotalPago = BigDecimal.valueOf(totalPaid);
            BigDecimal taxaDesagio = ValorTotalBruto.subtract(ValorTotalPago).divide(ValorTotalBruto, 10, RoundingMode.HALF_UP);

            for (String line : lines) {
                String[] columns = line.split("\\|");
                if (columns.length == 3) {
                    for (int i = 0; i < columns.length; i++) {
                        columns[i] = columns[i].trim();
                    }
                    data.add(columns);
                } else {
                    String[] errorLine = new String[]{line + " - linha mal formatada"};
                    data.add(errorLine);
                }
            }

            int rowCount = 0;
            int totalRows = data.size();
            int processedRows = 0;

            for (String[] row : data) {
                try {
                    if (processedRows % 10 == 0) {
                        updateProcessingWindow("Processando linha: " + rowCount);
                        setProgress((processedRows * 100) / totalRows);
                    }

                    if (row.length != 3) {
                        continue;
                    }


                    String titulo = row[0];
                    String parcela = row[1];

                    String nossoNumero = "";
                    String contrato = "";
                    int codigoBanco = bankCode;

                    String valorStr = sanitizeValue(row[2]);
                    BigDecimal valorBruto =
                            new BigDecimal(valorStr.replace(".", "").replace(",", "."));


                    BigDecimal iof = BigDecimal.valueOf(iofRate);
                    BigDecimal tarifa = BigDecimal.valueOf(fee);

                    BigDecimal iofTitulo = BigDecimal.ZERO;
                    BigDecimal tarifaTitulo = BigDecimal.ZERO;

                    if (iof.compareTo(BigDecimal.ZERO) > 0) {
                        iofTitulo = iof.divide(BigDecimal.valueOf(totalRows), RoundingMode.HALF_UP);
                    }

                    if (tarifa.compareTo(BigDecimal.ZERO) > 0) {
                        tarifaTitulo = tarifa.divide(BigDecimal.valueOf(totalRows), RoundingMode.HALF_UP);
                    }


                    BigDecimal juros = valorBruto.multiply(taxaDesagio);
                    BigDecimal valorLiquido = valorBruto.subtract(iofTitulo).subtract(tarifaTitulo).subtract(juros);

                    System.out.println("taxa desagio: " + taxaDesagio);
                    // Consultar no banco os dados: empresa, série e parcela (real)
                    TituloInfo info = getTituloInfo(titulo, codigoBanco);

                    String empresa = info != null ? String.valueOf(info.getCodigoEmpresa()) : "000";

                    String serie = info != null ? info.getSerie() : "";

                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
                            empresa, titulo, serie, parcela, nossoNumero, contrato, codigoBanco,
                            valorBruto, iofTitulo, juros, tarifaTitulo, valorLiquido);

                    rowCount++;
                    processedRows++;
                    updateProcessingWindow("Linha processada: " + String.join(" | ", row));
                } catch (Exception e) {
                    System.out.println("Erro ao processar linha: " + String.join(" | ", row));
                    System.out.println("Detalhes do erro: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (processedRows == rowCount) {
                setProgress(100);
            } else {
                showError("Erro ao processar todas as linhas: " + processedRows + "/" + rowCount);
            }
        } catch (IOException e) {
            System.out.println("Erro ao gerar arquivo CSV a partir do reprocessado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> readAllLinesWithDifferentEncodings(File file) throws IOException {
        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            System.out.println("Erro ao ler arquivo com UTF-8, tentando ISO-8859-1");
            try {
                lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
            } catch (MalformedInputException ex) {
                System.out.println("Erro ao ler arquivo com ISO-8859-1, tentando Windows-1252");
                try {
                    lines = Files.readAllLines(file.toPath(), Charset.forName("Windows-1252"));
                } catch (MalformedInputException exc) {
                    System.out.println("Erro ao ler arquivo com todos os encodings tentados");
                    throw new IOException("Falha ao ler arquivo com todos os encodings tentados", exc);
                }
            }
        }
        return lines;
    }

    private String sanitizeValue(String value) {
        // Remove todos os espaços em branco
        value = value.replaceAll("\\s+", "");
        // Remove outros caracteres indesejados (se necessário, adicione mais caracteres aqui)
        value = value.replaceAll("[^\\d,\\.]", "");
        // Substitui vírgulas por pontos para garantir a formatação correta

        return value;
    }

    @Override
    public String getEmpresa(String titulo) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
