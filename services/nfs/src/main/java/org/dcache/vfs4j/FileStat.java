package org.dcache.vfs4j;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import jnr.ffi.Struct;

public class FileStat extends Struct {

    private static final DateTimeFormatter LS_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd HH:mm");

    public final Signed64 st_dev = new Signed64();      /* Device.  */
    public final Signed64 st_ino = new Signed64();         /* File serial number.	*/
    public final Signed64 st_nlink = new Signed64();     /* Link count.  */
    public final Signed32 st_mode = new Signed32();       /* File mode.  */
    public final Signed32 st_uid = new Signed32();      /* User ID of the file's owner.	*/
    public final Signed32 st_gid = new Signed32();      /* Group ID of the file's group.*/
    public final Signed64 st_rdev = new Signed64();     /* Device number, if device.  */
    public final Signed64 st_size = new Signed64();        /* Size of file, in bytes.  */
    public final Signed64 st_blksize = new Signed64();  /* Optimal block size for I/O.  */
    public final Signed64 st_blocks = new Signed64();   /* Number 512-byte blocks allocated. */
    public final Signed64 st_atime = new Signed64();     // Time of last access (time_t)
    public final Signed64 st_atimensec = new Signed64(); // Time of last access (nanoseconds)
    public final Signed64 st_mtime = new Signed64();     // Last data modification time (time_t)
    public final Signed64 st_mtimensec = new Signed64(); // Last data modification time (nanoseconds)
    public final Signed64 st_ctime = new Signed64();     // Time of last status change (time_t)
    public final Signed64 st_ctimensec = new Signed64(); // Time of last status change (nanoseconds)
    public final Signed64 __unused4 = new Signed64();
    public final Signed64 __unused5 = new Signed64();
    public final Signed64 __unused6 = new Signed64();

    public static final int S_IFIFO = 0010000;  // named pipe (fifo)
    public static final int S_IFCHR = 0020000;  // character special
    public static final int S_IFDIR = 0040000;  // directory
    public static final int S_IFBLK = 0060000;  // block special
    public static final int S_IFREG = 0100000;  // regular
    public static final int S_IFLNK = 0120000;  // symbolic link
    public static final int S_IFSOCK = 0140000; // socket
    public static final int S_IFMT = 0170000;   // file mask for type checks
    public static final int S_ISUID = 0004000;  // set user id on execution
    public static final int S_ISGID = 0002000;  // set group id on execution
    public static final int S_ISVTX = 0001000;  // save swapped text even after use
    public static final int S_IRUSR = 0000400;  // read permission, owner
    public static final int S_IWUSR = 0000200;  // write permission, owner
    public static final int S_IXUSR = 0000100;  // execute/search permission, owner
    public static final int S_IRGRP = 0000040;  // read permission, group
    public static final int S_IWGRP = 0000020;  // write permission, group
    public static final int S_IXGRP = 0000010;  // execute/search permission, group
    public static final int S_IROTH = 0000004;  // read permission, other
    public static final int S_IWOTH = 0000002;  // write permission, other
    public static final int S_IXOTH = 0000001;  // execute permission, other

    public static final int ALL_READ = S_IRUSR | S_IRGRP | S_IROTH;
    public static final int ALL_WRITE = S_IWUSR | S_IWGRP | S_IWOTH;
    public static final int S_IXUGO = S_IXUSR | S_IXGRP | S_IXOTH;

    protected FileStat(jnr.ffi.Runtime runtime) {
        super(runtime);

    }

    public static boolean S_ISTYPE(int mode, int mask) {
        return (mode & S_IFMT) == mask;
    }

    public static boolean S_ISDIR(int mode) {
        return S_ISTYPE(mode, S_IFDIR);
    }

    public static boolean S_ISCHR(int mode) {
        return S_ISTYPE(mode, S_IFCHR);
    }

    public static boolean S_ISBLK(int mode) {
        return S_ISTYPE(mode, S_IFBLK);
    }

    public static boolean S_ISREG(int mode) {
        return S_ISTYPE(mode, S_IFREG);
    }

    public static boolean S_ISFIFO(int mode) {
        return S_ISTYPE(mode, S_IFIFO);
    }

    public static boolean S_ISLNK(int mode) {
        return S_ISTYPE(mode, S_IFLNK);
    }

    public static java.lang.String modeToString(int mode) {
        StringBuilder result = new StringBuilder(10);
        switch (mode & S_IFMT) {
            case S_IFBLK:
                result.append("b");
                break;
            case S_IFCHR:
                result.append("c");
                break;
            case S_IFDIR:
                result.append("d");
                break;
            case S_IFIFO:
                result.append("p");
                break;
            case S_IFSOCK:
                result.append("s");
                break;
            case S_IFLNK:
                result.append("l");
                break;
            case S_IFREG:
                result.append("-");
                break;
            default:
                result.append("?");
        }

        //owner, group, other
        for (int i = 0; i < 3; i++) {
            int acl = (mode >> (6 - 3 * i)) & 0000007;
            switch (acl) {
                case 00:
                    result.append("---");
                    break;
                case 01:
                    result.append("--x");
                    break;
                case 02:
                    result.append("-w-");
                    break;
                case 03:
                    result.append("-wx");
                    break;
                case 04:
                    result.append("r--");
                    break;
                case 05:
                    result.append("r-x");
                    break;
                case 06:
                    result.append("rw-");
                    break;
                case 07:
                    result.append("rwx");
                    break;
            }
        }
        return result.toString();
    }

    //technically _size (java long) will overflow after ~8 exabytes, so "Z"/"Y" is unreachable
    private final static java.lang.String[] SIZE_UNITS = {"", "K", "M", "G", "T", "P", "E", "Z", "Y"};

    public static java.lang.String sizeToString(long bytes) {
        if (bytes == 0) {
            return "0";
        }
        int orderOfMagnitude = (int) Math.floor(Math.log(bytes) / Math.log(1024));
        double significantSize = (double) bytes / (1L << orderOfMagnitude * 10);
        DecimalFormat sizeFormat = new DecimalFormat("#.#"); //not thread safe
        return sizeFormat.format(significantSize) + SIZE_UNITS[orderOfMagnitude];
    }

    /**
     * @return the equivalent of "ls -lh" (as close as possible)
     */
    @Override
    public java.lang.String toString() {
        java.lang.String humanReadableSize = sizeToString(st_size.get());
        java.lang.String humanReadableMTime = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(st_mtime.get()), ZoneId.systemDefault())
                .format(LS_TIME_FORMAT);
        return modeToString(st_mode.get()) + " " + java.lang.String.format("%4d %4d %4d %4s %s",
                st_nlink.get(), st_uid.get(), st_gid.get(), humanReadableSize, humanReadableMTime);
    }
}
