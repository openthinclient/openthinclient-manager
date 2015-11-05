package org.openthinclient.advisor;

// Example dhcp client simulation written using JDHCP API.
// Jason Goldschmidt, Nick Stone 10/08/1998
// last updated 9/06/1999
import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.StringTokenizer;
import edu.bucknell.net.JDHCP.*;

//----------------------------------------------------------------
// dhcpclient simulation program
//----------------------------------------------------------------
/**
 * Die Klasse cDHCPClient ist Teil des jDHCP Projekt Pakets und wurde auf die
 * Anforderungen des Programms openthinclient Advisor angepasst.
 * Die Aufgabe der Klasse cDHCPClient ist es mit Hilfe der Klassen
 * des Pakets edu.bucknell.net.JDHCP eine DHCP Anfrage ins Netzwerk zu senden
 * und die Antwort abzufangen und auszuwerten.
 * Das jDHCP Projekt wurde von der Projektseite http://www.eg.bucknell.edu/~jgoldsch/dhcp/ abgerufen.
 *
 * @author Jason Goldschmidt, Nick Stone 10/08/1998
 */
public class cDHCPClient {

    /**
     * Variable zur Speicherung des Pr??fungsergebnisses.
     * Die Variable wird mit dem Text "DHCP request timed out while sending the DHCP message" initialisiert.
     * Sollte im Pr??fungsverlauf ein DHCP Server antworten werden dessen Informationen in die Variable gespeichert.
     */
    private static String Ergebnis = "DHCP request timed out while sending the DHCP message";
    /**
     * Hilfsvariable zur Pr??fung des Testergebnisses.
     */
    private static Boolean works = false;

    /**
     * Methode main() startet die Pr??fung.
     * Es wird versucht einen DHCP Server zu ermitteln indem ein DHCP Discover Paket ins Netzwerk gesandt wird.
     *
     * @param pMAC
     * Der Parameter pMAC enth??lt die MAC Adresse eines im System ermittelten Netzwerkadapters.
     */
    public static void main(String pMAC) {

        /*********************************************************
         * Starting dhcpclient sample application written using *
         * JDHCP v1.1.1. Learn more about the JDHCP project at  *
         * http://www.eg.bucknell.edu/~jgoldsch/dhcp/           *
         * JDHCP is an API for writting Java(tm) applications   *
         * that speak the Dynamic Host Configuration Protocol   *
         * Note: dhcpclient is merely a simulation of what a    *
         * DHCP client does within its lifecycle as specified   *
         * in RFC 2131 and 2132. This application and JDHCP was *
         * co-authored by Jason Goldschmidt and Nick Stone.     *
         *********************************************************/
        String hwaddr = new String();
        hwaddr = pMAC;


        try {
            DHCPSocket mySocket =
                    new DHCPSocket(DHCPMessage.CLIENT_PORT); // create socket
            // Use port 67 if you are configuring as a bootp relay agent
            //	    DHCPSocket mySocket = new DHCPSocket(67);

            // Put the hardware address of the computer you are using here.

            Client x = new Client(mySocket, hwaddr);

            x.start();  // start the client. Sit back and enjoy the simulation fun
            Thread.sleep(5000);
            x.stop();
        } catch (java.net.BindException e1) {
            Ergebnis = "DHCP \r\n";
            Ergebnis += "Socket Bind Error: Another process is bound to this port \r\n";
            Ergebnis += "otherwise you do not have access to bind a process to this port.";
        } catch (Exception e2) {
        }
    }

    /**
     * Setzt die Variable Ergebnis.
     * Wird ben??tigt um aus der Klasse Client auf diese Variable zugreifen zu k??nnen.
     * @param Erg
     * Der Parameter Erg enth??lt den neuen Wert f??r die Variable Ergebnis.
     */
    public static void setErgebnis(String Erg) {
        cDHCPClient.Ergebnis = Erg;
    }

    /**
     * ??bergibt den Inhalt der Variable Ergebnis.
     * Wird ben??tigt um die ermittelten Pr??fungsergebnisse auf der Oberfl??che auszugeben.
     * @return
     * Der return Parameter enth??lt den Inhalt der Variable Ergebnis.
     */
    public static String getErgebnis() {
        return cDHCPClient.Ergebnis;
    }

