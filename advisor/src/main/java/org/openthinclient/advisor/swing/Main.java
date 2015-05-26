package org.openthinclient.advisor.swing;

/**
 * Main-Klasse die beim starten des Programms ausgef??hrt wird. Sie erzeugt ein
 * Objekt des jFrames Startscreen und macht diesen sichtbar.
 * @author Benedikt Diehl
 */
public class Main {

    /**
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Startscreen startscreen = new Startscreen();
        startscreen.setVisible(true);
    }
}
