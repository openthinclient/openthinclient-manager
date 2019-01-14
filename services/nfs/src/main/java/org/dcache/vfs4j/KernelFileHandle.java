package org.dcache.vfs4j;

import com.google.common.io.BaseEncoding;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.dcache.nfs.vfs.Inode;

import static com.google.common.base.Preconditions.checkArgument;

/**
 *
 */
public class KernelFileHandle {

    // stolen from /usr/include/bits/fcntl-linux.h
    public final static int MAX_HANDLE_SZ = 128;

    private final byte[] handleData;

    protected KernelFileHandle(byte[] bytes) {

        checkArgument(bytes.length >= 8);
        int len = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).getInt(0);
        if (len + 8 == bytes.length) {
            // file handle has the correct size
            handleData = bytes;
        } else {
            handleData = Arrays.copyOfRange(bytes, 0, 8 + len);
        }
    }

    protected KernelFileHandle(Inode inode) {
        handleData = inode.getFileId();
    }

    byte[] toBytes() {
        return handleData;
    }

    Inode toInode() {
        return Inode.forFile(handleData);
    }

    @Override
    public java.lang.String toString() {
        return "[" + BaseEncoding.base16().lowerCase().encode(handleData) + "],"
                + " len = " + handleData.length;
    }
}
