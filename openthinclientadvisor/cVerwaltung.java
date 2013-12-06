package openthinclientadvisor;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * cVerwaltung ist die Verwaltungsklasse.
 * Sie enthält Methoden zum prüfen der Hard- und Softwarevoraussetzungen.
 * Zudem steuert Sie die Kommunikation zwischen der GUI und den restlichen Klassen.
 * @author Benedikt Diehl und Daniel Vogel
 */
public class cVerwaltung {

    /**
     * Windows dient als Hilfsvariable.
     * Die Variable wird durch die Methode checkOS() auf true gesetzt wenn das Programm unter Windows ausgeführt wird.
     */
    static boolean Windows = false;
    /**
     * Variable Standardmode ist eine Hilfsvariable.
     * Standardmode wird in der Methode TestStarten() benötigt um die für den
     * Standarmode notwendige Testreihe auszuführen.
     * Die Variable wird auf true gesetzt wenn die Standardmode aktiv ist.
     */
    static boolean standardmode = true;
    /**
     * Hilfsvariable um den Servermode sauber beenden zu können.
     * Die Variable wird auf true gesetzt wenn die Servermode aktiv ist.
     */
    static boolean servermode = false;
    /**
     * Hilfsvariable um zu erkennen ob der Server läuft.
     * Die Variable wird beim Start der Prüfung des Servermode auf "true" gesetzt.
     */
    static boolean serverrun = false;
    /**
     * Hilfsvariable in der das Ergebnis der Prüfung des freien Fesplattenspeichers in GB gehalten wird.
     */
    static int hd = 0;
    /**
     * Hilfsvariable in der die minimal benötigte Arbeitsspeichergröße gespeichert wird.
     * Die Variable wird mit 500 initialisiert da das die Minimal benötigte RAM Größe für Linux Systeme ist.
     * Wird beim Start des Programms ein Windows System erkannt wird diese Variable auf 1000 gesetzt.
     */
    static int ramsize = 500;
    /**
     * Hilfsvariable die zur Prüfung der benötigten Arbeitsspeichergröße verwendet wird.
     */
    static boolean ramOk = false;
    /**
     * Ein Objekt von cNetwork wird erstellt welches zur Prüfung der Netzwerkfunktionalität benötigt wird.
     */
    public static cNetwork cNetwork = new cNetwork();
    /**
     * Ein Objekt cReadWriteSplit wird erstellt dieses wird zum lesen und schreiben von Dateien benötigt.
     */
    public static cReadWriteSplit rws = new cReadWriteSplit();
    /**
     * Ein Objekt der GUI wird erstellt dieses stellt die Benutzeroberfläche dar.
     */
    public static GUI gui = new GUI();
    /**
     * Ein Objekt von cFilechooser wird erstellt dies wird benötigt um eine Benutzeroberfläche zum Abspeichern der Log Datei darzustellen.
     */
    public static cFilechooser fc = new cFilechooser();
    /**
     * Ein Objekt von cResults wird erstellt.
     */
    public static cResults results = new cResults();

    /**
     * Der Konstruktor setzt die Benutzeroberfläche Visible und Zentriert diese in der Bildschirmmitte.
     * Es wird zusätzlich die die Methode ckeckIfIniFileExists() der Klasse cReadWriteSplit ausgeführt.
     */
    public cVerwaltung() {
        gui.setLocationRelativeTo(null);
        cReadWriteSplit.ckeckIfIniFileExists();


    }

    /**
     * Methode um die GUI Visible zu schalten.
     */
    public static void showGUI() {
        gui.setVisible(true);
    }

    /**
     * Öffnet das Fenster jFrProxy das die Möglichkeit bietet Proxy Einstellungen zu Setzen.
     */
    public static void setProxy() {
        jFrProxy proxyGUI = new jFrProxy(cNetwork.getWerteProxy());
    }

    /**
     *  Überträgt die neuen Proxyeinstellungen von jFrProxy nach cNetwork
     */
    public static void setProxySettings() {
        cNetwork.setWerteProxy(jFrProxy.getProxySettings());
    }

    /**
     * Entsperrt die GUI und zeigt Sie an.
     */
    public static void GUIUnlock() {
        gui.setEnabled(true);
        gui.setVisible(true);
    }

