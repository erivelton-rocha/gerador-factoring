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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 *
 * @autor ANALISTA_SISTEMA
 */
public class JPService extends AbstractBankService {

    @Override
    public String extractAndProcess(File txtFile, double totalValue, double totalPaid, double iofRate, double fee, int bankCode) {
        // Iniciar a interface gráfica de processamento
        startProcessingWindow();
        try {
            // Extrair dados do arquivo TXT e gerar um novo arquivo de texto para validação
            File reprocessedFile = new File(txtFile.getParent(), "dados_reprocessados.txt");
            reprocessTxt(txtFile, reprocessedFile);
            // Gerar o arquivo CSV
            File csvFile = new File(txtFile.getParent(), "dados_processados.csv");
            generateCsv(reprocessedFile, csvFile, totalValue, totalPaid, iofRate, fee, bankCode);
            // Fechar a interface gráfica de processamento,
            closeProcessingWindow();
            // Retornar o caminho do arquivo CSV gerado
            return csvFile.getAbsolutePath();
        } catch (IOException e) {
            String errorMessage = "Erro ao processar arquivo: " + e.getMessage();
            showError(errorMessage);
            return null;
        } catch (Exception e) {
            String errorMessage = "Erro inesperado: " + e.getMessage();
            showError(errorMessage);
            return null;
        }
    }

    private void reprocessTxt(File txtFile, File reprocessedFile) throws IOException {
        // Extrair dados do arquivo TXT e gerar um novo arquivo de texto para validação
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
        data = extractData(extractedText);
        return data;
    }

    private List<String[]> extractData(String extractedText) {
        List<String[]> data = new ArrayList<>();
        // Extrair dados do texto
        String[] lines = extractedText.split("\n");
        boolean isDataSection = false;
        for (String line : lines) {
            if (line.contains("Espécie Número Vencimento Valor Sacado")) {
                isDataSection = true;
                continue;
            }
            if (line.contains("Pela presente cessão")) {
                isDataSection = false;
                break;
            }
            if (isDataSection) {
                String[] part = line.split("[ ]+");
                // verifica se tem pelo menos 4 posições
                if (part.length >= 4) {
                    String documento = part[1].trim(); // documento
                    String vencimento = part[2].trim(); // vencimento
                    String valorBruto = part[3].trim();
                    StringBuilder nomeSacado = new StringBuilder();
                    for (int i = 4; i < part.length; i++) {
                        if (i > 4) {
                            nomeSacado.append(" ");
                        }
                        nomeSacado.append(part[i]);
                    }
                    String sacado = nomeSacado.toString().trim();
                    data.add(new String[]{documento, sacado, vencimento, valorBruto});
                }
            }
        }
        // Adicionar ponto de depuração
//        System.out.println("Dados extraídos: " + data.size() + " linhas");
        return data;
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

            for (String[] row : data) {
                try {
                    // Atualizar a interface gráfica a cada 10 linhas para melhorar o desempenho
                    if (rowCount % 10 == 0) {
                        updateProcessingWindow("Processando linha: " + rowCount);
                        // Atualizar a barra de progresso
                        setProgress((rowCount * 100) / totalRows);
                    }
                    rowCount++;

                    // Verificar se a linha está mal formatada
                    if (row.length != 4) {
//                        System.out.println("Linha mal formatada: " + String.join(" | ", row));
                        continue;
                    }

                    String documento = row[0];
                    String[] documentoParts = documento.split("-");
                    String titulo = documentoParts[0];
                  
                    String parcela = documentoParts.length > 1 ? documentoParts[1].substring(0, 2) : ""; // Corrigir a extração da parcela
                    String nossoNumero = "";
                    String contrato = "";
                    int codigoBanco = bankCode;

                    // Corrigir a conversão do valor
                    String valorStr = row[3];//
                    BigDecimal valorBruto = new BigDecimal(valorStr.replace(".", "").replace(",", "."));

                    BigDecimal iof = valorBruto.multiply(BigDecimal.valueOf(iofRate / 100));
                    BigDecimal tarifa = BigDecimal.valueOf(fee);

                    // Calcular a taxa de deságio
                    //double taxaDesagio = (totalValue - totalPaid) / totalValue;
                    // Calcular juros
                    BigDecimal taxaDesagio = BigDecimal.valueOf(totalValue)
                            .subtract(BigDecimal.valueOf(totalPaid))
                            .divide(BigDecimal.valueOf(totalValue), 10, RoundingMode.HALF_UP);

                    // Calcular juros
                    BigDecimal juros = valorBruto.multiply(taxaDesagio);

                    // Calcular o valor líquido
                    BigDecimal valorLiquido = valorBruto.subtract(iof).subtract(tarifa).subtract(juros);

                    // Exibir resultados
// Consultar no banco os dados: empresa, série e parcela (real)
                TituloInfo info = getTituloInfo(titulo, codigoBanco);

                String empresa = info != null ? String.valueOf(info.getCodigoEmpresa()) : "000";
                
                String serie = info != null ? info.getSerie() : "";

                    // Consultar o código da empresa no banco de dados
                
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

                    updateProcessingWindow("Linha processada: " + String.join(" | ", row));
                } catch (Exception e) {
//                    System.out.println("Erro ao processar linha: " + String.join(" | ", row));
//                    System.out.println("Detalhes do erro: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Atualizar a barra de progresso para 100% no final
            setProgress(100);
        } catch (IOException e) {
            throw new IOException("Erro ao gravar dados no arquivo CSV. " + e.getMessage());
        }
    }
    

    private boolean isHeaderOrFooter(String line) {
        // Implemente a lógica para identificar linhas de cabeçalho/rodapé
        line = line.trim();
        return line.equals("Número Nome do devedor Vencimento Valor no Vencimento")
                || line.contains("Direitos Creditórios")
                || line.matches("^.*abaixo:$")
                || line.isEmpty();
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

    @Override
    public String getEmpresa(String titulo) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
