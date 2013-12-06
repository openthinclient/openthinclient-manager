package openthinclientadvisor;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Die Klasse cInetConnection prüft, ob eine funktionierende Internetverbindung vorhanden ist.
 * @author Benedikt Diehl
 */
public class cInetConnection {

    private int BUF_SIZE;
    /**
     * Hilfsvariable die das Prüfungsergebnis der Methode TOnline() speichert.
     */
    private boolean tonline = false;
    /**
     * Hilfsvariable die das Prüfungsergebnis der Methode Google() speichert.
     */
    private boolean google = false;
    /**
     * Hilfsvariable die das Prüfungsergebnis der Methode Microsoft() speichert.
     */
    private boolean microsoft = false;
    /**
     * Hilfsvariable die das Prüfungsergebnis der Methode MSN() speichert.
     */
    private boolean msn = false;
    /**
     * Hilfsvariable die das Endergebnis der Gesamtprüfung speichert.
     */
    private boolean internet = false;

    /**
     * Die Methode CheckInternetConnection stellt die Hauptprüfung dar und steuert den Prüfungsprozess.
     * In der Methode wird die Internetverbindung überprüft in dem die Website http://www.openthinclient.org abgerufen wird.
     * Da diese Prüfung alleine  kein zuverlässiges Ergebnis liefert, wird die Internetverbindung im Anschluss
     * noch genauer durch den Aufruf der Methoden Google, TOnline, Microsoft und MSN geprüft.
     * Um Prüffehler durch seitens der Webseitenbetreiber geänderte Inhalte zu vermeiden,
     * müssen immer nur zwei dieser vier Prüfmethoden ein positives Ergebnis liefern.
     *
     * @return
     * Der return Parameter liefert das Ergebnis der Prüfung als Boolean wert zurück.
     */
    public boolean CheckInternetConnection() {

        //prüft ob überhaupt eine Verbindungsmöglichkeit besteht.

        try {
            URL url = new URL("http://www.openthinclient.org");
            URLConnection urlConnection = url.openConnection();

            InputStream inputStream = urlConnection.getInputStream();
            Reader reader = new InputStreamReader(inputStream);

            StringBuilder contents = new StringBuilder();
            CharBuffer buf = CharBuffer.allocate(BUF_SIZE);

            while (true) {
                reader.read(buf);
                if (!buf.hasRemaining()) {
                    break;
                }

                contents = contents.append(buf);
            }
            inputStream.close();

            //Verbindungsmöglichkeit besteht es wird überprüft ob das Internet tatsächlich verfügbar ist oder ob nur eine Proxy seite angezeigt wird

            this.Google();
            this.TOnline();
            this.Microsoft();
            this.MSN();

            // Über diese Konstruktion aus || und && wird sichergestellt das immer nur 2 der 4 Testergebnisse
            // positiv ausfallen müssen dadurch soll verhindert werden das durch Änderung an einer der Seiten
            // durch die Betreiber ein falsches Ergebnis resultiert.

            if ((tonline && msn) || (microsoft && google) || (msn && google) || (tonline && google) || (tonline && microsoft) || (msn && microsoft)) {
                internet = true;
            }
            return internet;
        } catch (Exception e) {
            return internet;
        }
    }

    /**
     * Die Methode TOnline prüft den HTML-Code der Seite http://www.t-online.de auf spezifische Merkmale.
     * Sollte diese Prüfung einen Treffer erzielen, wird die Boolean Variable tonline auf true gesetzt.
     *
     * @throws Exception
     */
    public void TOnline() throws Exception {
        try {
            InputStream is = null;
            String s = null;
            URL url = new URL("http://www.t-online.de");
            is = url.openStream();
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            while ((s = dis.readLine()) != null) {
                if (s.contains("Deutsche Telekom AG")) {
                    tonline = true;
                }
            }

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
    }

    /**
     * Die Methode Google prüft den HTML-Code der Seite http://www.google.com auf spezifische Merkmale.
     * Sollte diese Prüfung einen Treffer erzielen wird die Boolean Variable google auf true gesetzt.
     *
     * @throws Exception
     */
    public void Google() throws Exception {
        try {
            InputStream is = null;
            String s = null;
            URL url = new URL("http://www.google.com");
            is = url.openStream();
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            while ((s = dis.readLine()) != null) {
                if (s.contains("mail.google") && s.contains("news.google")) {
                    google = true;
                }
            }

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
    }

    /**
     * Die Methode Microsoft prüft den HTML-Code der Seite http://www.Microsoft.com auf spezifische Merkmale.
     * Sollte diese Prüfung einen Treffer erzielen, wird die Boolean Variable Microsoft auf true gesetzt.
     *
     * @throws Exception
     */
    public void Microsoft() throws Exception {
        try {
            InputStream is = null;
            String s = null;
            URL url = new URL("http://www.Microsoft.com");
            is = url.openStream();
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            while ((s = dis.readLine()) != null) {
                if (s.contains("Microsoft Corporation")) {
                    microsoft = true;
                }
            }

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
    }

    /**
     * Die Methode Microsoft prüft den HTML-Code der Seite http://www.msn.com auf spezifische Merkmale.
     * Sollte diese Prüfung einen Treffer erzielen, wird die Boolean Variable MSN auf true gesetzt.
     *
     * @throws Exception
     */
    public void MSN() throws Exception {

        try {
            InputStream is = null;
            String s = null;
            URL url = new URL("http://www.msn.com");
            is = url.openStream();
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            while ((s = dis.readLine()) != null) {
                if (s.contains("Messenger") && s.contains("Hotmail")) {
                    msn = true;
                }
            }

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
    }
}