    /**
     * Startet die Tests im jeweiligen Betriebsmodus.
     * Über eine Reihe von Verzweigungen ist festgelegt welche Tests in
     * welchem Modi durchgeführt werden.
     * @throws SocketException
     */
    public static void TestStarten() throws SocketException {
        if (serverrun == true) {
            cNetwork.KillServer();
            serverrun = false;
            gui.disableServerMode();
            gui.setResultsFalseServer();
            portresult();
            GUIUnlock();
        } else {
            if (standardmode == true) {
                WriteInTextBox(getDateAndTime());
                WriteInTextBox("Hostname: " + cNetwork.getHostname());
                WriteInTextBox(checkJavaVersion());
                WriteInTextBox("OS:  " + checkOS());
                WriteInTextBox(checkOSVersion());
                WriteInTextBox(checkTotalRam());
                File[] f = File.listRoots();
                for (File file : f) {
                    WriteInTextBox("Device: " + file.getAbsoluteFile());
                    WriteInTextBox("Total Space: " + file.getTotalSpace() / 1024 / 1024 / 1024 + " GB");
                    WriteInTextBox("Free Space: " + file.getFreeSpace() / 1024 / 1024 / 1024 + " GB\r\n");
                    hd = hd + Integer.parseInt("" + file.getFreeSpace() / 1024 / 1024 / 1024);
                }
                WriteInTextBox(cNetwork.Networkadapter());
                WriteInTextBox(cNetwork.InternetChecker());
                WriteInTextBox(cNetwork.dhcpChecker());
                checkResults();
                GUIUnlock();
            } else {
                if (servermode == true) {
                    WriteInTextBox(getDateAndTime());
                    WriteInTextBox("Hostname: " + cNetwork.getHostname() + "\r\n");
                    gui.enterServerMode();
                    serverrun = true;
                    cNetwork.RunServer();
                    GUIUnlock();
                } else {
                    WriteInTextBox(getDateAndTime());
                    WriteInTextBox("Hostname: " + cNetwork.getHostname() + "\r\n");
                    cNetwork.PortScanner(gui.getServerIP());
                    //GUIUnlock();
                }
            }
        }

    }

    /**
     * Prüft ob das Betriebssystem Windows ist und setzt die Variable Windows entsprechend.
     * @return
     * Gibt den OS Typ als String zurück (Windows/ Linux)
     */
    public static String checkOS() {
        String OS = System.getProperty("os.name");
        if (OS.contains("Windows")) {
            Windows = true;
        }
        return OS;
    }

    /**
     * Passt die Oberfläche und Variable ramsize an wenn das Programm auf einem Windows System läuft.
     */
    public static void tabelle() {
        checkOS();
        if (Windows == true) {
            gui.writeInTable("Minimum 1 GB, recommended 1,5 GB RAM on Microsoft Windows", 1, 0);
            ramsize = 1000;
        }
    }

    /**
     * Schreibt ein Logfile auf die Festplatte.
     * Der Pfad und der Dateiname werden mithilfe eines Filechoosers
     * im SaveDialog vom User eingegeben.
     * Der Inhalt der TextBox jTxtAusgabe und der Pfad+Dateiname wird an die
     * Methode rws.overwritefile() übergeben.
     */
    public static void SaveLog() {
        rws.overwriteFile(fc.saveDialog(), getTextBox());
    }

    /**
     * Öffnet das About Fenster
     */
    public static void showAbout() {
        About About = new About();
        About.setVisible(true);
    }

