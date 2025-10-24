package com.factoring.pdf2csv.service;

import com.factoring.pdf2csv.view.ProcessingWindow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ANALISTA_SISTEMA
 */
public class JPService2 extends AbstractBankService {

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

    public void setProgress(int value) {
        if (this.processingWindow != null) {
            this.processingWindow.setProgress(value);
        }
    }

    @Override
    public void closeProcessingWindow() {
        if (this.processingWindow != null) {
            this.processingWindow.close();
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
        String startIndicator = "Documento Sacado Vencimento Valor(R$) Deságio R$";

        String endIndicator = "Qtd de Títulos:";


        for (String line : text.split("\\n")) {
            line = line.trim().replaceAll("\\t", " ").replaceAll("\\s+", " ");;
//            System.out.println(stringSemEspaco);
            if(line.equals("\\r") || line.equals("")){
                continue;
            }
            if (line.contains(startIndicator)) {
                capture = true;
                continue; // Skip the start indicator line
            }

            if(line.contains("Página")){
                continue;
            }
            if (!capture) {
                continue; // skip if not find start indicator
            }
            if (line.isBlank() || line.isEmpty()
                    || line.equals("R$")) {
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

        // Regex para capturar os componentes desejados
        String regex = "(\\d{6}-\\d{2})\\s+.*?\\s(\\d{2}/\\d{2}/\\d{4})\\s+([\\d.,]+)\\s+([\\d.,]+)";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

        // Concatenar linhas para capturar dados em várias linhas
        StringBuilder combinedLines = new StringBuilder();
        for (String line : lines) {
            combinedLines.append(line).append(" ");
        }

        Matcher matcher = pattern.matcher(combinedLines.toString());

        while (matcher.find()) {
            String[] match = new String[4];
            match[0] = matcher.group(1); // ID
            match[1] = matcher.group(2); // Data
            match[2] = matcher.group(3); // Valor bruto
            match[3] = matcher.group(4); // Valor desagio
            data.add(match);
        }

        return data;
    }

    private String[] parseRecord(String record) {
        Pattern pattern = Pattern.compile("^\\d{6}[-N]\\d{2}");
        Matcher matcher = pattern.matcher(record);

        if (matcher.matches()) {
            String documento = matcher.group(1); // Captura 6 dígitos, hífen ou N, e 2 dígitos (ex: 053175-04)
            String sacado = matcher.group(2);    // Captura o nome do sacado (qualquer coisa até a data)
            String vencimento = matcher.group(3); // Captura a data (ex: 27/08/25)
            String moeda = matcher.group(4);     // Captura a moeda (ex: R$)
            String valor = matcher.group(5);     // Captura o valor (ex: 956,97 ou 4.148,09)

            // ... seu código para processar os dados ...

            return new String[]{documento, sacado.trim(), vencimento, moeda, valor};
        } else {
            // Handle the case where the record does not match the expected pattern
            throw new IllegalArgumentException("Record does not match expected format: " + record);
        }
    }


    private void generateCsv(File reprocessedFile, File csvFile, double totalValue, double totalPaid, double iofRate, double fee, int bankCode) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Escrever cabeçalho do CSV
            writer.println("EMPRESA;DOCUMENTO;SERIE;PARCELA;NOSSO_NUMERO;CONTRATO;CODIGO_BANCO;VALOR_BRUTO;IOF;JUROS;TARIFA;VALOR_LIQUIDO");

            // Ler dados do arquivo reprocessado e calcular valores
            List<String> lines = Files.readAllLines(reprocessedFile.toPath(), StandardCharsets.UTF_8);
            List<String[]> data = new ArrayList<>();

            for (String line : lines) {
                String[] columns = line.split("\\|");
//                System.out.println(columns.length);
                if (columns.length == 4) {
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
//            System.out.println("Dados para gerar CSV: " + data.size() + " linhas");
            // Formatação de valores
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            DecimalFormat decimalFormat = new DecimalFormat("0.00", symbols);
            int rowCount = 0;
            int totalRows = data.size();
            int processedRows = 0;

            BigDecimal totalLinhas = new BigDecimal(data.size());
            BigDecimal tarifa = BigDecimal.valueOf(fee).divide(totalLinhas, 5,RoundingMode.HALF_UP);

            for (String[] row : data) {
                try {
                    // Atualizar a interface gráfica a cada 10 linhas para melhorar o desempenho
                    if (processedRows % 10 == 0) {

                        // Atualizar a barra de progresso
                        updateProcessingWindow("Processando linha: " + rowCount);
                        setProgress((processedRows * 100) / totalRows);
                    }

                    // Verificar se a linha está mal formatada
                    if (row.length != 4) {
//                        System.out.println("Linha mal formatada: " + String.join(" | ", row));
                        continue;
                    }

                    String documento = row[0];
                    String[] documentoParts = documento.split("N|-");

                    String titulo = documentoParts[0];
                    String parcela = documentoParts[1];

//                    String parcela = documentoParts.length > 1 ? documentoParts[1].substring(0, 2) : ""; // Corrigir a extração da parcela
                    String nossoNumero = "";
                    String contrato = "";
                    int codigoBanco = bankCode;

                    // Corrigir a conversão do valor
                    String valorStr = row[2];//
                    BigDecimal valorBruto = new BigDecimal(valorStr.replace(".", "").replace(",", "."));

                    BigDecimal iof = valorBruto.multiply(BigDecimal.valueOf(iofRate / 100));



                    // Calcular a taxa de deságio
                    //double taxaDesagio = (totalValue - totalPaid) / totalValue;
                    // Calcular juros

                    String desagio = row[3];
                    BigDecimal juros = new BigDecimal(desagio.replace(".","").replace(",","."));


                    // Calcular o valor líquido
                    BigDecimal valorLiquido = valorBruto.subtract(iof).subtract(tarifa).subtract(juros);

                    // Consultar no banco os dados: empresa, série e parcela (real)
                    TituloInfo info = getTituloInfo(titulo, codigoBanco);

                    String empresa = info != null ? String.valueOf(info.getCodigoEmpresa()) : "000";
                    String parcelaBanco = info != null ? info.getCodigoParcela() : "";
                    String serie = info != null ? info.getSerie() : "";

//                    // Escrever linha no CSV com formatação americana
//                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
//                            empresa, titulo, serie, parcela, nossoNumero, contrato, codigoBanco,
//                            decimalFormat.format(valorBruto), decimalFormat.format(iof), decimalFormat.format(juros), decimalFormat.format(tarifa), decimalFormat.format(valorLiquido));
                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
                            empresa, titulo, serie, parcela, nossoNumero, contrato, codigoBanco,
                            valorBruto,
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
            System.out.println("total de linhas: " + rowCount);
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
