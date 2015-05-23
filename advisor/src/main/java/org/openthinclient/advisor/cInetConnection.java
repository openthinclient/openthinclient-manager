package org.openthinclient.advisor;

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
 * Die Klasse cInetConnection pr??ft, ob eine funktionierende Internetverbindung vorhanden ist.
 * @author Benedikt Diehl
 */
public class cInetConnection {

    private int BUF_SIZE;
    /**
     * Hilfsvariable die das Pr??fungsergebnis der Methode TOnline() speichert.
     */
    private boolean tonline = false;
    /**
     * Hilfsvariable die das Pr??fungsergebnis der Methode Google() speichert.
     */
    private boolean google = false;
    /**
     * Hilfsvariable die das Pr??fungsergebnis der Methode Microsoft() speichert.
     */
    private boolean microsoft = false;
    /**
     * Hilfsvariable die das Pr??fungsergebnis der Methode MSN() speichert.
     */
    private boolean msn = false;
    /**
     * Hilfsvariable die das Endergebnis der Gesamtpr??fung speichert.
     */
    private boolean internet = false;

    /**
     * Die Methode CheckInternetConnection stellt die Hauptpr??fung dar und steuert den Pr??fungsprozess.
     * In der Methode wird die Internetverbindung ??berpr??ft in dem die Website http://www.openthinclient.org abgerufen wird.
     * Da diese Pr??fung alleine  kein zuverl??ssiges Ergebnis liefert, wird die Internetverbindung im Anschluss
     * noch genauer durch den Aufruf der Methoden Google, TOnline, Microsoft und MSN gepr??ft.
     * Um Pr??ffehler durch seitens der Webseitenbetreiber ge??nderte Inhalte zu vermeiden,
     * m??ssen immer nur zwei dieser vier Pr??fmethoden ein positives Ergebnis liefern.
     *
     * @return
     * Der return Parameter liefert das Ergebnis der Pr??fung als Boolean wert zur??ck.
     */
    public boolean CheckInternetConnection() {

        //pr??ft ob ??berhaupt eine Verbindungsm??glichkeit besteht.

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

            //Verbindungsm??glichkeit besteht es wird ??berpr??ft ob das Internet tats??chlich verf??gbar ist oder ob nur eine Proxy seite angezeigt wird

            this.Google();
            this.TOnline();
            this.Microsoft();
            this.MSN();

            // ??ber diese Konstruktion aus || und && wird sichergestellt das immer nur 2 der 4 Testergebnisse
            // positiv ausfallen m??ssen dadurch soll verhindert werden das durch ??nderung an einer der Seiten
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
     * Die Methode TOnline pr??ft den HTML-Code der Seite http://www.t-online.de auf spezifische Merkmale.
     * Sollte diese Pr??fung einen Treffer erzielen, wird die Boolean Variable tonline auf true gesetzt.
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
     * Die Methode Google pr??ft den HTML-Code der Seite http://www.google.com auf spezifische Merkmale.
     * Sollte diese Pr??fung einen Treffer erzielen wird die Boolean Variable google auf true gesetzt.
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
     * Die Methode Microsoft pr??ft den HTML-Code der Seite http://www.Microsoft.com auf spezifische Merkmale.
     * Sollte diese Pr??fung einen Treffer erzielen, wird die Boolean Variable Microsoft auf true gesetzt.
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
     * Die Methode Microsoft pr??ft den HTML-Code der Seite http://www.msn.com auf spezifische Merkmale.
     * Sollte diese Pr??fung einen Treffer erzielen, wird die Boolean Variable MSN auf true gesetzt.
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
