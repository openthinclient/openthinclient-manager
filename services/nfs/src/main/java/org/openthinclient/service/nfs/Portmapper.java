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

package org.openthinclient.service.nfs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import org.acplt.oncrpc.*;
import org.acplt.oncrpc.server.*;

/**
 * The class <code>jportmap</code> implements a Java-based ONC/RPC port
 * mapper, speaking the widely deployed protocol version 2.
 * 
 * <p>
 * This class can be either used stand-alone (a static <code>main</code> is
 * provided for this purpose) or as part of an application. In this case you
 * should check first for another portmap already running before starting your
 * own one.
 * 
 * @version $Revision: 132 $ $Date: 2006-01-30 18:09:01 +0100 (Mo, 30 Jan 2006) $ $State$ $Locker$
 * @author Harald Albrecht
 */
public class Portmapper extends OncRpcServerStub implements OncRpcDispatchable {

  /**
   * Create a new portmap instance, create the transport registration
   * information and UDP and TCP-based transports, which will be bound later to
   * port 111. The constructor does not start the dispatcher loop.
   */
  public Portmapper() throws OncRpcException, IOException {
    this(PMAP_PORT, PMAP_PROGRAM);
  }

  /**
   * Create a new portmap instance, create the transport registration
   * information and UDP and TCP-based transports, which will be bound later to
   * the specified port. The constructor does not start the dispatcher loop.
   */
  public Portmapper(int port, int programNumber) throws OncRpcException,
      IOException {
    if (0 == programNumber)
      programNumber = PMAP_PROGRAM;
    if (0 == port)
      port = PMAP_PORT;
    
    //
    // We only need to register one {progam, version}.
    //
    info = new OncRpcServerTransportRegistrationInfo[]{new OncRpcServerTransportRegistrationInfo(
        programNumber, PMAP_VERSION)};
    //
    // We support both UDP and TCP-based transports for ONC/RPC portmap
    // calls, and these transports are bound to the well-known port 111.
    //
    transports = new OncRpcServerTransport[]{
        new OncRpcUdpServerTransport(this, port, info, 32768),
        new OncRpcTcpServerTransport(this, port, info, 32768)};
    //
    // Finally, we add ourself to the list of registered ONC/RPC servers.
    // This is just a convenience.
    //
    servers.addElement(new OncRpcServerIdent(programNumber, PMAP_VERSION,
        OncRpcProtocols.ONCRPC_TCP, port));
    servers.addElement(new OncRpcServerIdent(programNumber, PMAP_VERSION,
        OncRpcProtocols.ONCRPC_UDP, port));
    //
    // Determine all local IP addresses assigned to this host.
    // Once again, take care of broken JDKs, which can not handle
    // InetAdress.getLocalHost() properly. Sigh.
    //
    try {
      InetAddress loopback = InetAddress.getByName("127.0.0.1");
      InetAddress[] addrs = InetAddress.getAllByName("127.0.0.1");
      //
      // Check whether the loopback address is already included in
      // the address list for this host. If not, add it to the list.
      //
      boolean loopbackIncluded = false;
      for (int idx = 0; idx < addrs.length; ++idx) {
        if (addrs[idx].equals(loopback)) {
          loopbackIncluded = true;
          break;
        }
      }
      if (loopbackIncluded) {
        locals = addrs;
      } else {
        locals = new InetAddress[addrs.length + 1];
        locals[0] = loopback;
        System.arraycopy(addrs, 0, locals, 1, addrs.length);
      }
    } catch (UnknownHostException e) {
      //
      // Trouble getting all addresses for this host (which might
      // have been caused by some dumb security manager -- yeah, as
      // if managers were not dumb by definition), so fall back to
      // allowing only the loopback address.
      //
      locals = new InetAddress[1];
      locals[0] = InetAddress.getByName("127.0.0.1");
    }
  }

