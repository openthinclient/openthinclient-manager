package org.openthinclient.pkgmgr.op;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.openthinclient.pkgmgr.db.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class PackageOperationInstall implements PackageOperation {

    private static final Logger LOG = LoggerFactory.getLogger(PackageOperationInstall.class);

    private final Package pkg;
    private final Path localPackageFile;

    public PackageOperationInstall(Package pkg, Path localPackageFile) {
        this.pkg = pkg;
        this.localPackageFile = localPackageFile;
    }

    private int findAREntry(String segmentName, EntryCallback callback) throws IOException {
        final ArArchiveInputStream ais = new ArArchiveInputStream(Files.newInputStream(localPackageFile));

        ArArchiveEntry e;
        int callbackCount = 0;
        while ((e = ais.getNextArEntry()) != null)
            if (e.getName().equals(segmentName)) {
                callback.handleEntry(e.getName(), ais);
                callbackCount++;
            }

        ais.close();

        return callbackCount;
    }

    @Override
    public Package getPackage() {
        return pkg;
    }

    @Override
    public void execute(PackageOperationContext context) throws IOException {

        if (findAREntry("data.tar.gz", (entry, ais) -> {
            final TarArchiveInputStream tis = new TarArchiveInputStream(new GZIPInputStream(ais));
            TarArchiveEntry t;
            while ((t = tis.getNextTarEntry()) != null)
                installFile(tis, t, context);
        }) == 0) {
            throw new IOException("Illegal package format. Missing data.tar.gz");
        }
    }

    @SuppressWarnings("unchecked")
    private void installFile(TarArchiveInputStream tis, TarArchiveEntry t, PackageOperationContext context)
            throws IOException {

        final Path relativePath = Paths.get(t.getName());

        if (System.getProperty("os.name").toUpperCase().contains("WINDOWS") && t.getFile().getPath().contains("::"))
            throw new IOException();

        if (t.isFile()) {
            try (final OutputStream os = context.createFile(relativePath)) {
                IOUtils.copy(tis, os);
            }

        } else if (t.isDirectory()) {
            context.createDirectory(relativePath);

        } else if (t.isLink() || t.isSymbolicLink()) {
            // FIXME shouldn't we distinguish between hard and soft links?

            context.createSymlink(relativePath, Paths.get(t.getLinkName()));
        }
        // FIXME warn about unknown entries!
    }

    private interface EntryCallback {

        void handleEntry(String s, InputStream inputstream) throws IOException;
    }

}
