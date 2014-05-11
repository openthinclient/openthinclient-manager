package org.openthinclient.advisor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import javax.swing.JOptionPane;

/**
 * Enth??lt Methoden zum Lesen von Daten aus einer Datei sowie zum Schreiben von Daten in eine Datei.
 * Zudem bietet die Klasse Funktionen um die eingelesenen Informationen anhand eines Trennzeichens zu
 * splitten. Im folgenden Kapitel werden die einzelnen Methoden der Klasse beschrieben.
 * 
 * @author Daniel Vogel
 */
public class cReadWriteSplit {

    /**
     * Diese Methode dient dazu einen String an das Ende einer Datei zu schreiben.
     * Die Methode beinhaltet die zwei ??bergabeparameter vom Datentyp String.
     * Im ersten (String filename) wird der Dateiname und Dateipfad als String
     * angegeben. Der zweite ??bergabeparameter (String data) enth??lt die Daten,
     * welche in die Datei geschrieben werden sollen. Bisher wird diese Funktion
     * noch nicht genutzt. Aufgrund des geringen Arbeitsaufwandes wurde die
     * Methode jedoch f??r sp??tere Features des Programmes gleich mit eingebaut.
     * Vorstellbar w??re z. B. die Portliste, welche in der Datei ???ports.ini??? abgelegt
     * wird ??ber die GUI erweiterbar zu machen. Hierf??r k??nnte diese Methode
     * zum Einsatz kommen.
     *
     * @param filename Dateipfad und Dateiname
     * @param data String der in die Datei geschrieben werden soll
     */
    public void writeFileAdd(String filename, String data) {
        RandomAccessFile datei = null;
        try {
            datei = new RandomAccessFile(filename, "rw");
            try {
                datei.seek(datei.length());
                datei.writeBytes(data);
                datei.close();
            } catch (IOException iOException) {
            }

        } catch (FileNotFoundException ex) {
        }
    }

    /**
     * Diese Methode schreibt ebenfalls einen String in eine Datei. Im Gegensatz
     * zur zuvor beschriebenen Methode writeFileAdd() werden die Daten jedoch
     * nicht ans Dateiende angeh??ngt, sondern ??berschrieben. Die ??bergabeparameter
     * sind mit den oben genannten Parametern aus der Methode writeFileAdd()
     * identisch.
     *
     * @param filename Dateipfad und Dateiname
     * @param data String der in die Datei geschrieben werden soll
     */
    public void overwriteFile(String filename, String data) {
        if (filename != null) {

            RandomAccessFile datei = null;
            try {
                datei = new RandomAccessFile(filename, "rw");
                try {
                    datei.writeBytes(data);
                    datei.close();
                } catch (IOException iOException) {
                }

            } catch (FileNotFoundException ex) {
            }
        } else {
        }
    }

    /**
     * Die Methode liest den Inhalt einer Datei aus und gibt diesen als String
     * zur??ck. Die Methode hat den ??bergabeparameter (String dateiname). In
     * diesem wird der Dateipfad und Dateiname der zu lesenden Datei angeben.
     *
     * @param dateiname Dateipfad + Name
     * @return String mit Dateiinhalt
     */
    public static String readFile(String dateiname) {
        RandomAccessFile datei = null;
        String zeile = null;
        try {
            datei = new RandomAccessFile(dateiname, "r");
            try {
                zeile = datei.readLine();
                while (zeile != null) {
                    zeile = datei.readLine();
                }
            } catch (IOException ioException) {
            }
        } catch (Exception ex) {
        }
        return zeile;
    }

    /**
     * Die Methode splittet einen String anhand eines Trennzeichens und gibt
     * diesen als String-Array zur??ck. Mit dem ??bergabeparameter (String zeile)
     * wird der zu splittende String an die Methode ??bergeben.
     * Der Parameter (String trennzeichen) legt das Trennzeichen fest.
     * Folgende Beispiel soll die Funktion verdeutlichen:
     *
     * <p>Enth??lt der String z.B. folgende Daten: "Alex;Berta;Christian"
     * und als Trennzeichen wird ein ??? ; ??? (Stichpunkt) gew??hlt, so enth??lt das
     * Array folgende Daten:</p>
     * <p>splittArray[0] = Alex</p>
     * <p>splittArray[1] = Berta</p>
     * <p>splittArray[2]= Christian</p>
     *
     * @param zeile String der gesplittet werden soll
     * @param trennzeichen Trennzeichen
     * @return Array
     */
    public static String[] split(String zeile, String trennzeichen) {
        String[] splittArray = zeile.split(trennzeichen);
        return splittArray;
    }

    /**
     * Diese Methode kombiniert die Funktion des Einlesens einer Datei und das
     * splitten deren Inhalt. Mithilfe der ??bergabeparameter wird der Dateiname
     * und Dateipfad sowie das Trennzeichen bestimmt. R??ckgabewert der Methode
     * ist ein String- Array.
     *
     * @param dateiname Dateipfad + Dateiname
     * @param splitzeichen Splittzeichen
     * @return Array
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String[] readSplitFile(String dateiname, String splitzeichen) throws FileNotFoundException, IOException {
        String[] splittArray = null;
        RandomAccessFile datei = null;
        String zeile = null;
        datei = new RandomAccessFile(dateiname, "r");
        zeile = datei.readLine();
        while (zeile != null) {
            splittArray = zeile.split(splitzeichen);
            zeile = datei.readLine();
        }
        return splittArray;
    }

    /**
     * Dies Methode cReadWriteSplit pr??ft ob die Datei ???ports.ini??? im
     * Ausf??hrungsverzeichnis des openthinclient Advisors existiert. Diese
     * Datei wird vom Port-Scanner sowie vom Server-Dienst zur Ausf??hrung
     * zwingend ben??tigt. Sie enth??lt die Angaben der Ports die von der
     * openthinclient software suite ben??tigt werden. Sollte die Datei nicht
     * vorhanden sein wird sie im selben Verzeichnis indem der openthinclient-Advisor
     * ausgef??hrt wird erstellt. Falls der Benutzer in diesem Verzeichnis keine
     * Schreibrechte besitzt, wird eine Fehlermeldung ausgegeben.
     * Nach Best??tigung mit >>OK>> wird das Programm beendet.
     */
    public static void ckeckIfIniFileExists() {
        String ports = "1098;1099;2069;3873;4444;4445;8009;8080;8083;10389;67;69;514;4011";
        File lagerDatei = new File("ports.ini");
        if (!lagerDatei.exists()) {
            try {
                PrintWriter creator = new PrintWriter("ports.ini");
                creator.write(ports);
                creator.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "You don??t have any write permission in the execution path! \r\nPlease run the Openthinclient Advisor as root!", "Error_Message", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        } else {
        }
    }
}
