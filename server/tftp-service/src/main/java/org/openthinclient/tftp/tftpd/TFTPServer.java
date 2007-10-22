/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *******************************************************************************/
package org.openthinclient.tftp.tftpd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * A read-only implementation of a <a
 * href="http://www.faqs.org/rfcs/rfc1350.html">TFTP (RFC 1350)</a> server. It
 * supports the <a href="http://www.faqs.org/rfcs/rfc2347.html">TFTP Option
 * extenstion (RFC 2347)</a> and the <a
 * href="http://www.faqs.org/rfcs/rfc1784.html">TFTP Timeout Interval and
 * Transfer Size Options (RFC 1784)</a>
 * 
 * @author levigo
 */
public class TFTPServer implements Runnable {
  private static final Logger logger = Logger.getLogger(TFTPServer.class);

  // track open channels
  private Collection<DatagramChannel> openDatagramChannels = new ArrayList<DatagramChannel>();
  
  // packet opcodes
  private static final short READ = 1;
  private static final short WRITE = 2;
  private static final short DATA = 3;
  private static final short ACK = 4;
  private static final short ERROR = 5;
  private static final short OACK = 6;

  // error codes
  private static final short ERROR_UNDEFINED = 0;
  private static final short ERROR_FILE_NOT_FOUND = 1;
  private static final short ERROR_ACCESS_VIOLATION = 2;
  private static final short ERROR_ILLEGAL_OPERATION = 4;

  // other stuff
  public static final short DEFAULT_TFTP_PORT = 69;
  private static final int RECV_TIMEOUT = 2000;
  private static final int MAX_RETRIES = 5;
  private static final int STD_TFTP_MAX_PACKET_LENGTH = 516;

  /**
   * Give up after 100 errors within one second, in order to not bog down the
   * whole server if something goes very, very wrong repeatedly.
   */
  private static final int MAX_ERRORS_IN_ONE_SECOND = 100;

  /**
   * A Thread which handles a single TFTP send operation.
   */
  private class TFTPSend extends Thread {
    private DatagramChannel channel;
    private final SocketAddress peer;

    private int timeout = RECV_TIMEOUT;
    protected InputStream source;
    private HashMap<String, String> options;
    private Selector selector;
    private final DatagramChannel serverChannel;
    private int blksize = 512;
    private String filename;
    private String modestring;

    /**
     * 
     * @param peer
     * @param buffer
     * @param serverChannel s *
     * @param triggerPrefix
     * @param triggerClass
     * @param ldapLogin
     * @param ldapPassword
     * @throws IOException
     * @throws IOException
     */
    public TFTPSend(SocketAddress peer, ByteBuffer buffer,
        DatagramChannel serverChannel) throws IOException {
      super("TFTP Send for " + peer);

      this.peer = peer;
      this.serverChannel = serverChannel;

      parseRequest(peer, buffer, serverChannel);
    }

    /**
     * @param peer
     * @param buffer
     * @param serverChannel
     * @throws IOException
     */
    private void parseRequest(SocketAddress peer, ByteBuffer buffer,
        DatagramChannel serverChannel) throws IOException {
      buffer.position(2);
      filename = getCString(buffer);
      modestring = getCString(buffer);
      if (null == filename || null == modestring)
        throw new IOException("Malformed request");

      if (!modestring.equalsIgnoreCase("octet"))
        throw new IOException("Transfer mode \"" + modestring
            + "\" not supported");

      // don't fall for the obvious hacks
      if (filename.matches(".*\\.\\.[/\\\\].*"))
        throw new FileNotFoundException(
            "No relative filename hacks, please (nice try, though).");

      // gather options
      options = new HashMap<String, String>();
      while (true) {
        String option = getCString(buffer);
        String value = getCString(buffer);
        if (null == option || null == value)
          break;
        options.put(option.toLowerCase(), value);
      }
    }

