package org.openthinclient.advisor.swing;

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 * Die Klasse stellt Filechosser zum Lesen und speichern von Dateien zur Verf??gung.
 * Ein Filechooser stellt das Dateisystem grafisch in einer Baumstruktur dar.
 * Dadurch bietet er dem Anwender eine einfache Art, den Speicherort und Name
 * einer Datei zu w??hlen. Diese M??glichkeit kann sowohl zum Speichern als auch
 * zum ??ffnen von Dateien genutzt werden.
 *
 * @author Daniel
 */
public class cFilechooser {

    /**
     * Die String Variable lastSelectedFile dient als Zwischenspeicher der beiden
     * Filechooser. Hier wird der Dateipfad und Dateiname zwischengespeichert.
     */
    File lastSelectedFile;

    /**
     * ??ffnet einen FileChooser zum speichern von Dateien und gibt die vom
     * Anwender gew??hlten "Pfad+Dateiname" als String zur??ck! Da im
     * openthinclient Advisor mit dieser Methode das Log-File gespeichert wird,
     * ist in dieser Methode zus??tzlich eine Verzweigung eingebaut die
     * sicherstellt, dass dem Dateinamen die Endung ".log" angeh??ngt wird falls
     * dies durch den Anwender nicht erfolgt.
     *
     * @return String "Pfad+Dateiname"
     */
    public File saveDialog() {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.isDirectory()
                        || f.getName().toLowerCase().endsWith(".log");
            }

            @Override
            public String getDescription() {
                return "Log-Datei";
            }
        });

        fc.setDialogTitle("Choose Path and FileName");
        fc.setMultiSelectionEnabled(false);
        int state = fc.showSaveDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String filename = file.getName();
            if (!filename.endsWith(".log")) {
                file = new File(file.getParentFile(), filename + ".log");
            }
            lastSelectedFile = file;
            return lastSelectedFile;
        }
        // the request has been cancelled
        return null;
    }
}
