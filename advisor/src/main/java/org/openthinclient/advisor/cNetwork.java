package org.openthinclient.advisor;

import org.openthinclient.manager.util.http.config.NetworkConfiguration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Die Klasse cNetwork beinhaltet Methoden zur Pr??fung der Netzwerkfunktionalit??t des Systems.
 * Es werden in der Klasse cNetwork auch die Daten f??r den zu verwendenden Proxy Server gespeichert.
 *
 * @author Benedikt Diehl und Daniel Vogel
 */
public class cNetwork {

    private NetworkConfiguration.ProxyConfiguration proxyConfiguration;

    /**
     * Die Variable hostname dient als Hilfsvariable und h??lt den Hostname des
     * Systems. Dieser Hostname wird mithilfe der Methode getHostname() ermittelt und gesetzt.
     */
    private String hostname = "";
    /**
     * Die internet Variable dient als Hilfsvariable und speichert das Ergebnis
     * der Methode verifyInternetConnection() als Boolean wert.
     */
    private static boolean internet = false;
    /**
     * Die nics Variable dient als Hilfsvariable und speichert das Ergebnis der
     * Methode Networkadapter() als Boolean wert.
     */
    private static boolean nics = false;
    /**
     * Die Variable serverrun dient als Hilfsvariable und wird beim Start des
     * Serverpr??fmodus gesetzt. Die serverrun Variable wird ben??tigt um den
     * Servermode wieder sauber beenden.
     */
    private static boolean serverrun = false;
    /**
     * Die MACAddress Variable dient als Hilfsvariable und h??lt eine MAC-Adresse
     * vor. Die MAC Adresse wird im Ablauf der Methode Networkadapter()
     * ermittelt und in die Variable geschrieben. Diese Variable wird im
     * Pr??fungsverlauf zur Ermittlung des DHCP Servers ben??tigt.
     */
    private static String MACAddress;

    /**
     * Im Standard Konstruktor der Klasse cNetwork wird der im System gesetzte
     * Proxyserver ermittelt und die Daten f??r den Programmablauf gespeichert.
     * Dies geschieht mithilfe der Methode getSystemProxy die den im System
     * gesetzten Proxyserver ermittelt. Danach wird mit den Methoden
     * System.setProperty und setProxyValues die ermittelte Proxyeinstellung
     * im Programm gesetzt. Sollte im System kein Proxyserver konfiguriert sein,
     * so werden die Daten leer initialisiert.
     */
    public cNetwork() {
        proxyConfiguration = this.getSystemProxy();
        if (proxyConfiguration == null) {
            proxyConfiguration = new NetworkConfiguration.ProxyConfiguration();
        }
    }

    /**
     * Determine the system proxy settings.
     * @return a {@link org.openthinclient.manager.util.http.config.NetworkConfiguration.ProxyConfiguration} or <code>null</code> if none could be found.
     */
    private NetworkConfiguration.ProxyConfiguration getSystemProxy() {
        try {

            System.setProperty("java.net.useSystemProxies", "true");
            List<Proxy> proxies = ProxySelector.getDefault().select(
                    URI.create("http://www.openthinclient.org/"));


            final Optional<NetworkConfiguration.ProxyConfiguration> first = proxies.stream()
                    .map(Proxy::address)
                    .filter(address -> address != null && address instanceof InetSocketAddress)
                    .map(address -> (InetSocketAddress) address)
                    .map(addr -> {
                        final NetworkConfiguration.ProxyConfiguration config = new NetworkConfiguration.ProxyConfiguration();
                        config.setEnabled(true);
                        config.setHost(addr.getHostName());
                        config.setPort(addr.getPort());
                        return config;
                    }).findFirst();

            if (first.isPresent()) {
                return first.get();
            }
            return null;
        } catch (Exception e) {
            // doing nothing
            return null;
        }

    }