    /**
     * @param peer
     * @param buffer
     * @param serverChannel
     * @throws IOException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SocketException
     * @throws ClosedChannelException
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void initTransfer(SocketAddress peer, DatagramChannel serverChannel)
        throws IOException, InstantiationException, IllegalAccessException {
      channel = DatagramChannel.open();
      channel.socket().bind(new InetSocketAddress(0));

      // set up selector
      selector = Selector.open();
      channel.configureBlocking(false);
      channel.register(selector, SelectionKey.OP_READ);
      
      // add to open channels
      openDatagramChannels.add(channel);

      // normalize filename
      if (!filename.startsWith("/"))
        filename = "/" + filename;

      TFTPExport bestMatch = findExport(filename);
      TFTPProvider provider = bestMatch.getProvider();

      final String prefix = bestMatch.getPathPrefix();
      final String localName = filename.substring(bestMatch.getPathPrefix()
          .length());

      // this is where the data will come from.
      source = provider.getStream(peer, serverChannel.socket()
          .getLocalSocketAddress(), prefix, localName);

      logger.info("Starting TFTP send for " + filename + " to " + peer);

      Map<String, String> recognizedOptions = new HashMap<String, String>();

      // timeout option: sets the retransmit timeout
      if (options.containsKey("timeout")) {
        try {
          timeout = Integer.parseInt(options.get("timeout")) * 1000;
          recognizedOptions.put("timeout", Integer.toString(timeout / 1000));
        } catch (NumberFormatException e) {
          logger.error("Got invalid timeout option argument: "
              + options.get("timeout"), e);
        }
      }

      // tsize option: requests the file size
      if (options.containsKey("tsize")) {
        long len = provider.getLength(peer, serverChannel.socket()
            .getLocalSocketAddress(), prefix, localName);
        if (len < 0)
          len = getDataLength();

        recognizedOptions.put("tsize", Long.toString(len));
      }

      if (options.containsKey("blksize")) {
        try {
          blksize = Integer.parseInt(options.get("blksize"));
          if (blksize < 10 || blksize > 65535)
            throw new IOException("Illegal blksize option: " + blksize);
          recognizedOptions.put("blksize", Integer.toString(blksize));
          if (logger.isInfoEnabled())
            logger.info("Using blksize=" + blksize);
        } catch (NumberFormatException e) {
          throw new IOException("Got invalid blksize option argument: "
              + options.get("blksize"));
        }
      }

      if (recognizedOptions.size() > 0) {
        // send OACK
        ByteBuffer buffer1 = ByteBuffer.allocate(STD_TFTP_MAX_PACKET_LENGTH);

        buffer1.clear();
        buffer1.putShort(OACK);
        for (Iterator<Map.Entry<String, String>> i = recognizedOptions
            .entrySet().iterator(); i.hasNext();) {
          Map.Entry<String, String> e = i.next();
          buffer1.put(e.getKey().getBytes("ASCII"));
          buffer1.put((byte) 0);
          buffer1.put(e.getValue().getBytes("ASCII"));
          buffer1.put((byte) 0);
        }
        buffer1.flip();

        if (logger.isDebugEnabled())
          logger.debug("Sending OACK with options " + recognizedOptions);
        sendAndWaitForACK(buffer1, ByteBuffer
            .allocate(STD_TFTP_MAX_PACKET_LENGTH), (short) 0);
      }

      setName("TFTP Send for " + peer + " file: " + filename);
    }

    /**
     * @param filename
     * @return
     * @throws FileNotFoundException
     */
    private TFTPExport findExport(String filename) throws FileNotFoundException {
      TFTPExport bestMatch = null;
      for (TFTPExport export : exports)
        if (filename.startsWith(export.getPathPrefix())
            && (bestMatch == null || export.getPathPrefix().length() > bestMatch
                .getPathPrefix().length()))
          bestMatch = export;

      if (null == bestMatch)
        throw new FileNotFoundException(filename);
      return bestMatch;
    }

    /**
     * Get the data length the hard way: read the stream and replace it with the
     * read result.
     * 
     * @return
     * @throws IOException
     */
    private long getDataLength() throws IOException {
      long len;
      // provider doesn't know (yet)
      ByteArrayOutputStream s = new ByteArrayOutputStream();
      byte b[] = new byte[1024];
      int read;
      while ((read = source.read(b)) >= 0)
        s.write(b, 0, read);

      source.close();
      b = s.toByteArray();
      len = b.length;

      source = new ByteArrayInputStream(b);
      return len;
    }

