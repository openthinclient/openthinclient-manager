/*
 * Copyright (c) 2009 - 2018 Deutsches Elektronen-Synchroton,
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
package org.dcache.oncrpc4j.rpc;

import org.dcache.oncrpc4j.xdr.BadXdrOncRpcException;
import org.dcache.oncrpc4j.xdr.XdrDecodingStream;
import org.dcache.oncrpc4j.xdr.XdrEncodingStream;
import org.dcache.oncrpc4j.xdr.XdrAble;

public class RpcMessage implements XdrAble {

    private int _xid;
    private int _type;

    RpcMessage(XdrDecodingStream xdr) throws BadXdrOncRpcException {
        this.xdrDecode(xdr);
    }

    RpcMessage(int xid, int type) {
        _xid = xid;
        _type = type;
    }

    public void xdrDecode(XdrDecodingStream xdr) throws BadXdrOncRpcException {
        _xid = xdr.xdrDecodeInt();
        _type = xdr.xdrDecodeInt();
    }

    public void xdrEncode(XdrEncodingStream xdr) {
        xdr.xdrEncodeInt(_xid);
        xdr.xdrEncodeInt(_type);
    }

    public int xid() {
        return _xid;
    }

    public int type() {
        return _type;
    }
}
