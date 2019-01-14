package org.dcache.vfs4j;

import static java.nio.charset.StandardCharsets.UTF_8;
import jnr.ffi.Struct;

public class Dirent extends Struct {

    private static final int MAX_NAME_LEN = 255;

    public Dirent(jnr.ffi.Runtime runtime) {
        super(runtime);
    }

    public  java.lang.String getName() {
        byte[] b = new byte[MAX_NAME_LEN];
        int i = 0;
        for (; d_name[i].get() != '\0'; i++) {
            b[i] = d_name[i].get();
        }

        return new java.lang.String(b, 0, i, UTF_8);
    }

    public final Signed64 d_ino = new Signed64();
    public final Signed64 d_off = new Signed64();
    public final Unsigned16 d_reclen = new Unsigned16();
    public final Unsigned8 d_type = new Unsigned8();
    public final Signed8[] d_name = array(new Signed8[MAX_NAME_LEN]);
}
