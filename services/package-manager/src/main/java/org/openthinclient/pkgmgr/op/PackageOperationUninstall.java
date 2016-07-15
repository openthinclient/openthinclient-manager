package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContent;
import org.openthinclient.pkgmgr.progress.ProgressReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Stream;

public class PackageOperationUninstall implements PackageOperation {

    private static final Logger LOG = LoggerFactory.getLogger(PackageOperationUninstall.class);

    private final Package pkgToUninstall;

    public PackageOperationUninstall(Package pkgToUninstall) {
        this.pkgToUninstall = pkgToUninstall;
    }

    @Override
    public Package getPackage() {
        return pkgToUninstall;
    }

    @Override
    public void execute(PackageOperationContext context, ProgressReceiver progressReceiver) throws IOException {

        progressReceiver.progress("Uninstalling package '" + pkgToUninstall.getName() + "'");

        final List<PackageInstalledContent> contents = context.getDatabase().getInstalledContentRepository().findByPkgOrderBySequenceDesc(pkgToUninstall);

        LOG.info("Uninstalling package {} {}", pkgToUninstall.getName(), pkgToUninstall.getVersion());

        for (PackageInstalledContent content : contents) {

            switch (content.getType()) {
                case FILE:
                    deleteFile(context, content);
                    break;
                case DIR:
                    deleteDirectory(context, content);
                    break;
                case SYMLINK:
                    context.delete(content.getPath());
                    break;
            }

        }

        pkgToUninstall.setInstalled(false);
        context.getDatabase().getPackageRepository().save(pkgToUninstall);
    }

    private void deleteDirectory(PackageOperationContext context, PackageInstalledContent content) throws IOException {
        Stream<Path> children = context.list(content.getPath());

        if (children.count() == 0) {
            LOG.info("Deleting empty directory {}", content.getPath());
            context.delete(content.getPath());
        } else {
            LOG.info("Skipping non empty directory {}", content.getPath());
        }
    }

    private void deleteFile(PackageOperationContext context, PackageInstalledContent content) throws IOException {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing SHA1 digest algorithm", e);
        }
        if (context.isRegularFile(content.getPath())) {
            try (InputStream in = context.newInputStream(content.getPath())) {
                int read;
                final byte[] bytes = new byte[10 * 1024];
                while ((read = in.read(bytes)) != -1) {
                    md.update(bytes, 0, read);
                }
            }
            final byte[] digest = md.digest();

            final String d = PackageOperationDownload.byteArrayToHexString(digest);

            if (d.equalsIgnoreCase(content.getSha1())) {
                LOG.info("Deleting unmodified file {}", content.getPath());
                context.delete(content.getPath());
            } else {
                LOG.warn("Not deleting modified file {}", content.getPath());
            }
        } else {
            LOG.warn("Previously installed file could not be found {}", content.getPath());
        }
    }
}
