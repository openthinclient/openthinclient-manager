package org.openthinclient.advisor;

import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Die GUI-Klasse stellt die Hauptoberfl??che des openthinclient Advisors dar.
 * Sie bietet Bedienelemente zum Steuern der Anwendung.
 * 
 * @author Benedikt und Daniel
 */
public class GUI extends javax.swing.JFrame {

    /** Creates new form GUI */
    public GUI() {
        initComponents();
        jTxtAusgabe.setEditable(false);
        jTXTServerIP.setEnabled(false);
    }

    /**
     * Sperrt die Oberfl??che der GUI f??r s??mtliche Mausklicks und Tasteneingaben.
     * Diese Methode wird beispielsweise beim Aufruf des jFrames "jFrProxy" ben??tigt.
     */
    public void guiLocked() {
        this.setEnabled(false);
    }

    /**
     * l??scht die Textbox
     */
    public void clearTextBox() {
        jTxtAusgabe.setText("");
    }

    /**
     * schreibt einen String in Textbox
     * @param text2 String
     */
    public void WriteInTextBox(String text2) {
        jTxtAusgabe.setText(jTxtAusgabe.getText() + "\r\n" + text2);
    }

    /**
     * schreibt String in FAQ-BOX
     * @param text2 String
     */
    public void WriteInFAQBox(String text2) {
        jTextFAQ.setText(jTextFAQ.getText() + "\r\n" + text2);
    }

    /**
     * entsperrt den Start-Button
     */
    public void unlockStartButton() {
        jBtnStart.setEnabled(true);
    }

    /**
     * gibt den Inhalt der Textbox als String zur??ck
     * @return String
     */
    public String getTextBox() {
        String Ausgabe = jTxtAusgabe.getText();
        return Ausgabe;
    }

    /**
     * wechselt zur Log-Ansicht
     */
    public void showLog() {
        jTabbedPane1.setSelectedIndex(1);
    }

    /**
     * deaktiviert das Tab zum Wechsel der Ansicht
     */
    public void disableTab() {
        jTabbedPane1.setEnabled(false);
    }

    /**
     * aktiviert das Tab zum Wechsel der Ansicht
     */
    public void enableTab() {
        jTabbedPane1.setEnabled(true);
    }

    /**
     * wechselt in die Results ansicht
     */
    public void showResults() {
        jTabbedPane1.setSelectedIndex(0);
    }

    /**
     * aktiviert das Feld zur Eingabe der Server-IP
     */
    public void showServerIP() {
        jTXTServerIP.setEnabled(true);
    }

    /**
     * deaktiviert das Feld zur Eingabe der Server-IP
     */
    public void hintServerIP() {
        jTXTServerIP.setEnabled(false);
    }

    /**
     * liest die Server-IP aus jTXTServerIP aus und gibt diese als String zur??ck
     * @return IP des Servers als String
     */
    public String getServerIP() {
        String ServerIP = jTXTServerIP.getText();
        return ServerIP;
    }

    /**
     * setzt die GUI in den ServerMode
     */
    public void enterServerMode() {
        jBtnStart.setText("Stop Server");
        jBtnAbout.setEnabled(false);
        jBtnClearLog.setEnabled(false);
        jBtnLogfile.setEnabled(false);
        jBtnProxy.setEnabled(false);
        jBtnChangeMode.setEnabled(false);
    }

    /**
     * deaktiviert den Server-Mode in der GUI
     */
    public void disableServerMode() {
        jBtnStart.setText("Start");
        jBtnAbout.setEnabled(true);
        jBtnClearLog.setEnabled(true);
        jBtnLogfile.setEnabled(true);
        jBtnProxy.setEnabled(true);
        jBtnChangeMode.setEnabled(true);
    }

    /**
     * setzt das Result auf pass
     * @param x x-koordinate des Results
     * @param y y-koordinate des Results
     */
    public void setResultsPass(int x, int y) {
        jTable.setValueAt("pass", x, y);
    }

    /**
     * schreibt in die Tabelle
     * @param text String der geschrieben werden soll
     * @param x x-koordinate an die geschrieben werden soll
     * @param y x-koordinate des die gesxchrieben werden soll
     */
    public void writeInTable(String text, int x, int y) {
        jTable.setValueAt(text, x, y);
    }

