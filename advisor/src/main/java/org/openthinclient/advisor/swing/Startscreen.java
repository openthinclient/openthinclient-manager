package org.openthinclient.advisor.swing;

import org.openthinclient.advisor.cVerwaltung;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import javax.swing.JOptionPane;

/**
 * GUI welche die Auswahl sowie den Wechsel des Betriebsmodus erm??glicht.
 * Sie erscheint direkt nach Programmstart oder beim Klick auf den Button
 * "Change Execution Mode" in der Haupt-GUI.
 *
 * @author Daniel Vogel
 */
public class Startscreen extends javax.swing.JFrame {

    /** 
     * Konstruktor der Klasse Startscreen. Hier wird die Methode centerGUI aufgerufen
     * welche die GUI in der Bildschirmmitte anzeigt. Des weiteren wird der Radio-Button
     * der Standardmode angeklickt und ein Objekt der Klasse cVerwaltung erzeugt.
     */
    public Startscreen() {
        initComponents();
        centerGUI(this);
        jRBtnStd.doClick();
        cVerwaltung verwaltung = new cVerwaltung();
    }

    /**
     * Zeigt die GUI in der Bildschirmmitte an
     * @param gui Angabe der GUI (this)
     */
    private void centerGUI(Window gui) {
        Dimension dm = Toolkit.getDefaultToolkit().getScreenSize();
        double width = dm.getWidth();
        double height = dm.getHeight();
        double xPosition = (width / 2 - gui.getWidth() / 2);
        double yPosition = (height / 2 - gui.getHeight() / 2);
        gui.setLocation((int) xPosition, (int) yPosition);
    }

    /**
     * Diese Methode zeigt ein PopUp Fenster mit Informationen an. Beim Starten
     * der Standard-Mode werden Hinweise zum Beta-Status der Software gegeben.
     * Wenn die Anwendung im Server-Mode gestartet unter Linux ausgef??hrt wird,
     * erh??lt der Anwender weitere. Hinweise
     */
    private void PopUp() {
        if (jRBtnStd.isSelected() == true) {
            JOptionPane.showMessageDialog(null, "This is the version 1.0b of the Openthinclient Avisor\r\nWe still have some difficulties with the DHCP scanner at the moment!", "Information", JOptionPane.INFORMATION_MESSAGE);
        }
        if (jRBtnServer.isSelected() == true && cVerwaltung.getWindows() == false) {
            JOptionPane.showMessageDialog(null, "In some versions of Linux there are problems to bound the ports: 67, 69 and 514", "Information", JOptionPane.INFORMATION_MESSAGE);
        }
        if (jRBtnClient.isSelected() == true) {
            JOptionPane.showMessageDialog(null, "UDP-scan results are currently not reliable", "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jRBtnStd = new javax.swing.JRadioButton();
        jRBtnServer = new javax.swing.JRadioButton();
        jRBtnClient = new javax.swing.JRadioButton();
        jBtnRun = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setFocusableWindowState(false);
        setResizable(false);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/openthinclient/advisor/logo.jpg"))); // NOI18N

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Welcome to the Openthinclient-Advisor! ");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Please choose the execution mode", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        buttonGroup1.add(jRBtnStd);
        jRBtnStd.setText("Standard-Mode");
        jRBtnStd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRBtnStdActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRBtnServer);
        jRBtnServer.setText("Server-Mode");
        jRBtnServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRBtnServerActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRBtnClient);
        jRBtnClient.setText("Client-Mode");
        jRBtnClient.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRBtnClientActionPerformed(evt);
            }
        });

        jBtnRun.setText("Run");
        jBtnRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnRunActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jRBtnStd)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jRBtnServer)
                        .addGap(66, 66, 66)
                        .addComponent(jBtnRun))
                    .addComponent(jRBtnClient))
                .addContainerGap(81, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jRBtnStd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRBtnServer)
                    .addComponent(jBtnRun))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRBtnClient)
                .addContainerGap(21, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(56, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(48, 48, 48))
            .addGroup(layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(11, 11, 11)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Methode des Run-Button, startet das Programm im jeweiligen Modi, welcher per Radio-Button ausgew??hlt wurde.
     * @param evt
     */
    private void jBtnRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnRunActionPerformed
        this.PopUp();
        cVerwaltung.showGUI();
        this.dispose();
    }//GEN-LAST:event_jBtnRunActionPerformed
    /**
     * Methode des Radio-Button zur Wahl der Standard-Mode. Ist Bestandteil einer Button-Group
     * @param evt
     */
    private void jRBtnStdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRBtnStdActionPerformed
        cVerwaltung.enterStandardMode();
    }//GEN-LAST:event_jRBtnStdActionPerformed
    /**
     * Methode des Radio-Button zur Wahl der Server-Mode. Ist Bestandteil einer Button-Group
     * @param evt
     */
    private void jRBtnServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRBtnServerActionPerformed
        cVerwaltung.enterServerMode();
    }//GEN-LAST:event_jRBtnServerActionPerformed
    /**
     * Methode des Radio-Button zur Wahl der Client-Mode. Ist Bestandteil einer Button-Group.
     * @param evt
     */
    private void jRBtnClientActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRBtnClientActionPerformed
        cVerwaltung.enterClientMode();
    }//GEN-LAST:event_jRBtnClientActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new Startscreen().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jBtnRun;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton jRBtnClient;
    private javax.swing.JRadioButton jRBtnServer;
    private javax.swing.JRadioButton jRBtnStd;
    // End of variables declaration//GEN-END:variables
}
