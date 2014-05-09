
// 	$Id: DHCPSocket.java,v 1.3 1999/09/07 03:00:44 jgoldsch Exp $	

package edu.bucknell.net.JDHCP;

import java.net.*;

/**
 * This class represents a Socket for sending DHCP Messages
 * @author Jason Goldschmidt 
 * @version 1.1.1  9/06/1999
 * @see java.net.DatagramSocket
 */


public class DHCPSocket extends DatagramSocket  {

    static protected int PACKET_SIZE = 1500; // default MTU for ethernet
    private int defaultSOTIME_OUT = 3000; // 3 second socket timeout
    private DatagramSocket gSocket = null;
    /** 
     * Constructor for creating DHCPSocket on a specific port on the local
     * machine. 
     * @param inPort the port for the application to bind.
     */

    public DHCPSocket (int inPort) throws SocketException {
	
	super(inPort);
	setSoTimeout(defaultSOTIME_OUT); // set default time out
    }
    
    /**
     * Sets the Maximum Transfer Unit for the UDP DHCP Packets to be set.
     * Default is 1500, MTU for Ethernet
     * @param inSize integer representing desired MTU
     */
    
    public void setMTU(int inSize) {
	PACKET_SIZE = inSize;
    }

    /**
     * Returns the set MTU for this socket
     * @return the Maximum Transfer Unit set for this socket
     */
    
    public int getMTU() {
	return PACKET_SIZE;
    }
    
    /**
     * Sends a DHCPMessage object to a predifined host.
     * @param inMessage well-formed DHCPMessage to be sent to a server
     */
       
    public synchronized void send(DHCPMessage inMessage)
	 throws java.io.IOException {
	byte data[] = new byte[PACKET_SIZE];
	data = inMessage.externalize();
	InetAddress dest = null;
	try {
	    dest = InetAddress.getByName(inMessage.getDestinationAddress());
	} catch (UnknownHostException e) {}
	
	DatagramPacket outgoing = 
	    new DatagramPacket(data,
			       data.length,
			       dest,
			       inMessage.getPort());
	//gSocket.
	send(outgoing); // send outgoing message
	
    }

    /** 
     * Receives a datagram packet containing a DHCP Message into
     * a DHCPMessage object.
     * @return true if message is received, false if timeout occurs.  
     * @param outMessage DHCPMessage object to receive new message into
     */
    
    public synchronized boolean receive(DHCPMessage outMessage)  {
	try {
	    DatagramPacket incoming = 
		new DatagramPacket(new byte[PACKET_SIZE], 
				   PACKET_SIZE);
	    //gSocket.
	    receive(incoming); // block on receive for SO_TIMEOUT

	    outMessage.internalize(incoming.getData());
	} catch (java.io.IOException e) {
	    return false;
        }  // end catch    
	return true;
    }
    
    
    
}

    




