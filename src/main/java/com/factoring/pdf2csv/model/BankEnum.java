package com.factoring.pdf2csv.model;

public enum BankEnum {
    

    ATF("ATF - 152", 152),
    HARPIA("HARPIA/AR3 - 161", 161),
    HARPIAF("HARPIA -161 (modelo fomento)", 161),
    BFC("BFC - 163", 163),
    JP("JP - 142",142),
    RG("R&G - 165", 165),
    SAFRA("Safra - 108",108),
    BANESTES("Banestes - 104",104),
    BRADESCO("Bradesco - 105 (HOMOLOGANDO)", 105),
    BRADESCO2("Bradesco - 105 contrato agrupado",105),
    ITAU("Itau - 107 contrato agrupado", 107),
    SBSECURITIZADORA("SB Securitizadora -157", 157),
    MATRIZ("Matriz - 158 (detalhamento aditivo)", 158),
    SB("SB - 159", 159),
    MATRIZ2("Matriz - 158 (versão contrato)", 158),
    ATHENA("ATHENA/ATLANTA -174", 174),
    SARFATY("MSOpen (Safarty) - 170",170),
    UNIQUEAAA("UniqueAAA/ SOMA - 154", 154),
    GARSON("Garson - 186", 186),
    QTUNIQUE("QT Unique - (Homologando) - 155", 155),
    BELAVISTA("Bela Vista - 162", 162),
    DANIELE("Daniele - (Homologando) - 166", 166),
    FIDEM("Fidem - (homologando - 167",167),
    TRUST("Trust - 190", 190),
    TRUST2("Trust 2 (modelo anexo 1)- 190",190),
    RED("Redfactor - 193",193),
    MULTIPLICA("Multiplica - 194", 194),
    BB("Banco do Brasil - 196", 196),
    LARCA("LARCA - 197", 197),
    CREDVALE("CREADVALE - 172",172),
    INVISTA("INVISTA - 200", 200),
    CAPITAL("Via Capital - 204", 204),
    PREMIUM("Premium - 205", 205),
    OLAM("OLAM - 206", 206),
    RNX("RNX FIDC - 208",208),
    OPERA("Opera Capital - 209", 209),
    MARCAPITAL("Marcapital - 211", 211),
    SOFISA("Sofisa - 213", 213);


    private final String displayName;
    private final int code;

    BankEnum(String displayName, int code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCode(){
        return code;
    }
    @Override
    public String toString() {
        return displayName;
    }
}
