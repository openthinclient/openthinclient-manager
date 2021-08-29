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
/*
 * Automatically generated by jrpcgen 1.0.5 on 04.05.05 18:15 jrpcgen is part of
 * the "Remote Tea" ONC/RPC package for Java See
 * http://remotetea.sourceforge.net for details
 */
package org.openthinclient.mountd.tea;

import java.io.IOException;
import java.net.InetAddress;

import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.XdrVoid;
import org.acplt.oncrpc.server.OncRpcCallInformation;
import org.acplt.oncrpc.server.OncRpcDispatchable;
import org.acplt.oncrpc.server.OncRpcServerStub;
import org.acplt.oncrpc.server.OncRpcServerTransport;
import org.acplt.oncrpc.server.OncRpcServerTransportRegistrationInfo;
import org.acplt.oncrpc.server.OncRpcTcpServerTransport;
import org.acplt.oncrpc.server.OncRpcUdpServerTransport;

public abstract class MountDaemonStub extends OncRpcServerStub
    implements
      OncRpcDispatchable {

  public MountDaemonStub() throws OncRpcException, IOException {
    this(0, 0);
  }

  public MountDaemonStub(int port, int mountdProgramNumber)
      throws OncRpcException, IOException {
    this(null, port, mountdProgramNumber);
  }

  public MountDaemonStub(InetAddress bindAddr, int port, int mountdProgramNumber)
      throws OncRpcException, IOException {
    info = new OncRpcServerTransportRegistrationInfo[]{new OncRpcServerTransportRegistrationInfo(
        mountdProgramNumber != 0 ? mountdProgramNumber : mount.MOUNTPROG, 1),};
    transports = new OncRpcServerTransport[]{
        new OncRpcUdpServerTransport(this, bindAddr, port, info, 32768),
        new OncRpcTcpServerTransport(this, bindAddr, port, info, 32768)};
  }

  public void dispatchOncRpcCall(OncRpcCallInformation call, int program,
      int version, int procedure) throws OncRpcException, IOException {
    if (version == 1) {
      switch (procedure){
        case 0 : {
          call.retrieveCall(XdrVoid.XDR_VOID);
          MOUNTPROC_NULL_1();
          call.reply(XdrVoid.XDR_VOID);
          break;
        }
        case 1 : {
          dirpath args$ = new dirpath();
          call.retrieveCall(args$);
          fhstatus result$ = MOUNTPROC_MNT_1(call.peerAddress, args$);
          call.reply(result$);
          break;
        }
        case 2 : {
          call.retrieveCall(XdrVoid.XDR_VOID);
          mountlist result$ = MOUNTPROC_DUMP_1();
          call.reply(result$);
          break;
        }
        case 3 : {
          dirpath args$ = new dirpath();
          call.retrieveCall(args$);
          MOUNTPROC_UMNT_1(args$);
          call.reply(XdrVoid.XDR_VOID);
          break;
        }
        case 4 : {
          call.retrieveCall(XdrVoid.XDR_VOID);
          MOUNTPROC_UMNTALL_1();
          call.reply(XdrVoid.XDR_VOID);
          break;
        }
        case 5 : {
          call.retrieveCall(XdrVoid.XDR_VOID);
          exports result$ = MOUNTPROC_EXPORT_1();
          call.reply(result$);
          break;
        }
        case 6 : {
          call.retrieveCall(XdrVoid.XDR_VOID);
          exports result$ = MOUNTPROC_EXPORTALL_1();
          call.reply(result$);
          break;
        }
        default :
          call.failProcedureUnavailable();
      }
    } else {
      call.failProcedureUnavailable();
    }
  }

  protected abstract void MOUNTPROC_NULL_1();

  protected abstract fhstatus MOUNTPROC_MNT_1(InetAddress address, dirpath arg1);

  protected abstract mountlist MOUNTPROC_DUMP_1();

  protected abstract void MOUNTPROC_UMNT_1(dirpath arg1);

  protected abstract void MOUNTPROC_UMNTALL_1();

  protected abstract exports MOUNTPROC_EXPORT_1();

  protected abstract exports MOUNTPROC_EXPORTALL_1();

}
// End of MountDaemonStub.java