    /**
     * Die Methode verifyInternetConnection pr??ft, ob das System eine Verbindung mit dem
     * Internet aufbauen kann. Dazu wird eine Instanz der Klasse cInetConnection
     * initiiert und die Methode checkConnectivity() aufgerufen. Diese
     * setzt als Pr??fungsergebnis die Variable Internet auf true oder false.
     * Die Methode gibt das Pr??fungsergebnis als String zur??ck.
     *
     * @return
     * Der R??ckgabeparameter gibt das Pr??fungsergebnis als String zur??ck.
     * Der String enth??lt die Aussage ob eine Internetverbindung m??glich ist oder nicht.
     */
    public String verifyInternetConnection() {
        internet = false;
        CheckInternetConnection checker = new CheckInternetConnection();
        checker.setProxyConfiguration(proxyConfiguration);
        internet = checker.checkConnectivity();
        String Ergebnis = " www.openthinclient.org is not reachable \r\n" + " Internet connectivity not present \r\n" + " Please check your network connection and the proxy settings \r\n";
        if (internet) {
            Ergebnis = " www.openthinclient.org is reachable \r\n" + " Internet connectivy present \r\n";
            return Ergebnis;
        }
        return Ergebnis;
    }

    /**
     * F??hrt eine TCP-Scann mit den Ports aus der Datei "Ports.ini" durch.
     * Mithilfe des Parameters kann der Zielrechner in Form einer IP-Adresse
     * oder mit dem Hostnamen angegeben werden.
     *
     * @param ServerIP IP-Adresse des Servers als String (192.168.1.1) oder Hostname
     */
    public static void PortScanner(String ServerIP) {
        String host = ServerIP;
        try {
            String[] splittArray = cReadWriteSplit.readSplitFile("ports.ini", "\\;");
            for (int i = 0; i < splittArray.length; i++) {
                int port = Integer.parseInt(splittArray[i]);
                cPortScanner curr = new cPortScanner(host, port);
                Thread th = new Thread(curr);
                th.start();
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException ex) {
//                }
            }

        } catch (Exception ex) {
            // FIXME some better error handling would be nice here
            ex.printStackTrace();
        }


    }

    /**
     * Beendet den Serverdienst auf allen Ports die in der Datei "ports.ini" stehen,
     * indem ein Portscan auf "Localhost" durchgef??hrt wird.
     */
    public void KillServer() {
        serverrun = false;
        try {
            String[] splittArray = cReadWriteSplit.readSplitFile("ports.ini", "\\;");
            for (int i = 0; i < splittArray.length; i++) {
                int port = Integer.parseInt(splittArray[i]);
                cKillServer curr = new cKillServer(port);
                Thread th = new Thread(curr);
                th.start();
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException ex) {
//                }
            }

        } catch (Exception ex) {
            // FIXME some better error handling would be nice here
            ex.printStackTrace();
        }
    }

