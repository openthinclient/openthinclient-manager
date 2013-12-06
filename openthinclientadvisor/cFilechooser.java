package openthinclientadvisor;

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 * Die Klasse stellt Filechosser zum Lesen und speichern von Dateien zur Verfügung.
 * Ein Filechooser stellt das Dateisystem grafisch in einer Baumstruktur dar.
 * Dadurch bietet er dem Anwender eine einfache Art, den Speicherort und Name
 * einer Datei zu wählen. Diese Möglichkeit kann sowohl zum Speichern als auch
 * zum Öffnen von Dateien genutzt werden.
 *
 * @author Daniel
 */
public class cFilechooser {

    /**
     * Die String Variable PathFilename dient als Zwischenspeicher der beiden
     * Filechooser. Hier wird der Dateipfad und Dateiname zwischengespeichert.
     */
    static String PathFilename;

    /**
     * Öffnet einen FileChooser zum speichern von Dateien und gibt die vom
     * Anwender gewählten "Pfad+Dateiname" als String zurück! Da im
     * openthinclient Advisor mit dieser Methode das Log-File gespeichert wird,
     * ist in dieser Methode zusätzlich eine Verzweigung eingebaut die
     * sicherstellt, dass dem Dateinamen die Endung ".log" angehängt wird falls
     * dies durch den Anwender nicht erfolgt.
     *
     * @return String "Pfad+Dateiname"
     */
    public String saveDialog() {
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
            String Filename = file.getName();
            if (Filename.contains(".log")) {
            } else {
                Filename = Filename + ".log";
            }
            if (cVerwaltung.getWindows() == true) {

                PathFilename = file.getParent() + "\\" + Filename;
            } else {
                PathFilename = file.getParent() + "/" + Filename;
            }
        } else {
            PathFilename = null;
        }
        cVerwaltung.GUIUnlock();
        return PathFilename;
    }

    /**
     * Öffnet einen FileChooser zum öffnen von Dateien und gibt den vom Anwender
     * gewählten "Pfad+Dateiname" als String zurück!
     * @return String "Pfad+Dateiname"
     */
    public String openDialog() {
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

        fc.setDialogTitle("Choose Path and FileName ");
        fc.setMultiSelectionEnabled(false);
        int state = fc.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String Filename = file.getName();
            if (Filename.contains(".log")) {
            } else {
                Filename = Filename + ".log";
            }
            if (cVerwaltung.getWindows() == true) {

                PathFilename = file.getParent() + "\\" + Filename;
            } else {
                PathFilename = file.getParent() + "/" + Filename;
            }
        } else {
            PathFilename = null;
        }
        cVerwaltung.GUIUnlock();
        return PathFilename;
    }
}