    /**
     * setzt das Result auf false
     * @param x x-koordinate des Results
     * @param y y-koordinate des Results
     */
    public void setResultFalse(int x, int y) {
        jTable.setValueAt("failed", x, y);
    }

    /**
     * setzt alle Results im Standard-Mode auf "failed"
     */
    public void setResultsFalse() {
        for (int i = 0; i < 8; i++) {
            jTable.setValueAt("failed", i, 1);
        }
    }

    /**
     * setzt alle Results im Server-Mode auf "not tested"
     */
    public void setResultsNotTestedServer() {
        for (int i = 0; i < 15; i++) {
            jTable.setValueAt("not tested", i, 1);
        }
    }

    /**
     * setzt alle Results im Standard-Mode auf "not tested"
     */
    public void setResultsNotTestedStandard() {
        for (int i = 0; i < 8; i++) {
            jTable.setValueAt("not tested", i, 1);
        }
    }

    /**
     * setzt alle Results im Server-Mode auf "false"
     */
    public void setResultsFalseServer() {
        for (int i = 0; i < 15; i++) {
            jTable.setValueAt("failed", i, 1);
        }
    }

    /**
     * l??scht alle Eintr??ge der Tabelle
     */
    public void clearTable() {
        for (int i = 0; i < 15; i++) {
            jTable.setValueAt("", i, 1);
            jTable.setValueAt("", i, 0);
        }
    }

    /**
     * l??scht den Inhalt der FAQ-Box
     */
    public void clearFAQ() {
        jTextFAQ.setText(null);
        jTextFAQ.setText("Details:");
    }