    /**
     * Gibt das aktuelle Datum und die Uhrzeit als String zurück.
     * Das Datum und die Uhrzeit werden im Format "Date and Time: dd.MM.yyyy 'at' HH:mm:ss" zurückgeliefert.
     * @return
     * Der return Parameter enthält das aktuelle Datum und die Uhrzeit im Format "Date and Time: dd.MM.yyyy 'at' HH:mm:ss".
     */
    public static String getDateAndTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy 'at' HH:mm:ss ");
        Date currentTime = new Date();
        String date = "Date and Time: " + formatter.format(currentTime);
        return date;
    }

    /**
     * Fügt den übergebenen String der TextBox jTxtAusgabe hinzu.
     * Da auf diese Methode auch aus den Threads von cServer,
     * cPortScanner und cKillServer zugegriffen wird ist diese Methode "synchronized".
     * @param text String
     * Der übergebene Parameter enthält Prüfungsergebnisse.
     */
    public synchronized static void WriteInTextBox(String text) {
        gui.WriteInTextBox(text);
    }

    /**
     * Gibt den Inhalt der TextBox jTxtAusgabe als String zurück.
     * @return
     * Der return Parameter enthält den Inhalt der TextBox jTxtAusgabe.
     */
    public static String getTextBox() {
        String Ausgabe = gui.getTextBox();
        return Ausgabe;
    }

    /**
     * Gibt den Inhalt der Variable Windows (true/false) zurück.
     * @return
     * Der return Parameter enthält den Inhalt der Variable Windows (true/false).
     */
    public static boolean getWindows() {
        return Windows;
    }

    /**
     * Gibt die auf dem System installierte Java Version als String zurück.
     * @return
     * Der return Parameter enthält die installierte Java Version im Format "Java Version: 1.6"
     */
    public static String checkJavaVersion() {
        String javaversion = "\r\n Java Version: " + System.getProperty("java.version");
        return javaversion;
    }

    /**
     * Liefert die Versions Nummer des installierten Betriebssystems zurück.
     * @return
     * Der return Parameter beinhaltet die Versionsnummer des Betriebssystems im Format "OS Version: xxxx".
     */
    public static String checkOSVersion() {
        String osversion = "OS Version:    " + System.getProperty("os.version");
        return osversion;
    }

    /**
     * Gibt die Größe des vorhandenen Arbeitsspeichers in MB zurück.
     * @return
     * Der return Parameter beinhaltet die Größe des Arbeitsspeichers im Format "TOTAL RAM: xxx MB".
     */
    public static String checkTotalRam() {
        OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        String ram = "TOTAL RAM: " + ((com.sun.management.OperatingSystemMXBean) bean).getTotalPhysicalMemorySize() / 1024 / 1024 + " MB\n";
        if (Integer.parseInt("" + ((com.sun.management.OperatingSystemMXBean) bean).getTotalPhysicalMemorySize() / 1024 / 1024) >= ramsize) {
            ramOk = true;
        }
        return ram;
    }

    /**
     * Setzt die Darstellung der Oberfläche in den StandardMode.
     * Hierfür wird die Tabelle mit den erforderlichen Inhalten der Standardmode gefüllt.
     * Des Weiteren wird die FAQ- Box gelöscht, das Log mit den entsprechenden
     * Daten gefüllt und das Feld zur Eingabe der IP-Adresse gesperrt.
     */
    public static void enterStandardMode() {
        gui.clearTable();
        gui.clearFAQ();
        gui.setResultsNotTestedStandard();
        gui.writeInTable("Sun Java 1.6 version", 0, 0);
        gui.writeInTable("Minimum 500 MB, recommended 1 GB RAM Linux", 1, 0);
        gui.writeInTable("Minimum 1 GB, recommended 2 GB of free harddisc space", 2, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 1098, 1099, 2069, 3873, 4444, 4445, 8009, 8080, 8083, 10389", 3, 0);
        gui.writeInTable("Several unassigned UDP-Ports: 67, 69, 514, 2069, 4011", 4, 0);
        gui.writeInTable("Only one configured NIC besides loopback", 5, 0);
        gui.writeInTable("Working internet connection", 6, 0);
        gui.writeInTable("A running DHCP server ", 7, 0);
        gui.clearTextBox();
        gui.enableTab();
        gui.showResults();
        gui.hintServerIP();
        tabelle();
        standardmode = true;
        WriteInTextBox("-------------------------------------Standard Mode is runnig!!!-------------------------------------\r\n");
    }

    /**
     * Setzt die Darstellung der Oberfläche in den ClientMode.
     * Hierbei wird der Zugriff auf die Tabelle gesperrt.
     * Das Log wird mit den entsprechenden Daten gefüllt und
     * das Feld zur Eingabe der IP-Adresse wird freigegeben.
     */
    public static void enterClientMode() {
        gui.clearTextBox();
        gui.showLog();
        gui.disableTab();
        gui.showServerIP();
        standardmode = false;
        servermode = false;
        WriteInTextBox("-------------------------------------Client Mode is runnig!!!-------------------------------------\r\n");
        WriteInTextBox("Enter the Server-IP and press the Start-Button!!\r\n");
    }

    /**
     * Setzt die Darstellung der Oberfläche in den ServerMode.
     * Hierbei wird die Tabelle mit den erforderlichen Inhalten der Servermode gefüllt.
     * Die FAQ-Box wird gelöscht, das Log wird mit den entsprechenden Daten
     * gefüllt und das Feld zur Eingabe der IP-Adresse wird gesperrt.
     */
    public static void enterServerMode() {
        gui.clearTable();
        gui.clearFAQ();
        gui.writeInTable("Several unassigned TCP-Ports: 1098", 0, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 1099", 1, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 2069", 2, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 3873", 3, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 4444", 4, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 4445", 5, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 8009", 6, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 8080", 7, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 8083", 8, 0);
        gui.writeInTable("Several unassigned TCP-Ports: 10389", 9, 0);
        gui.writeInTable("Several unassigned UDP-Ports: 67", 10, 0);
        gui.writeInTable("Several unassigned UDP-Ports: 69", 11, 0);
        gui.writeInTable("Several unassigned UDP-Ports: 514", 12, 0);
        gui.writeInTable("Several unassigned UDP-Ports: 2069", 13, 0);
        gui.writeInTable("Several unassigned UDP-Ports: 4011", 14, 0);
        gui.setResultsNotTestedServer();
        gui.clearTextBox();
        gui.enableTab();
        gui.showLog();
        gui.hintServerIP();
        standardmode = false;
        servermode = true;
        WriteInTextBox("-------------------------------------Server Mode is runnig!!!-------------------------------------");
        WriteInTextBox("First of all make sure that the openthinclient service is not running");
        WriteInTextBox("To check the connection between client and server press the start button");
        WriteInTextBox("an run the OpenThinClient-Advisor on a client too!!\r\n");
        WriteInTextBox("Start in client mode and enter the IP of this server\r\n");
        WriteInTextBox("In order to find the correct IP address of the server you get a list of all network cards that are installed. \r\n\r\n" + cNetwork.getLocalIps());

    }

    /**
     * Überprüft die Ergebnisse der Tests und schreibt Infos in die FAQ-Box.
     */
    public static void checkResults() {
        gui.setResultsFalse();
        if (standardmode == true) {
            gui.setResultsNotTested();
        }
        if (ramOk) {
            gui.setResultsPass(1, 1);
        } else {
            gui.WriteInFAQBox("Sorry, You have not enough RAM installed");
        }
        if (Double.parseDouble(System.getProperty("java.version").substring(0, 3)) >= 1.6) {
            gui.setResultsPass(0, 1);
        } else {
            gui.WriteInFAQBox("Please install JavaJRE 1.6");
        }
        if (cNetwork.internet() == true) {
            gui.setResultsPass(6, 1);
        } else {
            gui.WriteInFAQBox("Please make sure that your connected to the WWW");
        }
        if (hd >= 1) {
            gui.setResultsPass(2, 1);
        } else {
            gui.WriteInFAQBox("Sorry, You have not enough free harddisk space");
        }
        if (cNetwork.nics() == true) {
            gui.setResultsPass(5, 1);
        } else {
            gui.WriteInFAQBox("We identified more than one networkdevices on your system");
        }
        if (cDHCPClient.getWorks()) {
            gui.setResultsPass(7, 1);
        } else {
            gui.setResultFalse(7, 1);
            gui.WriteInFAQBox("DHCP-error, please check the log for details");
        }
    }

    /**
     * Schreibt eine Zeile in die FAQBox für jeden Port der gesperrt ist.
     */
    public static void portresult() {
        if (cResults.isP1098() == true) {
            gui.setResultsPass(0, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 1098 in your Firewall");
        }
        if (cResults.isP1099() == true) {
            gui.setResultsPass(1, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 1099 in your Firewall");
        }
        if (cResults.isP2069() == true) {
            gui.setResultsPass(2, 1);
            gui.setResultsPass(13, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 2069 in your Firewall");
        }
        if (cResults.isP3873() == true) {
            gui.setResultsPass(3, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 3873 in your Firewall");
        }
        if (cResults.isP4444() == true) {
            gui.setResultsPass(4, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 4444 in your Firewall");
        }
        if (cResults.isP4445() == true) {
            gui.setResultsPass(5, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 4445 in your Firewall");
        }
        if (cResults.isP8009() == true) {
            gui.setResultsPass(6, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 8009 in your Firewall");
        }
        if (cResults.isP8080() == true) {
            gui.setResultsPass(7, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 8080 in your Firewall");
        }
        if (cResults.isP8083() == true) {
            gui.setResultsPass(8, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 8083 in your Firewall");
        }
        if (cResults.isP10389() == true) {
            gui.setResultsPass(9, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 10389 in your Firewall");
        }
        if (cResults.isP67() == true) {
            gui.setResultsPass(10, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 67 in your Firewall");
        }
        if (cResults.isP69() == true) {
            gui.setResultsPass(11, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 69 in your Firewall");
        }
        if (cResults.isP514() == true) {
            gui.setResultsPass(12, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 514 in your Firewall");
        }
        if (cResults.isP4011() == true) {
            gui.setResultsPass(14, 1);
        } else {
            gui.WriteInFAQBox("Please open Port 4011 in your Firewall");
        }
    }
}