    /**
     * Setzt die Variable works.
     * Wird ben??tigt um aus der Klasse Client auf diese Variable zugreifen zu k??nnen.
     * @param w
     * Der Parameter Erg enth??lt den neuen Wert f??r die Variable works.
     */
    public static void setWorks(boolean w) {
        works = w;
    }

    /**
     * ??bergibt den Inhalt der Variable works.
     * Wird ben??tigt um die ermittelten Pr??fungsergebnisse auf der Oberfl??che auszugeben.
     * @return
     * Der return Parameter enth??lt den Inhalt der Variable works.
     */
    public static boolean getWorks() {
        return works;
    }
}

/**
 * Die Klasse Client ist Teil des jDHCP Projekt Pakets
 * und wurde auf die Anforderungen des Programms openthinclient Advisor angepasst.
 * Die Anpassungen betreffen die Ausgabe der Serverantworten und die Auswertung einer erhaltenen Leastime
 * welche f??r diese Pr??fung nicht relevant ist.
 * @author Jason Goldschmidt, Nick Stone 10/08/1998
 */
class Client extends Thread {

    DHCPSocket bindSocket = null;
    byte hwaddr[] = new byte[16];
    InetAddress serverIP;
    int portNum;
    boolean gSentinel;
    // DHCP option constants
    static final int REQUESTED_IP = 50;
    static final int LEASE_TIME = 51;
    static final int MESSAGE_TYPE = 53;
    static final int T1_TIME = 58;
    static final int T2_TIME = 59;
    /**
     * Hilfsvariable zur Erstellung des Pr??fungsergebnisses.
     */
    private String Ergebnis = "";

    //----------------------------------------------------------------
    // Constructor for Client class
    //----------------------------------------------------------------
    public Client(DHCPSocket inSocket, String inHwaddr) {
        bindSocket = inSocket;
        hwaddr = ChaddrToByte(inHwaddr);
        this.setName(inHwaddr);   // set thread name
        // note: a DHCPMessage can take no parameters and these values here
        // would still be the default.
        serverIP = DHCPMessage.BROADCAST_ADDR;  // localnet broadcast
        portNum = DHCPMessage.SERVER_PORT; // default DHCP server port
        gSentinel = true;
    }

