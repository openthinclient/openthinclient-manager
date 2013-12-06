package openthinclientadvisor;

import java.io.*;
import java.net.*;

/**
 * Die Klasse cKillServer ist für das beendet des Server-Dienstes verantwortlich,
 * indem Sie einen Port-Scan auf Localhost durchführt. Hierfür werden die
 * Übergabeparameter aus dem Konstruktor genutzt.
 *
 * <p>Die Klasse hat die Schnittstelle „runnable“ implementiert. Dies ist
 * erforderlich das die Klasse als Thread parallel ausgeführt werden kann.</p>
 *
 * @author Daniel Vogel
 */
public class cKillServer
        implements Runnable {

    /**
     * In diese Variable wird der Port zwischengespeichert, der durch den Konstruktor übergeben wird
     */
    int port;
    /**
     * Mit dieser Variablen wird "localhost" als Host für den Portscan gesetzt
     */
    String host = "localhost";
    /**
     * Zwischenspeicher für die Textausgabe des Portscans
     */
    String PSAusgabe;

    /**
     * Der Konstruktor der Klasse cKillServer hat einen Übergabeparameter.
     * Dieser dient zur Bestimmung des Ports, auf dem der Server-Dienst beendet
     * werden soll.
     * @param port Portnummer (80)
     */
    public cKillServer(int i) {
        this.port = i;
    }

    /**
     * <p>Die Run Methode wird beim Initialisieren der Klasse automatisch
     * gestartet. Sie führt einen Portscan auf dem jeweiligen Port durch.</p>
     *
     * <p>Bei erfolgreichem Port Scan, wird mithilfe der Methode WriteInTextBox
     * aus der Klasse cVerwaltung in die TextBox (LogFile) der GUI folgender
     * String geschrieben:</p>Kill Server on Port + (der jeweilige Port)</p>
     */
    public void run() {
        try {
            Socket target = new Socket(host, port);
            PSAusgabe = "Kill Server on Port " + port;
            cVerwaltung.WriteInTextBox(PSAusgabe);
            target.close();
        } catch (UnknownHostException ex) {
        } catch (IOException ex) {
        }
    }
}