    /**
     * Diese Methode startet den Server-Dienst welcher auf den Ports nach
     * eingehenden Verbindungen lauscht. Hierbei wird f??r jeden Port aus der
     * Datei "ports.ini" ein Objekt angelegt und als Thread gestartet.
     */
    public void RunServer() {
        serverrun = true;
        try {
            String[] splittArray = cReadWriteSplit.readSplitFile("ports.ini", "\\;");
            for (String aSplittArray : splittArray) {
                int port = Integer.parseInt(aSplittArray);
                Thread t1 = new Thread(new cServer(port));
                t1.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(cNetwork.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Diese Methode fr??gt alle im System installierten Netzwerkadapter ab und
     * gibt diese mit Name und zugeh??riger IP-Adresse als String zur??ck.
     * Localhost-Adapter werden dabei ausgeblendet. Die Methode wird im Server-Mode
     * f??r die Anzeige der NICs im Log-Fenster verwendet.
     * @return String "Network-Interface:" +(Adaptername) + "IP:" + IPAdresse
     */
    public static String getLocalIps() {
        String LocalIP = "";
        try {
            Enumeration<NetworkInterface> interfaceNIC = NetworkInterface.getNetworkInterfaces();
            // Alle Schnittstellen durchlaufen
            while (interfaceNIC.hasMoreElements()) {
                //Elemente abfragen und ausgeben
                NetworkInterface n = interfaceNIC.nextElement();
                // Adressen abrufen
                Enumeration<InetAddress> addresses = n.getInetAddresses();
                // Adressen durchlaufen
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.getClass().equals(Inet4Address.class) && n.getName().contains("lo") == false) {
                        LocalIP = LocalIP + String.format("Network-Interface: %s (%s)" + "\r\n", n.getName(), n.getDisplayName());
                        LocalIP = LocalIP + String.format("IP: %s" + "\r\n\r\n", address.getHostAddress());
                    }
                }
            }

        } catch (SocketException ex) {
            Logger.getLogger(cNetwork.class.getName()).log(Level.SEVERE, null, ex);
        }
        return LocalIP;
    }

    /**
     * Die Methode getHostname() liefert den Hostname des Systems als String zur??ck.
     * Der Hostname wird mit der Java Bibliothek java.net.InetAddress und deren
     * Methode getLocalHost().getHostName() ermittelt.
     * 
     * @return
     * Der Returnparameter liefert den Hostnamen des Systems zur??ck.
     */
    public String getHostname() {
        try {
            hostname = "" + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Logger.getLogger(cNetwork.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hostname;
    }

    /**
     * Diese Methode gibt den Wert der Variable internet zur??ck.
     *
     * @return
     * Der R??ckgabewert liefert den Inhalt der Variable internet der Klasse cNetwork zur??ck.
     */
    public static boolean internet() {
        return internet;
    }

    /**
     * Die Methode nics() gibt den Zustand der boolean variable nics zur??ck
     * welche vom Testergebnis der Methode cNetworkAdapters abh??ngt.
     *
     * @return
     * Der R??ckgabewert enth??lt den Wert der Variable nics der Klasse cNetwork.
     */
    public static boolean nics() {
        return nics;
    }

    /**
     * Mit der Methode Networkadapter() werden die im System installierten
     * Netzwerkkarten ermittelt. Im Ablauf der Klasse wird ein Objekt der
     * Klasse cNetworkAdapters erstellt und die Methode main() aufgerufen.
     * Die main() Methode der Klasse cNetworkAdapters liefert einen String
     * mit Adaptername und IP Adresse der gefundenen Netzwerkadapter zur??ck.
     * Es werden zus??tzlich mit den Methoden getNetworkOk() und getMAC()
     * weitere Informationen der Klasse cNetworkAdapters abgerufen und f??r
     * den weiteren Pr??fungsverlauf in die Variablen nics und MACAddress
     * geschrieben.
     *
     * @return
     * Der return Parameter liefert einen String mit dem Pr??fungsergebnis der Methode main() der Klasse cNetworkAdapters zur??ck.
     * Dieser String enth??lt die im System Installierten Netzwerkkarten und deren IPv4 Adresse.
     *
     * @throws SocketException
     */
    public static String Networkadapter() throws SocketException {
        String Ergebnis = "";
        cNetworkAdapters adapters = new cNetworkAdapters();
        Ergebnis = adapters.main();
        nics = adapters.getNetworkOk();
        MACAddress = adapters.getMAC();
        return Ergebnis;
    }

    /**
     * Die Methode dhcpChecker() ermittelt mit der Methode main() der Klasse
     * cDHCPClient den DHCP-Server im Netzwerk. Beim Ablauf der Methode
     * dhcpChecker() wird die main() Methode der Klasse cDHCPClient ausgef??hrt
     * und deren Pr??fungsergebnis mit der Methode getErgebnis() abgerufen.
     * Die Methode getErgebnis() liefert einen String zur??ck der die Antwort
     * eines oder mehrerer im Netzwerk gefundener DHCP Server darstellt.
     *
     * @return
     * Im Return Parameter wird das Ergebnis der DHCP Pr??fung der main() Methode der Klasse cDHCPClient an die Oberfl??che zur??ck gegeben.
     * Dieses Ergebnis enth??lt die Antwort/en des/der DHCP Server des Netzwerks.
     */
    public String dhcpChecker() {
        String Ergebnis = "It was not possible to check your network for existing DHCP servers";
        cDHCPClient.main(MACAddress);
        Ergebnis = cDHCPClient.getErgebnis();
        return Ergebnis;
    }

    /**
     * Die Methode getServerrun() gibt den Zustand der Variable serverun zur??ck.
     * Wenn der Serverdienst l??uft, hat diese den Zustand "true".
     * @return true if server runs
     */
    public static boolean getServerrun() {
        return serverrun;
    }

    public void setProxyConfiguration(NetworkConfiguration.ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    public NetworkConfiguration.ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }
}