    //----------------------------------------------------------------
    // Thread main
    //----------------------------------------------------------------
    @Override
    public void run() {
        try {
            DHCPMessage offerMessageIn = new DHCPMessage(serverIP, portNum);
            offerMessageIn = SendDiscover();
            DHCPMessage messageIn = offerMessageIn;
            byte[] messageType = new byte[1];

            while (gSentinel) {
                messageType = messageIn.getOption(MESSAGE_TYPE);
                switch (messageType[0]) {
                    case DHCPMessage.OFFER:
                        this.Ergebnis = this.Ergebnis + this.getName() + " received a DHCPOFFER for "
                                + bytesToString(messageIn.getYiaddr()) + " \r\n";

                        //messageIn.printMessage();
                        messageIn = SendRequest(messageIn);
                        break;
                    case DHCPMessage.ACK:
                        byte[] t1 = new byte[4];
                        byte[] t2 = new byte[4];
                        t1 = messageIn.getOption(T1_TIME);
                        t2 = messageIn.getOption(T2_TIME);
                        this.Ergebnis = this.Ergebnis + "Binding to IP address: " + bytesToString(messageIn.getYiaddr()) + " \r\n";
                        cDHCPClient.setErgebnis(Ergebnis);
                        cDHCPClient.setWorks(true);
                        gSentinel = false;
                        break;
                    case DHCPMessage.NAK:
                        System.out.println(this.getName());
                        System.out.print("Revieded DHCPNAK... ");
                        messageIn = SendDiscover();
                        break;
                    default:
                        break;
                }


            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    // -------------------------------------------------------------------
    // dhcpclient message send/recieve functions
    // -------------------------------------------------------------------
    // Sends DHCP Discover Message and returns the recieved Offer Message
    private DHCPMessage SendDiscover() {
        Random ranXid = new Random();
        DHCPMessage messageIn = new DHCPMessage(serverIP, portNum);
        DHCPMessage messageOut = new DHCPMessage(serverIP, portNum);
        try {
            // fill DHCPMessage object
            messageOut.setOp((byte) 1);    //setOp Method being used
            messageOut.setHtype((byte) 1);
            messageOut.setHlen((byte) 6);
            messageOut.setHops((byte) 0);
            messageOut.setXid(ranXid.nextInt()); // should be a random int
            messageOut.setSecs((short) 0);
            messageOut.setFlags((short) 0);
            messageOut.setChaddr(hwaddr); // set globaly defined hwaddr

            byte[] opt = new byte[1];
            opt[0] = (byte) DHCPMessage.DISCOVER;

            messageOut.setOption(MESSAGE_TYPE, opt);

            bindSocket.send(messageOut); // send DHCPDISCOVER

            this.Ergebnis = "Sending DHCPDISCOVER with MAC Address: " + this.getName() + " \r\n";


            boolean sentinal = true;
            while (sentinal) {
                if (bindSocket.receive(messageIn)) {
                    if (messageOut.getXid() == messageIn.getXid()) {
                        sentinal = false;
                    } else {
                        bindSocket.send(messageOut);
                    }
                } else {
                    bindSocket.send(messageOut);
                }
            }
        } catch (SocketException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println("dhcpclient::SendDiscover:" + e);
        }  // end catch
        return messageIn;

    }

    // Sends DHCPREQUEST Message and returns server message
    private DHCPMessage SendRequest(DHCPMessage offerMessageIn) {
        DHCPMessage messageOut = new DHCPMessage(serverIP, portNum);
        DHCPMessage messageIn = new DHCPMessage(serverIP, portNum);
        try {
            messageOut = offerMessageIn;
            messageOut.setOp((byte) 1);  // setup message to send a DCHPREQUEST
            byte[] opt = new byte[1];
            opt[0] = (byte) DHCPMessage.REQUEST;
            messageOut.setOption(MESSAGE_TYPE, opt); // change message type
            messageOut.setOption(REQUESTED_IP, offerMessageIn.getYiaddr());
            bindSocket.send(messageOut); // send DHCPREQUEST
            this.Ergebnis = this.Ergebnis + this.getName() + " sending DHCPREQUEST for "
                    + bytesToString(offerMessageIn.getOption(REQUESTED_IP)) + " \r\n";
            cDHCPClient.setErgebnis(Ergebnis);
            boolean sentinal = true;
            while (sentinal) {
                if (bindSocket.receive(messageIn)) {
                    if (messageOut.getXid() == messageIn.getXid()) {
                        sentinal = false;
                    } else {
                        bindSocket.send(messageOut);
                    }
                } else {
                    bindSocket.send(messageOut);
                }
            }
        } catch (SocketException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }  // end catch
        return messageIn;
    }

    // Sends DHCPRENEW message and returns server message
    private DHCPMessage ReNew(DHCPMessage offerMessageIn) {
        String server_IP = bytesToString(offerMessageIn.getSiaddr());
        DHCPMessage messageOut = null;
        try {
            messageOut =
                    new DHCPMessage(InetAddress.getByName(server_IP),
                    portNum); // unicast

        } catch (UnknownHostException ex) {
            System.err.println(ex);
        }
        DHCPMessage messageIn = new DHCPMessage(serverIP, portNum);
        try {
            messageOut = offerMessageIn;
            messageOut.setOp((byte) 1);  // setup message to send a DCHPREQUEST
            byte[] opt = new byte[1];
            opt[0] = (byte) DHCPMessage.REQUEST;
            messageOut.setOption(MESSAGE_TYPE, opt); // change message type
            // must set ciaddr
            messageOut.setCiaddr(offerMessageIn.getYiaddr());

            int so_timeout = bindSocket.getSoTimeout() / 1000;
            long t1 = byteToLong(offerMessageIn.getOption(T1_TIME));
            long t2 = byteToLong(offerMessageIn.getOption(T2_TIME));

            int elpstime = 1;

            bindSocket.send(messageOut); // send DHCPREQUEST
            boolean sentinal = true;
            while (sentinal) {
                if (((elpstime * so_timeout) + t1) >= t2) {
                    System.out.print(this.getName());
                    System.out.println(" rebinding, T1 has ran out...");
                    messageIn = ReBind(offerMessageIn);
                    break;
                }
                if (bindSocket.receive(messageIn)) {
                    sentinal = false;
                    break;
                } else {
                    bindSocket.send(messageOut);
                    elpstime++;
                }
            }
        } catch (SocketException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }  // end catch
        return messageIn;
    }

    // Sends DHCPREBIND message, returns server message
    private DHCPMessage ReBind(DHCPMessage offerMessageIn) {
        DHCPMessage messageOut = new DHCPMessage(serverIP, portNum); //broadcast
        DHCPMessage messageIn = new DHCPMessage(serverIP, portNum);
        try {
            messageOut = offerMessageIn;

            messageOut.setOp((byte) 1);  // setup message to send a DCHPREQUEST
            byte[] opt = new byte[1];
            opt[0] = (byte) DHCPMessage.REQUEST;
            messageOut.setOption(MESSAGE_TYPE, opt); // change message type

            messageOut.setCiaddr(offerMessageIn.getYiaddr());
            // must set ciaddr
            long leaseTime = byteToLong(offerMessageIn.getOption(LEASE_TIME));
            long t2 = byteToLong(offerMessageIn.getOption(T2_TIME));
            int so_timeout = bindSocket.getSoTimeout() / 1000;
            int elpstime = 1;

            bindSocket.send(messageOut); // send DHCPREQUEST
            boolean sentinal = true;
            while (sentinal) {
                if (((elpstime * so_timeout) + t2) >= leaseTime) {
                    System.out.print(this.getName());
                    System.out.print(" is sending DHCPRELEASE, T2 has ran out ");
                    System.out.println("shuttingdown.");
                    SendRelease(offerMessageIn);
                    break;
                }
                if (bindSocket.receive(messageIn)) {
                    if (messageOut.getXid() == messageIn.getXid()) {
                        sentinal = false;
                    } else {
                        bindSocket.send(messageOut);
                        elpstime++;
                    }
                } else {
                    bindSocket.send(messageOut);
                    elpstime++;
                }
            }

        } catch (Exception e) {
            System.err.println(e);
        }

        return messageIn;
    }

    // Sends DHCPRELEASE message, returns nothing
    private void SendRelease(DHCPMessage inOfferMessage) {
        DHCPMessage messageOut = new DHCPMessage(serverIP, portNum);
        try {
            messageOut = inOfferMessage;
            messageOut.setOp((byte) 1);  // setup message to send a DCHPREQUEST
            byte[] opt = new byte[1];
            opt[0] = (byte) DHCPMessage.RELEASE;
            messageOut.setOption(MESSAGE_TYPE, opt); // change message type

            bindSocket.send(messageOut); // send DHCPREQUEST
            gSentinel = false;
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    // ------------------------------------------------------------------
    // start dhcpclient Util functions
    // ------------------------------------------------------------------
    // Converts byte[4] => long
    long byteToLong(byte inB[]) {
        long ttime = (((char) inB[0] * (256 * 256 * 256))
                + ((char) inB[1] * (256 * 256))
                + ((char) inB[2] * (256))
                + (char) inB[3]);
        return ttime;
    }

    // Converts byte[4] => Strings
    String bytesToString(byte inB[]) {
        String st = new String();
        for (int n = 0; n < 4; n++) {
            st += (int) ((char) inB[n] % 256);
            if (n < 3) {
                st += ".";
            }
        }
        return st;
    }

    // Converts the Chaddr String => byte[15]
    private byte[] ChaddrToByte(String inChaddr) {
        StringTokenizer token = new StringTokenizer(inChaddr, ":");
        Integer tempInt = new Integer(0);
        byte outHwaddr[] = new byte[16];
        int temp;
        int i = 0;
        while (i < 6) {
            temp = tempInt.parseInt(token.nextToken(), 16);
            outHwaddr[i] = (byte) temp;
            i++;
        }
        return outHwaddr;



    }
}