  /**
   * Lookup port for (program, version, protocol). If no suitable registration
   * entry if found and an entry with another version, but the same program and
   * version number is found, this is returned instead. This is compatible with
   * the way Sun's portmap implementation works.
   * 
   * @param params server identification (program, version, protocol) to look
   *          up. The port field is not used.
   * 
   * @return port number where server listens for incomming ONC/RPC calls, or
   *         <code>0</code>, if no server is registered for (program,
   *         protocol).
   */
  OncRpcGetPortResult getPort(OncRpcServerIdent params) {
    OncRpcServerIdent ident = null;
    OncRpcGetPortResult result = new OncRpcGetPortResult();
    int size = servers.size();
    for (int idx = 0; idx < size; ++idx) {
      OncRpcServerIdent svr = (OncRpcServerIdent) servers.get(idx);
      if ((svr.program == params.program) && (svr.protocol == params.protocol)) {
        //
        // (program, protocol) already matches. If it has the same
        // version, then we're done. Otherwise we remember this
        // entry for possible later usage and search further through
        // the list.
        //
        if (svr.version == params.version) {
          result.port = svr.port;
          return result;
        }
        ident = svr;
      }
    }
    //
    // Return port of "best" match, if one was found at all, otherwise
    // just return 0, which indicates an invalid UDP/TCP port.
    //
    if (ident == null) {
      result.port = 0;
    } else {
      result.port = ident.port;
    }
    return result;
  }

  /**
   * Register a port number for a particular (program, version, protocol). Note
   * that a caller can not register the same (program, version, protocol) for
   * another port. In this case we return false. Thus, a caller first needs to
   * deregister any old entries which it whishes to update. Always add new
   * registration entries to the end of the list (vector).
   * 
   * @param params (program, version, protocol, port) to register.
   * 
   * @return <code>true</code> if registration succeeded.
   */
  XdrBoolean setPort(OncRpcServerIdent params) {
    if (params.program != PMAP_PROGRAM) {
      //
      // Only accept registration attempts for anything other than
      // the portmapper. We do not want clients to play tricks on us.
      //
      int size = servers.size();
      for (int idx = 0; idx < size; ++idx) {
        OncRpcServerIdent svr = (OncRpcServerIdent) servers.get(idx);
        if ((svr.program == params.program) && (svr.version == params.version)
            && (svr.protocol == params.protocol)) {
          //
          // In case (program, version, protocol) is already
          // registered only accept, if the port stays the same.
          // This will silently accept double registrations (i.e.,
          // due to duplicated UDP calls).
          //
          return new XdrBoolean(svr.port == params.port);
        }
      }
      //
      // Add new registration entry to end of the list.
      //
      servers.addElement(params);
      return new XdrBoolean(true);
    }
    return new XdrBoolean(false);
  }

  /**
   * Deregister all port settings for a particular (program, version) for all
   * transports (TCP, UDP, ...). While these are strange semantics, they are
   * compatible with Sun's portmap implementation.
   * 
   * @param params (program, version) to deregister. The protocol and port
   *          fields are not used.
   * 
   * @return <code>true</code> if deregistration succeeded.
   */
  XdrBoolean unsetPort(OncRpcServerIdent params) {
    boolean ok = false;
    if (params.program != PMAP_PROGRAM) {
      //
      // Only allow clients to deregister ONC/RPC servers other than
      // the portmap entries.
      //
      int size = servers.size();
      for (int idx = size - 1; idx >= 0; --idx) {
        OncRpcServerIdent svr = (OncRpcServerIdent) servers.get(idx);
        if ((svr.program == params.program) && (svr.version == params.version)) {
          servers.removeElementAt(idx);
          ok = true;
        }
      }
    }
    return new XdrBoolean(ok);
  }

  /**
   * Return list of registered ONC/RPC servers.
   * 
   * @return list of ONC/RPC server descriptions (program, version, protocol,
   *         port).
   */
  OncRpcDumpResult listServers() {
    OncRpcDumpResult result = new OncRpcDumpResult();
    result.servers = servers;
    return result;
  }

