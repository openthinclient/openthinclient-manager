package openthinclientadvisor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simuliert einen Server-Dienst, der auf eingehende Verbindungen lauscht.
 *
 * <p>In die Klasse ist die Schnittstelle „runnable“ implementiert. Dies ist
 * erforderlich, dass die Klasse als Thread parallel ausgeführt werden kann.</p>
 *
 * @author Daniel Vogel
 */
public class cServer implements Runnable {

    /**
     * In diese Variable wird der Port zwischengespeichert, der durch den Konstruktor übergeben wird
     */
    private int port;

    /**
     * Der Konstruktor der Klasse cServer beinhaltet einen Übergabeparameter.
     * Dieser dient zur Bestimmung des Ports.
     * @param pPort Port als Integer (z.B. "80") auf dem der Serverdienst gestartet werden soll
     * @throws IOException
     */
    public cServer(int pPort) throws IOException {
        this.port = pPort;
    }

    /**
     *
     * <p>Run Methode die beim Initialisieren der Klasse automatisch ausgeführt
     * wird. Sie startet den Server-Dienst auf dem Port, welcher vom Konstruktor
     * übergeben wird. Der Status des Server-Dienstes wird mithilfe der Methode
     * WriteInTextBox aus der Klasse cVerwaltung in die TextBox (LogFile) der
     * GUI geschrieben.</p>
     * 
     * Die Ausgabe wird wie folgt dargestellt:
     * <br>
     * <table border="3" frame="void">
     * <tr><td><b>Status des Port-Scans</b></td><td><b>Ausgabe für das Log-File</b></td>
     * <tr><td>Bei aktivem Server-Dienst</td>   <td>Server is listen to Port 80</td></tr>
     *
     * <tr><td>Bei eingehender Verbindung</td> <td>incoming connection on port 80</td></tr>
     *
     * <tr><td>Falls der Dienst auf dem gewünschten Port nicht gestartet werden kann</td>
     * <td>Server is crashed on Port 80</td></tr>
     * </table>
     *
     */
    public void run() {
        ServerSocket serverSocket = null;
        cVerwaltung.WriteInTextBox("Server is listen to Port " + port);
        try {
            serverSocket = new ServerSocket(port);
            Socket client = serverSocket.accept();
            if (cNetwork.getServerrun() == true) {
                cVerwaltung.WriteInTextBox("incoming connection on port " + port);
                cResults.setPortResult(port);
            }
            while (cNetwork.getServerrun() == true) {
                serverSocket.accept();
                if (cNetwork.getServerrun() == true) {
                    cVerwaltung.WriteInTextBox("incoming connection on port " + port);
                    cResults.setPortResult(port);
                }
            }
            serverSocket.close();

        } catch (IOException ex) {
            cVerwaltung.WriteInTextBox("Server is crashed on Port " + port);
            Logger.getLogger(cServer.class.getName()).log(Level.SEVERE, null, ex);
        }





    }
}