    /*
     * @see java.lang.Runnable#run()
     */
    @Override
	public void run() {
      try {
        long start = System.currentTimeMillis();
        initTransfer(peer, serverChannel);
        logger
            .info("TFTP startup took " + (System.currentTimeMillis() - start));

        try {
          // init two buffers so that we can keep re-sending the xmitBuffer if
          // the send is not ACKed.
          ByteBuffer xmitBuffer = ByteBuffer.allocate(blksize + 4);
          ByteBuffer recvBuffer = ByteBuffer.allocate(blksize + 4);

          short block = 1;
          byte inBuffer[] = new byte[blksize];
          int bytesInBuffer, read;
          do {
            xmitBuffer.clear();
            xmitBuffer.putShort(DATA);
            xmitBuffer.putShort(block);

            // fill the input buffer
            bytesInBuffer = 0;
            while ((read = source.read(inBuffer, bytesInBuffer, inBuffer.length
                - bytesInBuffer)) >= 0
                && bytesInBuffer < inBuffer.length)
              bytesInBuffer += read;

            xmitBuffer.put(inBuffer, 0, bytesInBuffer);
            xmitBuffer.flip();

            sendAndWaitForACK(xmitBuffer, recvBuffer, block);

            if (logger.isDebugEnabled())
              logger.debug("Sent DATA: block=" + block + " length="
                  + bytesInBuffer);

            block++;
          } while (bytesInBuffer == blksize);

          logger.info("TFTP send to " + peer + " finished normally.");
        } catch (IOException e) {
          logger.info("TFTP send to " + peer + " failed.", e);
          // try to send error packet
          sendErrorPacket(peer, channel, ERROR_UNDEFINED, e.toString());
        }
      } catch (FileNotFoundException e) {
        String message = e.getMessage();
        int idx = message.indexOf(": ");
        if (idx > 0)
          message = message.substring(idx + 2);
        logger.error("READ: file not found for " + peer + ": " + message);
        sendErrorPacket(peer, serverChannel, ERROR_FILE_NOT_FOUND, message);
      } catch (IOException e) {
        logger.error("READ: error starting transfer for " + peer + ": "
            + e.getMessage());
        sendErrorPacket(peer, serverChannel, ERROR_FILE_NOT_FOUND, e.toString());
      } catch (Throwable e) {
        logger.error("READ: error starting transfer for " + peer, e);
        sendErrorPacket(peer, serverChannel, ERROR_UNDEFINED, e.toString());
      }

      try {
        if (null != channel)
          channel.close();
      } catch (IOException e) {
        logger.error("Error closing channel", e);
      }
      try {
        if (null != selector)
          selector.close();
      } catch (IOException e) {
        logger.error("Error closing selector", e);
      }
    }

    /**
     * Send a packet and wait for the associated ACK message.
     * 
     * @param xmitBuffer buffer to send
     * @param recvBuffer buffer to use for reception
     * @param block the expected block number
     * @throws IOException
     */
    private void sendAndWaitForACK(ByteBuffer xmitBuffer,
        ByteBuffer recvBuffer, short block) throws IOException {
      int xmitTrys = 0;
      boolean gotACK = false;
      while (!gotACK) {
        // send data packet
        channel.send(xmitBuffer, peer);
        xmitBuffer.rewind();
        xmitTrys++;

        // wait for proper ack packet
        gotACK = waitForACK(recvBuffer, block);
        if (!gotACK && xmitTrys >= MAX_RETRIES)
          throw new IOException("Failed to transfer file: retries exceeded.");
      }
    }

    /**
     * Wait for an ACK message.
     * 
     * @param recvBuffer buffer to use for reception
     * @param block the expected block number
     * @throws IOException
     */
    private boolean waitForACK(ByteBuffer recvBuffer, short block)
        throws IOException {
      while (true) {
        // the only channel registered with the selector is our own
        // channel
        selector.selectedKeys().clear();
        int ready = selector.select(timeout);

        if (ready == 0) {
          logger.debug("ACK receive timeout");
          return false;
        }

        recvBuffer.clear();
        SocketAddress sender = channel.receive(recvBuffer);

        recvBuffer.flip();

        if (null == sender || !sender.equals(peer)) {
          if (logger.isDebugEnabled())
            logger.debug("Ignoring packet from host != my peer: " + sender);
        } else if (recvBuffer.getShort(0) != ACK) {
          if (recvBuffer.getShort(0) == ERROR) {
            String message = parseERRORPacket(recvBuffer);
            throw new IOException("Error received: " + message);
          } else if (logger.isDebugEnabled())
            logger.debug("Ignoring packet with opcode "
                + recvBuffer.getShort(0));
          // } else if (recvBuffer.getShort(2) == block - 1) {
          // // peer requests retransmit!
          // if (logger.isDebugEnabled())
          // logger.debug("Retransmit of block " + recvBuffer.getShort(2)
          // + " requested");
          // return false;
        } else if (recvBuffer.getShort(2) != block) {
          if (logger.isDebugEnabled())
            logger.debug("Ignoring packet with wrong block# "
                + recvBuffer.getShort(2));
        } else {
          if (logger.isDebugEnabled())
            logger.debug("Got ACK: block=" + recvBuffer.getShort(2));
          return true; // the ACK was ok, send the next packet.
        }
      }
    }
  }

  /**
   * The root directory which is made visible through TFTP
   */
  private final Set<TFTPExport> exports = new HashSet<TFTPExport>();

