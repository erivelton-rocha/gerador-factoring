/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.factoring.pdf2csv.service;

import com.factoring.pdf2csv.view.ProcessingWindow;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ANALISTA_SISTEMA
 * 
 * 
 * 
 * Processo coma  versão do datalhemento do aditivo
 */
public class MatrizService extends AbstractBankService {

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

        } catch (Exception e) {
            e.printStackTrace();
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

        }
    }

    private List<String[]> extractDataFromFile(File file) throws IOException {
        List<String[]> data = new ArrayList<>();
        String extractedText = new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
        List<String> relevantLines = extractRelevantData(extractedText);
        data = reprocessData(relevantLines);
        return data;

    }

    private List<String> extractRelevantData(String text) {
        List<String> relevantLines = new ArrayList<>();
        String[] secaoSacados = text.split("Sacado:");

        for (String secao : secaoSacados) {
            if (secao.trim().isEmpty()) {
                continue;
            }

            String[] linhas = secao.split("\n");
            String sacado = linhas[0].trim();

            for (int i = 1; i < linhas.length; i++) {
                String linha = linhas[i].trim();
                if (linha.startsWith("Título")) {
                    continue;
                }
                if (linha.startsWith("Qtd Aditivo:")) {
                    break;
                }
                String[] partes = linha.split("\\s+");
                if (partes.length >= 14) {
                    String newText = linha + " " + sacado;
                    relevantLines.add(newText);
                }

            }

        }

        return relevantLines;
    }

    private List<String[]> reprocessData(List<String> lines) {
        List<String[]> data = new ArrayList<>();

        for (String line : lines) {
            // Remove leading and trailing spaces
            line = line.trim();
            data.add(parseRecord(line));

        }

        return data;
    }

    private String[] parseRecord(String record) {
        // Split the record into parts based on spaces
        // Adjust the split logic based on the actual format of the records
        String[] parts = record.split("\\s+");

        // Obtem tamanho da String
        int tamanho = parts[0].length();
        String[] documento = parts[0].split("-");

        // Obtem a parcela
        String parcela = parts[0].substring(tamanho - 2);
        // Obtem o titulo
        String titulo = documento[0];

        String vencimento = parts[1];

        String strValorBruto = parts[6];
        String strValorLiquido = parts[13];
        String strJuros = parts[7];
        String strTarifa = parts[9];
        String strIof = parts[10];

        // Combine the remaining parts as the "sacado"
        StringBuilder sacado = new StringBuilder();
        for (int i = 14; i < parts.length; i++) {
            sacado.append(parts[i]).append(" ");
        }

        return new String[]{titulo, parcela, sacado.toString().trim(), vencimento, strValorBruto, strValorLiquido, strJuros, strTarifa, strIof};
    }

    private void generateCsv(File reprocessedFile, File csvFile, double totalValue, double totalPaid, double iofRate, double fee, int bankCode) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Escrever cabeçalho do CSV
            writer.println("EMPRESA;DOCUMENTO;SERIE;PARCELA;NOSSO_NUMERO;CONTRATO;CODIGO_BANCO;VALOR_BRUTO;IOF;JUROS;TARIFA;VALOR_LIQUIDO");

            // Ler dados do arquivo reprocessado e calcular valores
            List<String> lines = Files.readAllLines(reprocessedFile.toPath(), StandardCharsets.UTF_8);
            List<String[]> data = new ArrayList<>();
            System.out.println("Lendo arquivo reprossados.csv \nTotal de linhas: " + lines.size());
            for (String line : lines) {
                String[] columns = line.split("\\|");
//                System.out.println(columns.length);
                if (columns.length == 9) {
                    for (int i = 0; i < columns.length; i++) {
                        columns[i] = columns[i].trim();
                    }
                    data.add(columns);
                } else {
                    // Adicionar ponto de depuração para linhas mal formatadas
                    String[] errorLine = new String[]{line + " - linha mal formatada"};
                    data.add(errorLine);
//                    System.out.println("Linha mal formatada: " + line);
                }
            }
            // Adicionar ponto de depuração
            //System.out.println("Dados para gerar CSV: " + data.size() + " linhas");
            // Formatação de valores
//            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
//            DecimalFormat decimalFormat = new DecimalFormat("0.00", symbols);

//            BigDecimal totalPago = BigDecimal.valueOf(totalPaid);
//            BigDecimal totalBruto = BigDecimal.valueOf(totalValue);
//
//            BigDecimal valorTotalIof = BigDecimal.valueOf(iofRate); // Supondo que valorTotalIof é o valor total de IOF
//            BigDecimal tarifaTotal = BigDecimal.valueOf(fee);
            int rowCount = 0;
            int totalRows = data.size();
            int processedRows = 0;

            for (String[] row : data) {
                try {
                    // Atualizar a interface gráfica a cada 10 linhas para melhorar o desempenho
                    if (processedRows % 10 == 0) {

                        // Atualizar a barra de progresso
                        updateProcessingWindow("Processando linha: " + rowCount);
                        setProgress((processedRows * 100) / totalRows);
                    }

                    // Verificar se a linha está mal formatada
                    if (row.length != 9) {
//                        System.out.println("Linha mal formatada: " + String.join(" | ", row));
                        continue;
                    }

                    String titulo = row[0];
                    String parcela = row[1];
      
//            
                    String nossoNumero = "";
                    String contrato = "";
                    int codigoBanco = bankCode;

                    // Consultar no banco os dados: empresa, série e parcela (real)
                TituloInfo info = getTituloInfo(titulo, codigoBanco);

                String empresa = info != null ? String.valueOf(info.getCodigoEmpresa()) : "000";
             
                String serie = info != null ? info.getSerie() : "";
                    
                    // Corrigir a conversão do valor
                    BigDecimal valorBruto = new BigDecimal(row[4].replace(".", "").replace(",", "."));
                    BigDecimal valorLiquido = new BigDecimal(row[5].replace(".", "").replace(",", "."));
                    BigDecimal jurosLiquido = new BigDecimal(row[6].replace(".", "").replace(",", "."));
                    BigDecimal valorTarifa = new BigDecimal(row[7].replace(".", "").replace(",", "."));
                    BigDecimal valorIof = new BigDecimal(row[8].replace(".", "").replace(",", "."));


//                    // Escrever linha no CSV com formatação americana
//                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
//                            empresa, titulo, serie, parcela, nossoNumero, contrato, codigoBanco,
//                            decimalFormat.format(valorBruto), decimalFormat.format(iof), decimalFormat.format(juros), decimalFormat.format(tarifa), decimalFormat.format(valorLiquido));
                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
                            empresa, titulo, serie, parcela, nossoNumero, contrato, codigoBanco,
                            valorBruto,
                            valorIof,
                            jurosLiquido,
                            valorTarifa,
                            valorLiquido);
                    rowCount++;
                    processedRows++;
                    System.out.printf("Convertendo:%s-%s:%s - linha processada: %s\n", titulo, parcela, valorBruto, rowCount);
                    updateProcessingWindow("Linha processada: " + String.join(" | ", row));
                } catch (Exception e) {
                    System.out.println("Erro ao processar linha: " + String.join(" | ", row));
                    System.out.println("Detalhes do erro: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Atualizar a barra de progresso para 100% no final
            if (processedRows == rowCount) {

                setProgress(100);
            } else {
                showError("Erro ao proprocessar todas as linhas: " + processedRows + "/" + rowCount);
            }
        } catch (IOException e) {
            throw new IOException("Erro ao gravar dados no arquivo CSV. " + e.getMessage());
        }
    }

    @Override
    public String getEmpresa(String titulo) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