  /**
   * Checks whether the address given belongs to one of the local addresses of
   * this host.
   * 
   * @param addr IP address to check.
   * 
   * @return <code>true</code> if address specified belongs to one of the
   *         local addresses of this host.
   */
  boolean isLocalAddress(InetAddress addr) {
    int size = locals.length;
    for (int idx = 0; idx < size; ++idx) {
      if (addr.equals(locals[idx])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Dispatch incomming ONC/RPC calls to the individual handler functions. The
   * CALLIT method is currently unimplemented.
   * 
   * @param call The ONC/RPC call, with references to the transport and XDR
   *          streams to use for retrieving parameters and sending replies.
   * @param program the portmap's program number, 100000
   * @param version the portmap's protocol version, 2
   * @param procedure the procedure to call.
   * 
   * @throws OncRpcException if an ONC/RPC error occurs.
   * @throws IOException if an I/O error occurs.
   */
  public void dispatchOncRpcCall(OncRpcCallInformation call, int program,
      int version, int procedure) throws OncRpcException, IOException {
    //
    // Make sure it's the right program and version that we can handle.
    // (defensive programming)
    //
    if (program == PMAP_PROGRAM) {
      if (version == PMAP_VERSION) {
        switch (procedure){
          case 0 : { // handle NULL call.
            call.retrieveCall(XdrVoid.XDR_VOID);
            call.reply(XdrVoid.XDR_VOID);
            break;
          }
          case OncRpcPortmapServices.PMAP_GETPORT : { // handle port query
            OncRpcServerIdent params = new OncRpcServerIdent();
            call.retrieveCall(params);
            OncRpcGetPortResult result = getPort(params);
            call.reply(result);
            break;
          }
          case OncRpcPortmapServices.PMAP_SET : { // handle port registration
            //
            // ensure that no remote client tries to register
            //
            OncRpcServerIdent params = new OncRpcServerIdent();
            call.retrieveCall(params);
            XdrBoolean result;
            if (isLocalAddress(call.peerAddress)) {
              result = setPort(params);
            } else {
              result = new XdrBoolean(false);
            }
            call.reply(result);
            break;
          }
          case OncRpcPortmapServices.PMAP_UNSET : { // handle port
            // deregistration
            OncRpcServerIdent params = new OncRpcServerIdent();
            call.retrieveCall(params);
            XdrBoolean result;
            if (isLocalAddress(call.peerAddress)) {
              result = unsetPort(params);
            } else {
              result = new XdrBoolean(false);
            }
            call.reply(result);
            break;
          }
          case OncRpcPortmapServices.PMAP_DUMP : { // list all registrations
            call.retrieveCall(XdrVoid.XDR_VOID);
            OncRpcDumpResult result = listServers();
            call.reply(result);
            break;
          }
          default : // unknown/unimplemented procedure
            call.failProcedureUnavailable();
        }
      } else {
        call.failProgramMismatch(PMAP_VERSION, PMAP_VERSION);
      }
    } else {
      call.failProgramUnavailable();
    }
  }

  /**
   * List of IP addresses assigned to this host. Will be filled later by
   * constructor.
   */
  public InetAddress[] locals = null;

  /**
   * The list of registrated servers.
   */
  public Vector servers = new Vector();

  /**
   * Create an instance of an ONC/RPC portmapper and run it. As we have to
   * bootstrap the ONC/RPC port information chain, we do not use the usual
   * overloaded <code>run()</code> method without any parameters, but instead
   * supply it the transports to handle. Registration and deregistration is not
   * necessary and not possible.
   */
  public static void main(String[] args) {
    try {
      Portmapper pmap = new Portmapper();
      pmap.run(pmap.transports);
      pmap.close(pmap.transports);
    } catch (OncRpcException e) {
      e.printStackTrace(System.out);
    } catch (IOException e) {
      e.printStackTrace(System.out);
    }
  }

  /**
   * Well-known port where the portmap process can be found on Internet hosts.
   */
  public static final int PMAP_PORT = 111;

  /**
   * Program number of the portmapper as defined in RFC 1832.
   */
  public static final int PMAP_PROGRAM = 100000;

  /**
   * Program version number of the portmapper as defined in RFC 1832.
   */
  public static final int PMAP_VERSION = 2;

}

// End of jportmap.java
