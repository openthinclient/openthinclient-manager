package org.openthinclient.advisor;

import java.io.*;
import java.net.*;

/**
 * <p>Die Klasse cPortScanner f??hrt einen Port Scan auf einen Zielrechner durch.
 * Hierf??r werden die ??bergabeparameter aus dem Konstruktor genutzt</p>
 *
 * <p>In die Klasse ist die Schnittstelle ???runnable??? implementiert. Dies ist
 * erforderlich das die Klasse als Thread parallel ausgef??hrt werden kann.</p>
 *
 * @author Daniel Vogel
 */
public class cPortScanner implements Runnable {

    /**
     * In diese Variable wird der Port zwischengespeichert, der durch den
     * Konstruktor ??bergeben wird
     */
    int port;
    /**
     * In dieser Variable wird der Hostname bzw. die IP-Adresse des Zielrechners
     * gesetzt, der vom Konstruktor ??bergeben wird.
     */
    String host;

    /**
     * Der Konstruktor der Klasse cPortScanner hat zwei ??bergabeparameter. 
     * Der erste ??bergabewert (String host) dient zur Bestimmung des Zielrechners. 
     * Dieser kann entweder in Form einer IP-Adresse (192.168.1.10) oder als 
     * Hostname (openthinclient-server) angegeben werden. Der 2.te 
     * ??bergabeparamater (int port) bestimmt den Port, auf dem der Zielrechner 
     * der gescannt werden soll.</p>
     *
     * @param host IP-Adresse (192.168.1.1) oder Hostname (openthinclient-Server)
     * @param port Portnummer (80)
     */
    public cPortScanner(String pHost, int pPort) {
        this.port = pPort;
        this.host = pHost;
    }

    /**
     * Die Run Methode wird beim Initialisieren der Klasse automatisch gestartet.
     * Sie f??hrt einen Portscan auf den jeweiligen Host und Port durch.
     * 
     * <p>Das Ergebnis des Port Scans wird mithilfe der Methode WriteInTextBox 
     * aus der Klasse cVerwaltung in die TextBox (LogFile) der GUI 
     * geschrieben und sieht Beispielweise wie folgt aus:</p>
     * <br>
     * <table border="3" frame="void">
     * <tr><td><b>Status des Port-Scans</b></td><td><b>Ausgabe f??r das Log-File</b></td>
     * <tr><td>Bei erfolgreichem Port Scan</td>   <td>Connected to 192.168.1.10 on Port 67</td></tr>
     *
     * <tr><td>Sollte die IP-Adresse bzw. der Hostname ung??ltig sein oder nicht aufgel??st
     * werden k??nnen</td> <td>Unkown Host 192.168.1.10</td></tr>
     *
     * <tr><td>Wenn der Host erreichbar ist, jedoch keine Verbindung auf dem jeweiligen Port
     * zul??sst</td>     <td>No Connection to 192.168.1.10 on Port 67</td></tr>
     * </table>
     */
    public void run() {
        String PSAusgabe = "Connected to " + host + " on Port " + port;
        String AusgabeUnknown = "Unkown Host " + host;
        String AusgabeExeption = "No Connection to " + host + " on Port " + port;
        try {
            Socket target = new Socket(host, port);
            cVerwaltung.WriteInTextBox(PSAusgabe);
            target.close();
        } catch (UnknownHostException ex) {
            cVerwaltung.WriteInTextBox(AusgabeUnknown);
        } catch (IOException ex) {
            cVerwaltung.WriteInTextBox(AusgabeExeption);
        }
    }
}