  private static boolean PROVIDE_LOCAL_ADDRESS = true;

  private Selector serverSelector;

  /**
   * This could use some cool option parsing, but for now, we want to run it
   * from JBoss only anyway.
   * 
   * @param argv
   * @throws IOException
   */
  public static void main(String argv[]) throws IOException {
    new TFTPServer("share/tftp").start();
  }

  /**
   * Extract a null-terminated string from a ByteBuffer.
   * 
   * @param b
   * @return
   * @throws UnsupportedEncodingException
   */
  private static String getCString(ByteBuffer b) {
    int start = b.position();
    while (b.hasRemaining() && b.get() != 0);

    // did we find the terminating zero?
    if (b.position() == start || b.get(b.position() - 1) != 0)
      return null;

    byte s[] = new byte[b.position() - 1 - start];
    b.position(start);
    b.get(s);
    b.get(); // advance position just after the zero

    try {
      return new String(s, "ASCII");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Should not happen");
    }
  }

  /**
   * Send an error packet.
   * 
   * @param peer where to send it to
   * @param channel the channel to use
   * @param code the error code
   * @param message the error message
   * @throws IOException
   */
  private static void sendErrorPacket(SocketAddress peer,
      DatagramChannel channel, short code, String message) {
    if (logger.isDebugEnabled())
      logger.debug("Sending error packet to " + peer + ": " + code + "/"
          + message);
    byte messageBytes[];
    try {
      messageBytes = message.getBytes("ASCII");
      ByteBuffer b = ByteBuffer.allocate(5 + messageBytes.length);
      b.putShort(ERROR);
      b.putShort(code);
      b.put(messageBytes);
      b.put((byte) 0);
      b.flip();

      channel.send(b, peer);
    } catch (UnsupportedEncodingException e) {
      // that's ridiculous
      logger.fatal(e);
    } catch (IOException e) {
      // ok, there's little we can do about this short of logging it.
      logger.error("Exception sending error packet to " + peer, e);
    }
  }

  /**
   * Construct a TFTPServer which does not yet export any directory.
   * 
   * @throws IOException
   */
  public TFTPServer() throws IOException {
    this(null, DEFAULT_TFTP_PORT);
  }

  /**
   * Construct a TFTPServer which does not yet export any directory and uses the
   * given port.
   * 
   * @param port the port to use
   * @throws IOException
   */
  public TFTPServer(int port) throws IOException {
    this(null, port);
  }

  /**
   * Construct a TFTPServer which serves from the given base directory.
   * 
   * @param basedir the root directory
   * @throws IOException
   */
  public TFTPServer(String basedir) throws IOException {
    this(basedir, DEFAULT_TFTP_PORT);
  }

  /**
   * Construct a TFTPServer which serves from the given base directory and uses
   * the given port number.
   * 
   * @param basedir the root directory
   * @param port the port to use
   * @throws IOException
   */
  public TFTPServer(String basedir, int port) throws IOException {
    if (null != basedir)
      exports.add(new TFTPExport(basedir));

    serverSelector = Selector.open();
    if (!PROVIDE_LOCAL_ADDRESS) {
      // set up the channel
      configureChannel(new InetSocketAddress((InetAddress) null, port));
    } else {
      // To properly serve DHCP, we must bind to all local addresses
      // individually, in order to be able to distinguish, from which network
      // (on a multi-homed machine) a broadcast came.
      // bind to all local ports. Gobble up all addresses we can find.
      for (Enumeration i = NetworkInterface.getNetworkInterfaces(); i
          .hasMoreElements();) {
        NetworkInterface nif = (NetworkInterface) i.nextElement();
        for (Enumeration j = nif.getInetAddresses(); j.hasMoreElements();) {
          InetAddress address = (InetAddress) j.nextElement();
          if (address instanceof Inet4Address && !address.isLoopbackAddress())
            configureChannel(new InetSocketAddress(address, port));
        }
      }
    }
  }

  /**
   * @throws SocketException
   * @throws IOException
   */
  private void configureChannel(InetSocketAddress isa) throws IOException {
    DatagramChannel serverChannel = DatagramChannel.open();

    // try reusing address if already bound
    if (serverChannel.socket().isBound())
    	serverChannel.socket().setReuseAddress(true);
    
    serverChannel.socket().bind(isa);
    serverChannel.socket().setReceiveBufferSize(STD_TFTP_MAX_PACKET_LENGTH * 5);
    serverChannel.socket().setSendBufferSize(STD_TFTP_MAX_PACKET_LENGTH * 5);
    serverChannel.configureBlocking(false);
    serverChannel.register(serverSelector, SelectionKey.OP_READ);
 
    // add to open channels
    openDatagramChannels.add(serverChannel);

    logger.info("Listening on " + isa);
  }

