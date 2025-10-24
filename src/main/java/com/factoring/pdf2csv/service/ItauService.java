/*
* Modelo de contrato desagrupado
*
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

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ANALISTA_SISTEMA
 */
public class ItauService extends AbstractBankService {

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
        String extractedText = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        List<String> relevantLines = extractRelevantData(extractedText);
        data = reprocessData(relevantLines);
        return data;

    }

    private List<String> extractRelevantData(String text) {
        List<String> relevantLines = new ArrayList<>();
        boolean capture = false;
        String startIndicator = "Nosso Número";
        //String endIndicator = "Dúvidas, sugestões e reclamações:";

        for (String line : text.split("[\n\r]")) {
            line = line.trim();
            System.out.println("linha a processar: " + line);

            // Skip empty lines and irrelevant markers
            if (line.isBlank() || line.isEmpty()) {
                continue;
            }
            // ignora link do banco
            if (line.contains("Dúvidas, sugestões e reclamações:")
                    || line.contains("www.itau.com.br")
                    || line.contains("ou Caixa Postal")
                    || line.contains("Ouvidoria Corporativa")
                    || line.contains("ou Caixa Postal 67.600")
                    || line.contains("(todos os dias, 24h")) {
                continue;
            }
            // Check for the start indicator
            if (line.equals(startIndicator)) {
                capture = true;
                continue; // Skip the start indicator line
            }

//            // Check for the end indicator
//            if (line.contains(endIndicator)) {
//                capture = false;
//                continue;
//            }
            // Capture relevant lines
            if (capture) {
                System.out.println("☑ ️processou a linha: " + line);
                relevantLines.add(line);
            }
        }
        return relevantLines;
    }

    private List<String[]> reprocessData(List<String> lines) {
        List<String[]> data = new ArrayList<>();

        // Regex agora aceita CPF ou CNPJ
        String regex = "^(.*?)\\s*((?:\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})|(?:\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}))\\s*"
                + "(\\d{1,3}(\\.\\d{3})*,\\d{2})\\s*"
                + "(\\d{2}/\\d{2}/\\d{4})\\s*"
                + "(\\d+)$";

        Pattern pattern = Pattern.compile(regex);

        for (String line : lines) {
            line = line.trim();

            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                // Extrair partes desejadas:
                String nome = matcher.group(1).trim(); // Nome ou vazio
                String cnpj = matcher.group(2).trim();
                String valor = matcher.group(3).trim();
                String datavenc = matcher.group(5).trim();
                String numeroSequencial = matcher.group(6).trim();

                // Adiciona ao resultado
                data.add(new String[]{nome, cnpj, valor, datavenc, numeroSequencial});
            }
        }

        return data;
    }
