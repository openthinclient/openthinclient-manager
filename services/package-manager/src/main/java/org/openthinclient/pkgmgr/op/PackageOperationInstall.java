package org.openthinclient.pkgmgr.op;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContent;
import org.openthinclient.progress.ProgressReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class PackageOperationInstall implements PackageOperation {

    private static final Logger LOG = LoggerFactory.getLogger(PackageOperationInstall.class);

    private final Package pkg;

    public PackageOperationInstall(Package pkg) {
        this.pkg = pkg;
    }

    private int findAREntry(String segmentName, Path localPackageFile, EntryCallback callback) throws IOException {
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
    public void execute(PackageOperationContext context, ProgressReceiver progressReceiver) throws IOException {

        progressReceiver.progress("Installing package '" + pkg.getName() + "'");
        final Path localPackageFile = context.getLocalPackageRepository().getPackage(pkg);

        if (findAREntry("data.tar.gz", localPackageFile, (entry, ais) -> {
            final TarArchiveInputStream tis = new TarArchiveInputStream(new GZIPInputStream(ais));
            TarArchiveEntry t;
            final List<PackageInstalledContent> installedContents = new ArrayList<>();

            while ((t = tis.getNextTarEntry()) != null) {
                final PackageInstalledContent installedContent = installFile(tis, t, context);

                if (installedContent != null)
                    installedContents.add(installedContent);
            }

            // store the installed contents

            int sequenceNumber = 0;
            for (PackageInstalledContent e : installedContents) {
                e.setPackage(pkg);
                e.setSequence(sequenceNumber++);
            }
            pkg.setInstalled(true);
            context.getDatabase().getPackageRepository().save(pkg);
            context.getDatabase().getInstalledContentRepository().save(installedContents);

        }) == 0) {
            throw new IOException("Illegal package format. Missing data.tar.gz");
        }
    }

    @SuppressWarnings("unchecked")
    private PackageInstalledContent installFile(TarArchiveInputStream tis, TarArchiveEntry t, PackageOperationContext context)
            throws IOException {

        // skipping the root directory entry
        if (t.isDirectory() && t.getName().equals("./"))
            return null;

        final String name;
        if (t.getName().startsWith("./"))
            name = t.getName().substring(2);
        else
            name = t.getName();

        final Path relativePath = Paths.get(name);

        // FIXME Francois: t.getFile() == null on first-installation, add IOException throws message please
        if (System.getProperty("os.name").toUpperCase().contains("WINDOWS") &&
            t.getFile() != null && t.getFile().getPath() != null && t.getFile().getPath().contains("::"))
            throw new IOException();

        final PackageInstalledContent installedContent = new PackageInstalledContent();
        installedContent.setPath(relativePath);

        if (t.isFile()) {
            final String sha1;
            try (final OutputStream os = context.createFile(relativePath); final DigestOutputStream digestOut = new DigestOutputStream(os, getSha1MessageDigest())) {
                IOUtils.copy(tis, digestOut);

                sha1 = PackageOperationDownload.byteArrayToHexString(digestOut.getMessageDigest().digest()).toLowerCase();
            }

            installedContent.setType(PackageInstalledContent.Type.FILE);
            installedContent.setSha1(sha1);
        } else if (t.isDirectory()) {

            context.createDirectory(relativePath);
            installedContent.setType(PackageInstalledContent.Type.DIR);

        } else if (t.isLink() || t.isSymbolicLink()) {
            // FIXME shouldn't we distinguish between hard and soft links?
            context.createSymlink(relativePath, Paths.get(t.getLinkName()));
            installedContent.setType(PackageInstalledContent.Type.SYMLINK);
        } else {
            // FIXME anything we shall do about unsupported contents?
            LOG.error("Unsupported type of TAR content: " + t.getName());
            return null;
        }

        return installedContent;
    }

    private MessageDigest getSha1MessageDigest() {
        try {
            return MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing SHA1 message digest implementation", e);
        }
    }

    private interface EntryCallback {

        void handleEntry(String s, InputStream inputstream) throws IOException;
    }

}
