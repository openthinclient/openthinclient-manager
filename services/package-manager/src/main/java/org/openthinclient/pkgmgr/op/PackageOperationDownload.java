package org.openthinclient.pkgmgr.op;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import org.apache.commons.io.FileUtils;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.PackageChecksumVerificationFailedException;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.progress.ProgressReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PackageOperationDownload implements PackageOperation {

    private static final Logger LOG = LoggerFactory.getLogger(PackageOperationDownload.class);

    private final DownloadManager downloadManager;
    private final Package pkg;

    public PackageOperationDownload(Package pkg, DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
        this.pkg = pkg;
    }

    // TODO move this method to a proper utility class
    public static String byteArrayToHexString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte aB : bytes) {
            final int v = aB & 0xff;
            if (v < 16)
                sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

    @Override
    public Package getPackage() {
        return pkg;
    }


    protected MessageDigest getMD5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // this should never happen as MD5 has to be supported by all JVMs
            throw new RuntimeException("Broken JVM. No MD5 message digest available");
        }
    }

    @Override
    public void execute(PackageOperationContext context, ProgressReceiver progressReceiver) throws IOException {

        progressReceiver.progress("Downloading package '" + pkg.getName() + "' " + FileUtils.byteCountToDisplaySize(pkg.getSize()));

        URL sourceUrl = pkg.getSource().getUrl();
        if (!sourceUrl.toExternalForm().endsWith("/"))
            sourceUrl = new URL(sourceUrl.toExternalForm() + "/");
        final URL packageURL = new URL(sourceUrl, pkg.getFilename());

        LOG.info("Downloading package {}", packageURL);

        context.getLocalPackageRepository().addPackage(pkg, targetPath -> {
            downloadManager.download(packageURL, in -> {
                try (
                        final OutputStream out = Files.newOutputStream(targetPath);
                        final DigestOutputStream digestOut = new DigestOutputStream(out, getMD5Digest())) {

                    ByteStreams.copy(in, digestOut);

                    digestOut.flush();
                    final MessageDigest digest = digestOut.getMessageDigest();
                    final String md5sum = byteArrayToHexString(digest.digest());

                    LOG.info("Download of {} complete. Computed MD5 {}", packageURL, md5sum);

                    if (!Strings.isNullOrEmpty(pkg.getMD5sum()) && !pkg.getMD5sum().equalsIgnoreCase(md5sum)) {
                        LOG.error("Checksum validation failed. Exected {}, actual {}", pkg.getMD5sum().toLowerCase(), md5sum.toLowerCase());
                        throw new PackageChecksumVerificationFailedException("checksum validation failed", pkg, md5sum);
                    }

                }
                return null;
            }, progressReceiver);
        });

    }

}
