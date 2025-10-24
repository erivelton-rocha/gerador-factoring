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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 *
 * @author ANALISTA_SISTEMA
 *
 *
 * Processo da versão com o contrato
 */
public class MatrizService2 extends AbstractBankService {

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

        // Ler o arquivo com codificação UTF-8
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        // Extrair as linhas relevantes
        List<String> relevantLines = extractRelevantData(lines);

        // Reprocessar os dados
        data = reprocessData(relevantLines);

        return data;
    }

    private List<String> extractRelevantData(List<String> lines) {
        List<String> relevantLines = new ArrayList<>();
        boolean capture = false;
        String startIndicator = "Titulo de Crédito Sacado( CPF/CNPJ Valor (R$) Vencimento";
        String endIndicator = "Total:";

        for (String line : lines) {
            line = line.replaceAll("[\\r\\n]", "").trim();
            System.out.println(line);
            if (line.equalsIgnoreCase(startIndicator)) {
                capture = true;
                continue; // Skip the start indicator line
            }
            if (line.isBlank() || line.isEmpty()) {
                continue;
            }
            if (line.startsWith("Página")) {
                continue;

            }
            if (line.contains(endIndicator)) {
                capture = false;
                continue; // Stop capturing when end indicator is found
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
            line = line.trim();

            // Processa apenas linhas que começam com o número do documento
            if (line.matches("^\\d{7}-\\d{2}.*")) {
                // Ignora linhas que são apenas CNPJ
                if (!line.matches("^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$")) {
                    data.add(parseRecord(line));
                }
            }
        }

        return data;
    }

    /**
     * Verifica se a linha contém pelo menos dois valores numéricos.
     */
    private boolean hasAtLeastTwoValues(String line) {
        String[] parts = line.split("\\s+");
        int valueCount = 0;
        for (String part : parts) {
            // Verifica se é um número (inteiro ou decimal com vírgula)
            if (part.matches("\\d+(,\\d+)?")) {
                valueCount++;
            }
        }
        return valueCount >= 2;
    }

    private String[] parseRecord(String record) {
        try {
            // Split the record into parts based on spaces
            String[] parts = record.split("\\s+");

            // Extrair título e parcela do primeiro elemento
            String[] documento = parts[0].split("-");
            String titulo = documento[0];
            String parcela = documento[1];

            // Encontrar o valor e a data (últimos elementos)
            String vencimento = parts[parts.length - 1]; // Data está no último elemento
            String valorBruto = parts[parts.length - 2]; // Valor está no penúltimo elemento
            String valorDesagio = "0.0";

            // Construir o nome do sacado (todo o conteúdo entre o número do documento e o valor)
            StringBuilder sacado = new StringBuilder();
            int startIndex = 1; // Começa após o número do documento
            int endIndex = parts.length - 2; // Termina antes do valor

            for (int i = startIndex; i < endIndex; i++) {
                if (!parts[i].matches(".*\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}.*")) { // Ignora o CNPJ
                    sacado.append(parts[i]).append(" ");
                }
            }

            // Remove possíveis traços extras e espaços em excesso
            String sacadoFinal = sacado.toString()
                    .replace(" - ", " ")
                    .trim();

            return new String[]{
                titulo,
                parcela,
                sacadoFinal,
                vencimento,
                valorDesagio,
                valorBruto // Converter para formato numérico
            };
        } catch (Exception e) {
            System.out.println("Erro ao processar registro: " + record);
            e.printStackTrace();
            return null;
        }
    }

    private void generateCsv(File reprocessedFile, File csvFile, double totalValue, double totalPaid, double iofRate, double fee, int bankCode) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Escrever cabeçalho do CSV
            writer.println("EMPRESA;DOCUMENTO;SERIE;PARCELA;NOSSO_NUMERO;CONTRATO;CODIGO_BANCO;VALOR_BRUTO;IOF;JUROS;TARIFA;VALOR_LIQUIDO");

            // valor da tarifa
            // o valor da tarifa sera um ratio pela quantidade de titulos
//            BigDecimal tarifa = new BigDecimal(fee).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tarifa = new BigDecimal(fee); // Ler dados do arquivo reprocessado e calcular valores
            BigDecimal totalPago = BigDecimal.valueOf(totalPaid);
            BigDecimal totalBruto = BigDecimal.valueOf(totalValue);

            List<String> lines = Files.readAllLines(reprocessedFile.toPath());
            List<String[]> data = new ArrayList<>();

            for (String line : lines) {
                String[] columns = line.split("\\|");
//                System.out.println(columns.length);
                if (columns.length == 6) {
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
                    if (row.length != 6) {
//                        System.out.println("Linha mal formatada: " + String.join(" | ", row));
                        continue;
                    }

                    String titulo = row[0];
                    String parcela = row[1];

                    String nossoNumero = "";
                    String contrato = "";

                    // Validar e converter valores numéricos
                    BigDecimal valorDoTitulo = new BigDecimal(row[5].replace(".", "").replace(",", "."));

                    // Calcular tarifa
                    BigDecimal totalRegistros = BigDecimal.valueOf(totalRows);

                    BigDecimal tarifaPorTitulo = BigDecimal.ZERO;
                    if (tarifa.compareTo(BigDecimal.ZERO) > 0) {

                        tarifaPorTitulo = tarifa.divide(totalRegistros, 15, RoundingMode.HALF_UP);
                    }

                    // Calcular o juros (desagio)
                    BigDecimal taxaDesagio = BigDecimal.ZERO;

                    taxaDesagio = totalBruto.subtract(totalPago)
                            .divide(totalBruto, 15, RoundingMode.HALF_UP);

                    BigDecimal juros = valorDoTitulo.multiply(taxaDesagio);

                    // @TODO - implementar futuramente
                    BigDecimal iof = BigDecimal.ZERO;

                    BigDecimal valorLiquido = valorDoTitulo
                            .subtract(juros)
                            .subtract(tarifaPorTitulo)
                            .subtract(iof);

                    int codigoBanco = bankCode;
                    // Consulta a empresa pelo numero do titulo
                    // Consultar no banco os dados: empresa, série e parcela (real)
                    TituloInfo info = getTituloInfo(titulo, codigoBanco);

                    String empresa = info != null ? String.valueOf(info.getCodigoEmpresa()) : "000";
                    
                    String serie = info != null ? info.getSerie() : "";
//                    // Escrever linha no CSV com formatação americana
//  
                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
                            empresa, titulo, serie, parcela, nossoNumero, contrato, codigoBanco,
                            valorDoTitulo,
                            iof,
                            juros,
                            tarifaPorTitulo,
                            valorLiquido);
                    rowCount++;
                    processedRows++;
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