    /**
     * setzt das Result der Port-Test im Standard-Mode auf "not tested"
     */
    public void setResultsNotTested() {
        jTable.setValueAt("not tested", 3, 1);
        jTable.setValueAt("not tested", 4, 1);
        WriteInFAQBox("To check the unassigned ports change execution to server-mode");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jBtnStart = new javax.swing.JButton();
        jBtnLogfile = new javax.swing.JButton();
        jBtnProxy = new javax.swing.JButton();
        jBtnClearLog = new javax.swing.JButton();
        jBtnAbout = new javax.swing.JButton();
        jBtnChangeMode = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextFAQ = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTxtAusgabe = new javax.swing.JTextArea();
        jTXTServerIP = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Openthinclient Advisor 1.0b");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setResizable(false);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Control"));
        jPanel2.setFocusable(false);

        jBtnStart.setText("Start");
        jBtnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnStartActionPerformed(evt);
            }
        });

        jBtnLogfile.setText("Save logfile");
        jBtnLogfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnLogfileActionPerformed(evt);
            }
        });

        jBtnProxy.setText("Proxy settings");
        jBtnProxy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnProxyActionPerformed(evt);
            }
        });

        jBtnClearLog.setText("Clear Log");
        jBtnClearLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnClearLogActionPerformed(evt);
            }
        });

        jBtnAbout.setText("About");
        jBtnAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAboutActionPerformed(evt);
            }
        });

        jBtnChangeMode.setText("Change Execution-Mode");
        jBtnChangeMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnChangeModeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jBtnChangeMode, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)
                    .addComponent(jBtnClearLog, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE))
                .addGap(74, 74, 74)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jBtnLogfile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jBtnStart, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE))
                .addGap(63, 63, 63)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jBtnAbout, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jBtnProxy, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBtnStart)
                    .addComponent(jBtnProxy)
                    .addComponent(jBtnChangeMode))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBtnLogfile)
                    .addComponent(jBtnClearLog)
                    .addComponent(jBtnAbout))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/openthinclientadvisor/logo.jpg"))); // NOI18N
        jLabel1.setFocusable(false);

        jTabbedPane1.setFocusable(false);

        jTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Sun Java 1.6 version", "not tested"},
                {"Minimum 500 MB, recommended 1 GB RAM Linux", "not tested"},
                {"Minimum 1 GB, recommended 2 GB of free harddisc space", "not tested"},
                {"Several unassigned TCP-Ports: 1098, 1099, 2069, 3873, 4444, 4445, 8009, 8080, 8083, 10389", "not tested"},
                {"Several unassigned UDP-Ports: 67, 69, 514, 2069, 4011", "not tested"},
                {"Only one configured NIC besides loopback", "not tested"},
                {"Working internet connection", "not tested"},
                {"A running DHCP server ", "not tested"},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Requirements", "Test-Result"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable.setColumnSelectionAllowed(true);
        jTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(jTable);
        jTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable.getColumnModel().getColumn(0).setResizable(false);
        jTable.getColumnModel().getColumn(0).setPreferredWidth(600);
        jTable.getColumnModel().getColumn(1).setResizable(false);

        jTextFAQ.setColumns(20);
        jTextFAQ.setRows(5);
        jTextFAQ.setText("FAQ");
        jScrollPane3.setViewportView(jTextFAQ);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE))
                .addGap(18, 18, 18))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Results", jPanel3);

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jPanel1.setMaximumSize(new java.awt.Dimension(2767, 2767));

        jTxtAusgabe.setColumns(20);
        jTxtAusgabe.setRows(5);
        jScrollPane1.setViewportView(jTxtAusgabe);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 694, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Log", jPanel1);

        jTXTServerIP.setText("Enter Server IP");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(195, 195, 195)
                        .addComponent(jLabel1))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 721, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(20, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(286, Short.MAX_VALUE)
                .addComponent(jTXTServerIP, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(282, 282, 282))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(26, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(5, 5, 5)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 434, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTXTServerIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Die Methode des Start-Button sperrt die GUI und f??hrt die Methode
     * TestStarten der Klasse cVerwaltung aus welche den Test startet.
     * @param evt
     */
    private void jBtnStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnStartActionPerformed
        this.guiLocked();
        try {
            cVerwaltung.TestStarten();
        } catch (SocketException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jBtnStartActionPerformed
    /**
     * sperrt die GUI und f??hrt die Methode setProxy der Klasse cVerwaltung aus
     * @param evt
     */
    private void jBtnProxyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnProxyActionPerformed
        this.guiLocked();
        cVerwaltung.setProxy();
    }//GEN-LAST:event_jBtnProxyActionPerformed
    /**
     * sperrt die GUI und f??hrt die Methode SaveLog der Klasse cVerwaltung aus
     * @param evt
     */
    private void jBtnLogfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnLogfileActionPerformed
        this.guiLocked();
        cVerwaltung.SaveLog();
    }//GEN-LAST:event_jBtnLogfileActionPerformed
    /**
     * l??scht den Inhalt von jTxtAusgabe (Logfile)
     * @param evt
     */
    private void jBtnClearLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnClearLogActionPerformed
        jTxtAusgabe.setText(null);
    }//GEN-LAST:event_jBtnClearLogActionPerformed
    /**
     * schlie??t die GUI und erzeugt ein neues Objekt von Startscreen
     * @param evt
     */
    private void jBtnChangeModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnChangeModeActionPerformed
        this.dispose();
        Startscreen startscreen = new Startscreen();
        startscreen.setVisible(true);
    }//GEN-LAST:event_jBtnChangeModeActionPerformed
    /**
     * sperrt die GUI, erzeugt ein Objekt von About und macht dieses sichtbar
     * @param evt
     */
    private void jBtnAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAboutActionPerformed
        this.setEnabled(false);
        About About = new About();
        About.setVisible(true);
    }//GEN-LAST:event_jBtnAboutActionPerformed
// Unn??tige Main methode auskommentiert
    /**
     * @param args the command line arguments
     */
    /*public static void main(String args[]) {
    java.awt.EventQueue.invokeLater(new Runnable() {
    public void run() {
    new GUI().setVisible(true);



    }
    });
    }*/
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtnAbout;
    private javax.swing.JButton jBtnChangeMode;
    private javax.swing.JButton jBtnClearLog;
    private javax.swing.JButton jBtnLogfile;
    private javax.swing.JButton jBtnProxy;
    private javax.swing.JButton jBtnStart;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextField jTXTServerIP;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable;
    private javax.swing.JTextArea jTextFAQ;
    private javax.swing.JTextArea jTxtAusgabe;
    // End of variables declaration//GEN-END:variables
}
