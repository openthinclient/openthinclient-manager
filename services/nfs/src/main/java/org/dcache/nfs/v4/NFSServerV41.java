/*
 * Copyright (c) 2009 - 2017 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.nfs.v4;

import org.dcache.nfs.ChimeraNFSException;
import org.dcache.nfs.ExportFile;
import org.dcache.nfs.v4.xdr.COMPOUND4args;
import org.dcache.nfs.v4.xdr.COMPOUND4res;
import org.dcache.nfs.v4.xdr.nfs4_prot_NFS4_PROGRAM_ServerStub;
import org.dcache.nfs.v4.xdr.nfs_argop4;
import org.dcache.nfs.v4.xdr.nfs_resop4;
import org.dcache.nfs.nfsstat;
import org.dcache.oncrpc4j.rpc.OncRpcException;
import org.dcache.oncrpc4j.rpc.RpcCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.dcache.nfs.v4.xdr.nfs_opnum4;
import org.dcache.nfs.vfs.PseudoFs;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.dcache.commons.stats.RequestExecutionTimeGauges;
import org.dcache.nfs.status.BadSessionException;
import org.dcache.nfs.status.BadStateidException;
import org.dcache.nfs.status.BadXdrException;
import org.dcache.nfs.status.ExpiredException;
import org.dcache.nfs.status.InvalException;
import org.dcache.nfs.status.MinorVersMismatchException;
import org.dcache.nfs.status.NfsIoException;
import org.dcache.nfs.status.NotOnlyOpException;
import org.dcache.nfs.status.OpIllegalException;
import org.dcache.nfs.status.OpNotInSessionException;
import org.dcache.nfs.status.ResourceException;
import org.dcache.nfs.status.RetryUncacheRepException;
import org.dcache.nfs.status.SequencePosException;
import org.dcache.nfs.status.ServerFaultException;
import org.dcache.nfs.status.StaleClientidException;
import org.dcache.nfs.status.StaleStateidException;
import org.dcache.nfs.status.TooManyOpsException;
import org.dcache.nfs.v4.nlm.LockManager;
import org.dcache.nfs.v4.nlm.SimpleLm;
import org.dcache.nfs.v4.xdr.verifier4;

public class NFSServerV41 extends nfs4_prot_NFS4_PROGRAM_ServerStub {

    private static final Logger _log = LoggerFactory.getLogger(NFSServerV41.class);

    private static final RequestExecutionTimeGauges<String> GAUGES
            = new RequestExecutionTimeGauges<>(NFSServerV41.class.getName());

    private final VirtualFileSystem _fs;
    private final ExportFile _exportFile;
    private final NFSv4OperationFactory _operationFactory;
    private final NFSv41DeviceManager _deviceManager;
    private final NFSv4StateHandler _statHandler;
    private final LockManager _nlm;
    /**
     * Verifier to indicate client that server is rebooted. Current currentTimeMillis
     * is good enough, unless server reboots within a millisecond.
     */
    private final verifier4 _rebootVerifier = verifier4.valueOf(System.currentTimeMillis());

    private NFSServerV41(Builder builder) {
        _deviceManager = builder.deviceManager;
        _fs = builder.vfs;
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
        _exportFile = builder.exportFile;
        _operationFactory = builder.operationFactory;
        _nlm = builder.nlm == null ? new SimpleLm() : builder.nlm;
        _statHandler = builder.stateHandler == null ? new NFSv4StateHandler() : builder.stateHandler;
    }

    @Deprecated
    public NFSServerV41(NFSv4OperationFactory operationFactory,
            NFSv41DeviceManager deviceManager, VirtualFileSystem fs,
            ExportFile exportFile) {

        _deviceManager = deviceManager;
        _fs = fs;
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
        _exportFile = exportFile;
        _operationFactory = operationFactory;
        _nlm = new SimpleLm();
        _statHandler = new NFSv4StateHandler();
    }

    @Override
    public void NFSPROC4_NULL_4(RpcCall call$) {
        _log.debug("NFS PING client: {}", call$.getTransport().getRemoteSocketAddress());
    }

    @Override
    public COMPOUND4res NFSPROC4_COMPOUND_4(RpcCall call$, COMPOUND4args arg1) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ===");


        COMPOUND4res res = new COMPOUND4res();

        try {

            /*
             * here we have to checkfor utf8, but it's too much overhead to keep
             * spec happy.
             */
            res.tag = arg1.tag;
            String tag = arg1.tag.toString();
            MDC.put(NfsMdc.TAG, tag);
            MDC.put(NfsMdc.CLIENT, call$.getTransport().getRemoteSocketAddress().toString());

System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": NFS COMPOUND client: " + call$.getTransport().getRemoteSocketAddress() + " tag='" + tag + "' arg1.argarray=" + arg1.argarray.toString() + "' arg1.minorversion=" + arg1.minorversion.toString());
            _log.debug("NFS COMPOUND client: {}, tag: [{}]",
                    call$.getTransport().getRemoteSocketAddress(),
                    tag);

            int minorversion = arg1.minorversion.value;
            if ( minorversion > 1) {
                throw new MinorVersMismatchException(String.format("Unsupported minor version [%d]",arg1.minorversion.value) );
            }

	    if (arg1.argarray.length >= NFSv4Defaults.NFS4_MAX_OPS && minorversion == 0) {
		/*
		   in 4.1 maxops handled per session
		*/
		throw new ResourceException(String.format("Too many ops [%d]", arg1.argarray.length));
	    }
            res.resarray = new ArrayList<>(arg1.argarray.length);

// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": creating pseudofs");
            VirtualFileSystem fs = new PseudoFs(_fs, call$, _exportFile);

System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": pseudofs created");
            CompoundContextBuilder builder = new CompoundContextBuilder()
                    .withMinorversion(arg1.minorversion.value)
                    .withFs(fs)
                    .withDeviceManager(_deviceManager)
                    .withStateHandler(_statHandler)
                    .withLockManager(_nlm)
                    .withExportFile(_exportFile)
                    .withRebootVerifier(_rebootVerifier)
                    .withCall(call$);

            if (_deviceManager != null) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": deviceManager exists");
                builder.withPnfsRoleMDS();
                // we do proxy-io
                builder.withPnfsRoleDS();
            } else if (_exportFile == null) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": exportFile null");
                builder.withPnfsRoleDS();
            } else {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": something else");
                builder.withoutPnfs();
            }

            CompoundContext context = builder.build();

            boolean retransmit = false;
            for (int position = 0; position <arg1.argarray.length; position++) {
                nfs_argop4 op = arg1.argarray[position];
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": arg1 position=" + position + " op=" + op + " op.argop=" + op.argop);
                nfs_resop4 opResult = nfs_resop4.resopFor(op.argop);
                try {
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
                    if (minorversion != 0) {
                        checkOpPosition(op.argop, position, arg1.argarray.length);
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
                        if (position == 1) {
                            /*
                             * at this point we already have to have a session
                             */
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
							if (arg1.argarray.length > context.getSession().getMaxOps()) {
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
								throw new TooManyOpsException(String.format("Too many ops [%d]", arg1.argarray.length));
							}

// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
                            List<nfs_resop4> cache = context.getCache();
                            if (cache != null) {
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");

                                if (cache.isEmpty()) {
                                    /*
                                     * we got a duplicated request, but there
                                     * is nothing in the cache, though must be
                                     * as we are the second op in the compound.
                                     */
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
                                    throw new RetryUncacheRepException();
                                }
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");

                                res.resarray.addAll(cache.subList(position, cache.size()));
                                res.status = statusOfLastOperation(cache);
                                retransmit = true;
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": retransmit");
                                break;
                            }
                        }
                    }
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
                    long t0 = System.nanoTime();
                    _operationFactory.getOperation(op).process(context, opResult);
                    GAUGES.update(nfs_opnum4.toString(op.argop), System.nanoTime() - t0);
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");

                } catch (NfsIoException | ResourceException | ServerFaultException e) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": e=" + e.getMessage());
                    _log.error("NFS server fault: op: {} : {}", nfs_opnum4.toString(op.argop), e.getMessage());
                    opResult.setStatus(e.getStatus());
                } catch (BadXdrException | OpIllegalException | InvalException e) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": e=" + e.getMessage());
                    _log.warn("Faulty NFS client: op: {} : {}", nfs_opnum4.toString(op.argop), e.getMessage());
                    opResult.setStatus(e.getStatus());
                } catch (BadStateidException | StaleStateidException | ExpiredException
                        | BadSessionException | StaleClientidException  e) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": e=" + e.getMessage());
                    _log.info("Lost client state: op: {} : {}", nfs_opnum4.toString(op.argop), e.getMessage());
                    opResult.setStatus(e.getStatus());
                } catch (ChimeraNFSException e) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": e=" + e.getMessage());
                    opResult.setStatus(e.getStatus());
                } catch (OncRpcException e) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": e=" + e.getMessage());
                    opResult.setStatus(nfsstat.NFSERR_BADXDR);
                    _log.warn("Bad xdr: {}: ", e.getMessage());
                }

// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
                res.resarray.add(opResult);
                res.status = opResult.getStatus();
                if (res.status != nfsstat.NFS_OK) {
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
                    break;
                }
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
            }
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": end arg1 loop");

            if (!retransmit && context.cacheThis()) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
                context.getSessionSlot().update(res.resarray);
            }

            _log.debug( "OP: [{}] status: {}", res.tag, res.status);
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": tag='" + res.tag + "' status=" + res.status);

        } catch (ChimeraNFSException e) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": e=" + e.getMessage());
            _log.info("NFS operation failed: {}", e.getMessage());
            res.resarray = Collections.emptyList();
            res.status = e.getStatus();
        } catch (Exception e) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": e=" + e.getMessage());
            _log.error("Unhandled exception:", e);
            res.resarray = Collections.emptyList();
            res.status = nfsstat.NFSERR_SERVERFAULT;
        }finally{
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": finally removing");
            MDC.remove(NfsMdc.TAG);
            MDC.remove(NfsMdc.CLIENT);
            MDC.remove(NfsMdc.SESSION);
        }

System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": return res=" + res);
        return res;
    }

    /**
     * Get {@link NFSv4StateHandler} used by this nfs server.
     * @return state handler.
     */
    public NFSv4StateHandler getStateHandler() {
        return _statHandler;
    }

    /*
     *
     * from NFSv4.1 spec:
     *
     * SEQUENCE MUST appear as the first operation of any COMPOUND in which
     * it appears.  The error NFS4ERR_SEQUENCE_POS will be returned when it
     * is found in any position in a COMPOUND beyond the first.  Operations
     * other than SEQUENCE, BIND_CONN_TO_SESSION, EXCHANGE_ID,
     * CREATE_SESSION, and DESTROY_SESSION, MUST NOT appear as the first
     * operation in a COMPOUND.  Such operations MUST yield the error
     * NFS4ERR_OP_NOT_IN_SESSION if they do appear at the start of a
     * COMPOUND.
     *
     */
    private static void checkOpPosition(int opCode, int position, int total) throws ChimeraNFSException {

        /*
         * special case of illegal operations.
         */
        if (opCode > nfs_opnum4.OP_RECLAIM_COMPLETE || opCode < nfs_opnum4.OP_ACCESS) {
            return;
        }

        if(position == 0 ) {
            switch(opCode) {
                case nfs_opnum4.OP_SEQUENCE:
                case nfs_opnum4.OP_CREATE_SESSION:
                case nfs_opnum4.OP_EXCHANGE_ID:
                case nfs_opnum4.OP_DESTROY_SESSION:
                case nfs_opnum4.OP_DESTROY_CLIENTID:
                    break;
                default:
                    throw new OpNotInSessionException();
            }

            if (total > 1) {
                switch (opCode) {
                    case nfs_opnum4.OP_CREATE_SESSION:
                    case nfs_opnum4.OP_DESTROY_CLIENTID:
                    case nfs_opnum4.OP_EXCHANGE_ID:
                        throw new NotOnlyOpException();
                    default:
                    // NOP
                }
            }

        } else {
            switch (opCode) {
                case nfs_opnum4.OP_SEQUENCE:
                    throw new SequencePosException();
            }
        }
    }

    private static int statusOfLastOperation(List<nfs_resop4> ops) {
        return ops.get(ops.size() -1).getStatus();
    }

    public RequestExecutionTimeGauges<String> getStatistics() {
        return GAUGES;
    }

    public static class Builder {

        private NFSv4OperationFactory operationFactory;
        private NFSv41DeviceManager deviceManager;
        private VirtualFileSystem vfs;
        private ExportFile exportFile;
        private LockManager nlm;
        private NFSv4StateHandler stateHandler;

        public Builder withDeviceManager(NFSv41DeviceManager deviceManager) {
            this.deviceManager = deviceManager;
            return this;
        }

        public Builder withOperationFactory(NFSv4OperationFactory operationFactory) {
            this.operationFactory = operationFactory;
            return this;
        }

        public Builder withVfs(VirtualFileSystem vfs) {
            this.vfs = vfs;
            return this;
        }

        public Builder withLockManager(LockManager nlm) {
            this.nlm = nlm;
            return this;
        }

        public Builder withExportFile(ExportFile exportFile) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
            this.exportFile = exportFile;
            return this;
        }

        public Builder withStateHandler(NFSv4StateHandler stateHandler) {
            this.stateHandler = stateHandler;
            return this;
        }

        public NFSServerV41 build() {
            return new NFSServerV41(this);
        }
    }
}
