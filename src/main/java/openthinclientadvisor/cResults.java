package openthinclientadvisor;

/**
 * Stellt einen Zwischenspeicher der Ergebnisse des Port-Scans dar. Bei einer
 * eingehenden Verbindung setzt der jeweilige Thread der Klasse cServer die
 * passende Variable seines Ports in dieser Klasse auf "true". Nach Beenden
 * der Servermode werden die Ergebnisse abgefragt.
 *
 * @author Daniel Vogel
 */
public class cResults {

    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 1098
     */
    private static boolean p1098 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 1099
     */
    private static boolean p1099 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 2069
     */
    private static boolean p2069 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 3873
     */
    private static boolean p3873 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 4444
     */
    private static boolean p4444 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 4445
     */
    private static boolean p4445 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 8009
     */
    private static boolean p8009 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 8080
     */
    private static boolean p8080 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 8083
     */
    private static boolean p8083 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 10389
     */
    private static boolean p10389 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 67
     */
    private static boolean p67 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 69
     */
    private static boolean p69 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 514
     */
    private static boolean p514 = false;
    /**
     * Hilfsvariable für das Prüfungsergebnis von Port 4011
     */
    private static boolean p4011 = false;

    /**
     * Setzt den jeweils übergebenen Port auf true
     * @param port
     */
    public static void setPortResult(int port) {
        if (port == 1098) {
            p1098 = true;
        }
        if (port == 1099) {
            p1099 = true;
        }
        if (port == 2069) {
            p2069 = true;
        }
        if (port == 3873) {
            p3873 = true;
        }
        if (port == 4444) {
            p4444 = true;
        }
        if (port == 4445) {
            p4445 = true;
        }
        if (port == 8009) {
            p8009 = true;
        }
        if (port == 8080) {
            p8080 = true;
        }
        if (port == 8083) {
            p8083 = true;
        }
        if (port == 10389) {
            p10389 = true;
        }
        if (port == 67) {
            p67 = true;
        }
        if (port == 69) {
            p69 = true;
        }
        if (port == 514) {
            p514 = true;
        }
        if (port == 4011) {
            p4011 = true;
        }
    }

    /** gibt die Hilfsvariable p1098 zurück
     * @return gibt die Hilfsvariable p1098 zurück
     */
    public static boolean isP1098() {
        return p1098;
    }

    /** gibt die Hilfsvariable p1099 zurück
     * @return gibt die Hilfsvariable p1099 zurück
     */
    public static boolean isP1099() {
        return p1099;
    }

    /** gibt die Hilfsvariable p2069 zurück
     * @return gibt die Hilfsvariable p2069 zurück
     */
    public static boolean isP2069() {
        return p2069;
    }

    /** gibt die Hilfsvariable p3873 zurück
     * @return gibt die Hilfsvariable p3873 zurück
     */
    public static boolean isP3873() {
        return p3873;
    }

    /** gibt die Hilfsvariable p4444 zurück
     * @return gibt die Hilfsvariable p4444 zurück
     */
    public static boolean isP4444() {
        return p4444;
    }

    /** gibt die Hilfsvariable p4445 zurück
     * @return gibt die Hilfsvariable p4445 zurück
     */
    public static boolean isP4445() {
        return p4445;
    }

    /** gibt die Hilfsvariable p8009 zurück
     * @return gibt die Hilfsvariable p8009 zurück
     */
    public static boolean isP8009() {
        return p8009;
    }

    /** gibt die Hilfsvariable p8080 zurück
     * @return gibt die Hilfsvariable p8080 zurück
     */
    public static boolean isP8080() {
        return p8080;
    }

    /** gibt die Hilfsvariable p8083 zurück
     * @return gibt die Hilfsvariable p8083 zurück
     */
    public static boolean isP8083() {
        return p8083;
    }

    /** gibt die Hilfsvariable p10389 zurück
     * @return gibt die Hilfsvariable p10389 zurück
     */
    public static boolean isP10389() {
        return p10389;
    }

    /** gibt die Hilfsvariable p67 zurück
     * @return gibt die Hilfsvariable p67 zurück
     */
    public static boolean isP67() {
        return p67;
    }

    /** gibt die Hilfsvariable p69 zurück
     * @return gibt die Hilfsvariable p69 zurück
     */
    public static boolean isP69() {
        return p69;
    }

    /** gibt die Hilfsvariable p514 zurück
     * @return gibt die Hilfsvariable p514 zurück
     */
    public static boolean isP514() {
        return p514;
    }

    /** gibt die Hilfsvariable p4011 zurück
     * @return gibt die Hilfsvariable p4011 zurück
     */
    public static boolean isP4011() {
        return p4011;
    }
}
