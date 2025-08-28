package com.grupoproyeccion.play.modules.davivienda_account;

import com.grupoproyeccion.play.model.AccountBancolombia;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DaviviendaAccountService {

    private static final List<String> KNOWN_BRANCHES;

    static {
         List<String> branches = Arrays.asList(
            "Compras y Pagos PSE",
            "PORTAL PYMES INTERBANCARI",
            "PORTAL PYMES",
            "BTA PROCESOS ESP.",
            "BTA PROCESOS ESP",
            "0000",
            "PROCESOS ACH","ABEJORRAL", "AMALFI", "AMAGA", "ANDES", "APARTADO", "BELLO", "CENCOSUD METRO BELLO",
            "PUERTA DEL NORTE", "BELLO PARQUE FABRICATO", "BETULIA", "CALDAS", "CAUCASIA",
            "CIUDAD BOLIVAR", "CONCORDIA", "COPACABANA", "EL SANTUARIO ANTIOQUIA", "ENVIGADO",
            "CITI PLAZA ENVIGADO", "VIVA ENVIGADO", "FREDONIA", "ITAGUI", "CENTRAL MAYORISTA",
            "PLAZA ARRAYANES", "JARDIN", "JERICO", "LA CEJA", "VIVA LA CEJA", "LA ESTRELLA",
            "MARINILLA", "MILLA DE ORO CORPORATIVA Y EMPRESARIAL", "MEDELLIN CENTRO", "AYACUCHO",
            "BELEN", "BOLIVAR", "CALASANZ", "CARACAS", "EL POBLADO", "EXITO COLOMBIA", "MILLA DE ORO",
            "SANTA TERESITA", "LA SETENTA", "LA ESTACION", "AVENTURA", "FLORIDA PARQUE", "LOS MOLINOS",
            "MONTERREY", "OVIEDO", "PREMIUM PLAZA", "SAN DIEGO", "SAN LUCAS", "SANTAFE MEDELLIN",
            "EL TESORO", "UNICENTRO (MEDELLIN)", "VIVA LAURELES", "LA CENTRAL", "TERMINAL SUR",
            "TERMINAL DEL NORTE", "PUERTO BERRIO", "RIONEGRO ANTIOQUIA", "SAN NICOLAS RIONEGRO",
            "AVES MARIA", "MAYORCA", "SALGAR", "SANTA BARBARA ANTIOQUIA", "SONSON", "TAMESIS",
            "VENECIA", "YARUMAL", "ARAUCA", "SARAVENA", "TAME", "BARRANQUILLA CORPORATIVA Y EMPRESARIAL",
            "ALTOS DEL PRADO", "AMERICANO", "VIVA BARRANQUILLA", "BUENAVISTA II", "CALLE 82",
            "CALLE 76", "CALLE 93", "CARRERA 43", "EL PRADO", "PARQUE WASHINGTON", "PASEO BOLIVAR",
            "PLAZA NORTE", "PLAZA DEL PARQUE", "PRINCIPAL BARRANQUILLA", "SEMINARIO",
            "UNIVERSIDAD DEL ATLANTICO", "VIA 40", "VILLA CAMPESTRE", "SOLEDAD", "CCI CORPORATIVA Y EMPRESARIAL",
            "ZONA INDUSTRIAL CORPORATIVA Y EMPRESARIAL", "ALAMOS DIVER PLAZA", "ALMIRANTE",
            "AUTOPISTA NORTE", "AVIANCA", "AVENIDA CHILE", "AVENIDA PEPE SIERRA", "AVENIDA ROJAS",
            "BARRIO RESTREPO", "BULEVAR NIZA", "LA CABRERA", "CALLE 80", "CALLE 100", "CALLE 19",
            "CALLE 94", "CAN", "CANTON NORTE", "CARRERA DECIMA", "CASTILLA", "C.C.I", "CEDRITOS",
            "CENTRO ANDINO", "CENTRO MAYOR", "CENTRO INTERNACIONAL", "CHAPINERO", "CHAPINORTE", "CHICO",
            "CHICO RESERVADO", "CIUDAD MONTES", "CONNECTA 26", "CONTADOR", "CORABASTOS", "CORFERIAS",
            "DORADO PLAZA", "EL LAGO", "EL RICAURTE", "EL RETIRO", "LA ESMERALDA", "ESPECTADOR",
            "FATIMA‐VENECIA", "FEDERACION DE CAFETEROS", "LA FLORESTA", "FONTIBON", "GAITAN", "GALERIAS",
            "GOBERNACION", "GRAN AMERICA", "GRAN ESTACION", "HACIENDA", "HAYUELOS", "ILARCO",
            "ISERRA CALLE 100", "JAVERIANA", "KENNEDY", "LA ENERGIA", "LAS FERIAS", "LOURDES",
            "LA MAGDALENA", "MARLY", "MINUTO DE DIOS", "MODELIA", "MULTIPLAZA LA FELICIDAD",
            "MUSEO DEL ORO", "NIZA", "NORMANDIA", "NORTH POINT", "OFICINA PRINCIPAL", "ORQUIDEAS",
            "PABLO VI", "PALATINO", "PARALELO 108", "PARQUE COLINA", "PASEO SAN RAFAEL", "PLAZA BOLIVAR",
            "PLAZA DE LAS AMERICAS", "PLAZA CENTRAL", "PLAZA CLARO", "PLAZA IMPERIAL", "PORCIUNCULA",
            "PORTAL DE LA 80", "PORTOALEGRE", "PRADO VERANIEGO", "QUINTA PAREDES", "QUIRIGUA",
            "SALITRE PLAZA", "SAN PATRICIO", "SAN MARTIN", "SANTA ISABEL", "SANTA BARBARA",
            "SANTA ANA", "SANTAFE", "SANTA MARIA DE LOS ANGELES", "SANTA PAULA", "SEVILLANA",
            "SIETE DE AGOSTO", "LA SOLEDAD", "SUBA", "TELEPORT BUSINESS", "TERMINAL BOGOTA",
            "TINTAL PLAZA", "TITAN PLAZA", "TOBERIN", "TORRE CENTRAL", "TRINIDAD‐GALAN", "TUNAL",
            "UNICENTRO", "UNICENTRO 2", "UNICENTRO 3", "UNICENTRO DE OCCIDENTE", "VEINTE DE JULIO",
            "WORLD TRADE CENTER", "ZONA INDUSTRIAL", "ZONA FRANCA", "LA PLAZUELA", "PRINCIPAL CARTAGENA",
            "BAZURTO", "BOCAGRANDE PLAZA", "CARIBE PLAZA", "CENTENARIO", "GETSEMANI", "LA MATUNA",
            "MALL PLAZA EL CASTILLO", "MANGA", "PASEO LA CASTELLANA", "RONDA REAL", "CARMEN DE BOLIVAR",
            "EXITO MAGANGUE", "TURBACO", "CHIQUINQUIRA", "DUITAMA", "INNOVO", "MIRAFLORES BOYACA",
            "MONIQUIRA", "ACERIAS PAZ DEL RIO", "PAIPA", "SAMACA", "SOGAMOSO", "IWOKA",
            "PRINCIPAL TUNJA", "PLAZA REAL", "UNICENTRO TUNJA", "VIVA TUNJA", "VILLA DE LEYVA",
            "AGUADAS", "ANSERMA", "ARANZAZU", "CHINCHINA", "FILADELFIA", "LA DORADA", "PRINCIPAL MANIZALES",
            "AV SANTANDER", "CIUDAD UNIVERSITARIA", "EL CABLE", "FUNDADORES MANIZALES", "MALL PLAZA MANIZALES",
            "MANIZALES CENTRO", "PARQUE CALDAS", "TERMINAL DE MANIZALES", "MANZANARES", "NEIRA", "PACORA",
            "PALESTINA", "PENSILVANIA", "RIOSUCIO", "SALAMINA", "SUPIA", "VILLAMARIA", "FLORENCIA",
            "GRAN PLAZA FLORENCIA", "FUERTE MILITAR LARANDIA", "AGUAZUL", "TAURAMENA", "YOPAL",
            "ALCARAVAN", "EL YOPO", "COLONIAL", "POPAYAN", "CAMPANARIO", "TERRA PLAZA",
            "SANTANDER DE QUILICHAO", "AGUACHICA", "CODAZZI", "GAMARRA", "VALLEDUPAR", "GUATAPURI",
            "LOPERENA", "MAYALES PLAZA", "CERETE", "LORICA", "PRINCIPAL MONTERIA", "ALAMEDAS",
            "BUENAVISTA MONTERIA", "NUESTRO MONTERIA", "ANAPOIMA", "CAJICA", "CAQUEZA", "CHIA",
            "CENTRO CHIA", "FONTANAR CHIA", "COTA", "TERMINAL TERRESTRE DE CARGA", "EL COLEGIO",
            "EL ROSAL", "FACATATIVA", "CARRERA SEGUNDA", "FUNZA", "FUSAGASUGA", "FUSAGASUGA PARQUE PRINCIPAL",
            "PRINCIPAL GIRARDOT", "UNICENTRO GIRARDOT", "GUADUAS", "LA CALERA", "PRADERA POTOSI",
            "LA MESA", "MADRID", "MELGAR", "TOLEMAIDA", "MOSQUERA", "PACHO", "SAN ANTONIO DEL TEQUENDAMA",
            "SIBATE", "TERREROS", "MERCURIO", "MIRAFLORES BOGOTA", "SOPO", "ALPINA SOPO", "SUBACHOQUE",
            "TENJO", "TOCANCIPA", "UBATE", "VIANI", "VILLETA", "ZIPAQUIRA LA CASONA", "MEGACITY",
            "RIOHACHA", "SUCHIIMMA", "VIVA WAJIIRA", "SAN JUAN DEL CESAR", "VILLANUEVA", "GARZON",
            "LA PLATA", "PRINCIPAL NEIVA", "AVENIDA 26", "LAS CEIBAS", "NEIVA CENTRO", "SAN PEDRO PLAZA",
            "SANTA LUCIA PLAZA", "UNICENTRO NEIVA", "PALERMO", "PITALITO", "SAN ANTONIO PITALITO",
            "CIENAGA", "FUNDACION", "CENTRO HISTORICO", "EL RODADERO", "LA 23", "BUENAVISTA", "LAS PALMAS",
            "ACACIAS", "GRANADA", "PUERTO GAITAN", "PRINCIPAL VILLAVICENCIO", "BARZAL", "CENTAUROS",
            "PARQUE LOS LIBERTADORES", "PRIMAVERA URBANA", "UNICENTRO VILLAVICENCIO", "UNICO VILLAVICENCIO",
            "VIVA VILLAVICENCIO", "IPIALES", "GRAN PLAZA IPIALES", "LA UNION NARIÑO", "PRINCIPAL PASTO",
            "COMFAMILIAR NARIÑO", "PARQUE NARIÑO", "PASTO NORTE", "UNICENTRO PASTO", "UNICO PASTO",
            "TUMACO", "TUQUERRES", "PRINCIPAL CUCUTA", "AV CERO", "CALLE 10", "EXITO SAN MATEO",
            "GRAN COLOMBIA", "JARDIN PLAZA CUCUTA", "UNICENTRO CUCUTA", "VENTURA", "OCAÑA", "PAMPLONA",
            "PUERTO BOYACA", "PRINCIPAL ARMENIA", "ARMENIA CAFETERA", "CALIMA ARMENIA", "MOCAWA PLAZA",
            "PORTAL DEL QUINDIO", "UNICENTRO ARMENIA", "CALARCA", "GENOVA", "LA TEBAIDA", "MONTENEGRO",
            "QUIMBAYA", "APIA", "BELEN DE UMBRIA", "CARTAGO", "DOS QUEBRADAS", "CENTRO COMERCIAL EL PROGRESO",
            "LA VIRGINIA", "MARSELLA", "PRINCIPAL PEREIRA", "AV. 30 DE AGOSTO", "CUBA", "CIUDAD VICTORIA",
            "MEGACENTRO", "PARQUE ARBOLEDA", "PARQUE EL LAGO", "PEREIRA PLAZA", "UNICENTRO PEREIRA",
            "QUINCHIA", "SANTA ROSA DE CABAL", "TATAMA", "SAN ANDRES", "BARBOSA", "SAN SILVESTRE",
            "PRINCIPAL BUCARAMANGA", "BOLARQUI", "BULEVAR SANTANDER", "CABECERA DEL LLANO", "CACIQUE",
            "CALLE 52", "CARRERA 34", "CENTRO CIAL. ACROPOLIS", "GARCIA ROVIRA", "MEGAMALL",
            "PASEO DEL COMERCIO", "PROVENZA", "SOTOMAYOR", "CHARALA", "CAÑAVERAL", "GIRON", "CHIMITA",
            "OIBA", "PIEDECUESTA", "DELA CUESTA", "RIONEGRO", "SAN GIL", "SAN VICENTE", "SOCORRO",
            "SUAITA", "SINCELEJO", "AV LAS PEÑITAS", "GUACARI", "CHAPARRAL", "ESPINAL", "FRESNO",
            "PRINCIPAL IBAGUE", "CARRERA QUINTA", "CENTRO COMERCIAL LA ESTACION", "LA MACARENA",
            "MULTICENTRO", "MURILLO TORO", "LIBANO", "MARIQUITA", "ORTEGA", "BUENAVENTURA", "BUGA",
            "BUGA PLAZA", "CAICEDONIA", "CALI CORPORATIVA Y EMPRESARIAL", "ALAMEDA", "AVENIDA SEXTA NORTE",
            "CALI", "CALI SUR", "CARVAJAL", "CENTRO NORTE", "CHIPICHAPE", "CIUDAD JARDIN CALI",
            "COSMOCENTRO", "IMBANACO", "JARDIN PLAZA", "LA CATORCE", "LA 14 PASOANCHO",
            "LA 14 VALLE DE LILI", "LA ESTACION CALI", "NUEVA TEQUENDAMA", "OBELISCO", "PALMETTO",
            "PETECUY", "PLAZA CAYCEDO", "PREMIER", "SAN FRANCISCO", "UNICALI", "UNICO", "VIPASA",
            "EL DOVIO", "JAMUNDI", "LA UNIÓN VALLE", "OBANDO", "PALMIRA", "LLANOGRANDE",
            "UNICENTRO PALMIRA", "RESTREPO VALLE", "ROLDANILLO", "SEVILLA", "TULUA", "MUNICIPIO VERSALLES",
            "YUMBO", "YUMBO CARVAJAL", "ZARZAL"
        );
        KNOWN_BRANCHES = branches.stream()
                                 .sorted(Comparator.comparingInt(String::length).reversed())
                                 .collect(Collectors.toList());
    }

    // El controlador ahora manejará la lógica de 'supports'
    public boolean supports(String text) {
        String normalizedText = text.toUpperCase().replaceAll("\\s+", " ");
        return normalizedText.contains("DAVIVIENDA") && normalizedText.contains("CUENTA DE AHORROS");
    }

    public List<AccountBancolombia> parse(String text) {
        List<AccountBancolombia> transactions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        String year = findStatementYear(Arrays.asList(lines));
        Pattern startOfTransactionPattern = Pattern.compile("^\\s*\\d{2}\\s+\\d{2}\\s+\\$");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (startOfTransactionPattern.matcher(line).find()) {
                StringBuilder fullTransactionText = new StringBuilder(line);
                int nextIndex = i + 1;
                while (nextIndex < lines.length && !isStartOfNewTransaction(lines[nextIndex]) && !isFooterOrHeader(lines[nextIndex])) {
                    fullTransactionText.append(" ").append(lines[nextIndex].trim());
                    nextIndex++;
                }

                String fullLine = fullTransactionText.toString();

                try {
                    Pattern datePattern = Pattern.compile("^(\\d{2})\\s+(\\d{2})");
                    Matcher dateMatcher = datePattern.matcher(fullLine);
                    if (!dateMatcher.find()) continue;
                    String date = dateMatcher.group(1) + "/" + dateMatcher.group(2) + "/" + year;

                    Pattern valuePattern = Pattern.compile("\\$ ([\\d,.]+)([+-]?)");
                    Matcher valueMatcher = valuePattern.matcher(fullLine);
                    if (!valueMatcher.find()) continue;
                    double value = parseValue(valueMatcher.group(0));

                    String branch = "";
                    for (String knownBranch : KNOWN_BRANCHES) {
                        if (fullLine.contains(knownBranch)) {
                            branch = knownBranch;
                            break;
                        }
                    }
                    
                    String description = fullLine
                            .replace(dateMatcher.group(0), "")
                            .replace(valueMatcher.group(0), "")
                            .replace(branch, "")
                            .replaceAll("\\s+", " ")
                            .trim();
                    
                    transactions.add(new AccountBancolombia(date, description, value, branch));

                } catch (Exception e) {
                    System.err.println("Ignorando línea Davivienda por error de formato: " + fullLine);
                }
                i = nextIndex - 1;
            }
        }
        return transactions;
    }
    
    private double parseValue(String valuePart) {
        Matcher valueMatcher = Pattern.compile("[\\d,.]+").matcher(valuePart);
        valueMatcher.find();
        double value = Double.parseDouble(valueMatcher.group(0).replace(",", ""));
        if (valuePart.endsWith("-")) {
            value = -value;
        }
        return value;
    }

    private boolean isFooterOrHeader(String line) {
        String upperCaseLine = line.toUpperCase();
        return upperCaseLine.contains("ESTE PRODUCTO CUENTA CON SEGURO") ||
               upperCaseLine.contains("DEFENSOR DEL CONSUMIDOR FINANCIERO") ||
               upperCaseLine.contains("BANCO DAVIVIENDA S.A NIT") ||
               upperCaseLine.contains("CLASE DE MOVIMIENTO");
    }

    private boolean isStartOfNewTransaction(String line) {
        return Pattern.compile("^\\s*\\d{2}\\s+\\d{2}\\s+\\$").matcher(line.trim()).find();
    }
    
    private String findStatementYear(List<String> lines) {
        for (String line : lines) {
            if (line.contains("INFORME DEL MES")) {
                Pattern pattern = Pattern.compile("(\\d{4})");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) return matcher.group(1);
            }
        }
        return String.valueOf(java.time.Year.now().getValue());
    }
}