//    /**
//     * Verifica se a linha contém pelo menos dois valores numéricos.
//     */
//    private boolean hasAtLeastTwoValues(String line) {
//        String[] parts = line.split("\\s+");
//        int valueCount = 0;
//        for (String part : parts) {
//            // Verifica se é um número (inteiro ou decimal com vírgula)
//            if (part.matches("\\d{1,3}(\\.\\d{3})*(,\\d{2})?")) {
//                valueCount++;
//            }
//        }
//        return valueCount >= 1;
//    }
//
//    private String[] parseRecord(String record) {
//        // Split the record into parts based on spaces
//        String[] parts = record.split("\\s+");
//
//        // Determine the indices of the different parts of the record
//        String vencimento = parts[0];
//        //String parcela = parts[1];
//        String texto = parts[2];
////        String[] documentoparts = parts[2].split("-|N|D");
//        String numeroDuplicata ="";
//        String parcela = "";
//        String serie = "";
//
//        // Encontra a posição do primeiro caractere não numérico
//        int posicaoSeparador = -1;
//        for (int i = 0; i < texto.length(); i++) {
//            if (!Character.isDigit(texto.charAt(i))) {
//                posicaoSeparador = i;
//                serie = String.valueOf(texto.charAt(i));
//                break;
//            }
//        }
//
//        if (posicaoSeparador != -1) {
//            numeroDuplicata = texto.substring(0, posicaoSeparador);
//            parcela = texto.substring(posicaoSeparador + 1);
//        }
//
//        String sacado = "";
//        int i = 3;
//        while (!parts[i].equals("Nosso")) {
//            sacado += parts[i] + " ";
//            i++;
//        }
//        sacado = sacado.trim();
//
//        String nossoNumero = parts[i + 2];
//        String valorNota = parts[i + 7];
//        String juros = parts[i + 10];
//        String tarifa = parts[i + 13];
//        String iof = parts[i + 16];
//        String valorLiquido = parts[i + 20];
//
//        return new String[]{numeroDuplicata,parcela, serie ,vencimento, sacado, nossoNumero, valorNota, juros, tarifa, iof, valorLiquido};
//    }

    private void generateCsv(File reprocessedFile, File csvFile, double totalValue, double totalPaid, double iofRate, double fee, int bankCode) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Escrever cabeçalho do CSV
            writer.println("EMPRESA;DOCUMENTO;SERIE;PARCELA;NOSSO_NUMERO;CONTRATO;CODIGO_BANCO;VALOR_BRUTO;IOF;JUROS;TARIFA;VALOR_LIQUIDO");

            //BigDecimal iof = new BigDecimal(iofRate).setScale(2, RoundingMode.HALF_UP);
            // Ler dados do arquivo reprocessado e calcular valores
            List<String> lines = Files.readAllLines(reprocessedFile.toPath(), StandardCharsets.UTF_8);
            List<String[]> data = new ArrayList<>();

            for (String line : lines) {
                String[] columns = line.split("\\|");
//                System.out.println(columns.length);
                if (columns.length == 5) {
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

            //
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
                    if (row.length != 5) {
//                        System.out.println("Linha mal formatada: " + String.join(" | ", row));
                        continue;
                    }

                    String sequencial = row[4];
                    // Consultar no banco os dados: empresa, série e parcela (real)
                    TituloInfo info = getTituloByNbco(sequencial, bankCode);

                    String empresa = info != null ? String.valueOf(info.getCodigoEmpresa()) : "000";
                    String titulo = info != null ? info.getTitulo() : "000";
                    String parcela = info != null ? info.getCodigoParcela() : "000";

                    String serie = info != null ? info.getSerie() : "000";
                    String nossoNumero = sequencial;
                    String contrato = "";
                    int codigoBanco = bankCode;

                    // valores
                    double valorTitulo = Double.parseDouble(row[2].replace(".", "").replace(",", "."));
                    double tarifa = 0.0;
                    double valorLiquido = 0.0;

                    double iof = 0.0;

                    if (iofRate > 0.0) {
                        iof = iofRate / totalRows;
                    }

                    if (fee > 0.0) {
                        tarifa = fee / totalRows;

                    }
                    double desagio = (totalValue - totalPaid) - fee - iofRate;
                    double taxaDesagio = desagio / totalValue;
                    double juros = (valorTitulo * taxaDesagio);
                    //double jurosLiquido = juros - iof - tarifa;

                    valorLiquido = valorTitulo - juros - iof - tarifa;
//                  
                    System.out.println("taxa desagio: " + taxaDesagio);
//                    // Escrever linha no CSV com formatação americana
//                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
//                            empresa, titulo, serie, parcela, nossoNumero, contrato, codigoBanco,
//                            decimalFormat.format(valorBruto), decimalFormat.format(iof), decimalFormat.format(juros), decimalFormat.format(tarifa), decimalFormat.format(valorLiquido));
                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
                            empresa, titulo, serie, parcela, nossoNumero, contrato, codigoBanco,
                            valorTitulo,
                            iof,
                            juros,
                            tarifa,
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
