package com.factoring.pdf2csv.service;

import com.factoring.pdf2csv.model.BankEnum;

public class BankServiceFactory {

    public static BankService getService(BankEnum bank) {
        switch (bank) {
            case SB:
                return new SBService2();

            case HARPIA:
                return new HarpiaService2();
            case BFC:
                return new BFCService1();
            case JP:
                return new JPService2();
            case RG:
                return new RGService2();
            case SAFRA:
                return new SafraService();
            case ATF:
                return new ATFService();
            case BRADESCO:
                return new BradescoService();
            case MATRIZ:
                return new MatrizService();
            case ATHENA:
                return new AtlantaService2();
            case SARFATY:
                return new SarfatyService();
            case UNIQUEAAA:
                return new UniqueAAAService();
            case GARSON:
                return new GarsonService();
            case QTUNIQUE:
                return new QTUniqueService();
            case DANIELE:
                return new DanieleService();
            case FIDEM:
                return new FidemService();
            case MATRIZ2:
                return new MatrizService2();
            case BRADESCO2:
                return new BradescoService2();
            case ITAU:
                return new ItauService();
            case TRUST:
                return new TrustService();
            case CREDVALE:
                return new CredValeService();
            case LARCA:
                return new LarcaService();
            case MULTIPLICA:
                return new MultiplicaService();
            case RED:
                return new RedService();
            case INVISTA:
                return new InvistaService2();
            case BB:
                return new BBService();
            case BELAVISTA:
                return new BelaVistaService2();

            case OPERA:
                return new OperaService();
            case BANESTES:
                return new BanestesService();
            case CAPITAL:
                return new ViaCapitalService();

            case RNX:
                return new RNXService();

            case TRUST2:
                return new TrustService2();

            case OLAM:
                return new OlamService();

            case HARPIAF:
                return new HarpiaService3();

            case MARCAPITAL:
                return new MarcapitalService();

            case SOFISA:
                return new SofisaService();

            case SBSECURITIZADORA: {
                return new SBSecuritizadoraService();
            }
            case PREMIUM:{
                return new PremiumService();
            }

            default:
                throw new IllegalArgumentException("Banco não suportado: " + bank);
        }
    }
}