  /**
   * Launch the server thread...
   */
  public void start() {
    new Thread(this, "TFTP Server").start();
  }

  /**
   * ...And shut it down again.
   */
  public void shutdown() {
    logger.info("Shutting down TFTP server.");
    try {
    	for(DatagramChannel d: openDatagramChannels)
    		d.close();
      serverSelector.close();
    } catch (IOException e) {
      // ignore
    }
  }

  /*
   * @see java.lang.Runnable#run()
   */
  public void run() {
    long lastSecond = System.currentTimeMillis() / 1000;
    int errorCounter = 0;

    ByteBuffer buffer = ByteBuffer.allocate(STD_TFTP_MAX_PACKET_LENGTH);
    while (true) {
      // reset error counter every second
      long second = System.currentTimeMillis() / 1000;
      if (second != lastSecond)
        errorCounter = 0;

      try {
        int n = serverSelector.select();
        if (0 == n)
          continue;

        for (Iterator i = serverSelector.selectedKeys().iterator(); i.hasNext();) {
          SelectionKey key = (SelectionKey) i.next();
          i.remove();

          DatagramChannel serverChannel = (DatagramChannel) key.channel();

          buffer.clear();
          SocketAddress peer = serverChannel.receive(buffer);
          buffer.flip();

          if (buffer.limit() <= 2) {
            sendErrorPacket(peer, serverChannel, ERROR_UNDEFINED,
                "Short packet");
          } else {
            switch (buffer.getShort()){
              case READ :
                handleREAD(buffer, peer, serverChannel);
                break;
              case WRITE :
                sendErrorPacket(peer, serverChannel, ERROR_ACCESS_VIOLATION,
                    "WRITE not supported");
                logger.warn("WRITE not supported");
                break;
              case ERROR :
                logger.warn("Got ERROR " + parseERRORPacket(buffer));
                break;
              case ACK :
                // Ignore. We SHOULD actually make sure that even error packets
                // are acked, but I can't be bothered...
                break;
              default :
                logger.warn("Illegal operation " + buffer.getShort(0)
                    + " requested.");
                sendErrorPacket(peer, serverChannel, ERROR_ILLEGAL_OPERATION,
                    "Operation not supported");
            }
          }
        }
      } catch (ClosedSelectorException e) {
        // this is normal during shutdown
        logger.debug("Selector closed, shutting down.");
        return;
      } catch (AsynchronousCloseException e) {
        // this is normal during shutdown
        logger.debug("Channel closed, shutting down.");
        return;
      } catch (Throwable e) {
        errorCounter++;
        if (errorCounter > MAX_ERRORS_IN_ONE_SECOND) {
          logger.fatal("Shutting down due to repeated errors (" + errorCounter
              + " within the last second).");
          return;
        } else
          logger.error(
              "Caught throwable in main loop. Trying to hang on anyway. ("
                  + errorCounter + " errors within this second already)", e);
      }
    }
  }

  /**
   * Handle a RRQ request
   * 
   * @param buffer
   * @param peer
   * @param serverChannel
   * @throws IOException
   */
  private void handleREAD(ByteBuffer buffer, SocketAddress peer,
      DatagramChannel serverChannel) throws IOException {
    try {
      new TFTPSend(peer, buffer, serverChannel).start();
    } catch (IOException e) {
      logger.error("READ: error starting transfer for " + peer, e);
      sendErrorPacket(peer, serverChannel, ERROR_UNDEFINED, e.toString());
    }
  }

  /**
   * Parse an ERROR packet.
   * 
   * @param buffer
   * @return
   * @throws UnsupportedEncodingException
   */
  private String parseERRORPacket(ByteBuffer buffer)
      throws UnsupportedEncodingException {
    buffer.position(2);
    String message = buffer.getShort() + ": " + getCString(buffer);

    return message;
  }

  /**
   * Add a TFTPExport. If there is already an export serving the same basedir it
   * is replaced.
   * 
   * @param export
   */
  public void addExport(TFTPExport export) {
    exports.add(export);
  }

  /**
   * Remove an existing export. An export is considered to be existing, if it
   * serves the same basedir.
   * 
   * @param export
   */
  public boolean removeExport(TFTPExport export) {
    return exports.remove(export);
  }

  /**
   * Return the set of currently active exports.
   * 
   * @return
   */
  public Set<TFTPExport> getExports() {
    return Collections.unmodifiableSet(exports);
  }
}
