package org.openthinclient.pkgmgr.op;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.PackageChecksumVerificationFailedException;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.util.dpkg.LocalPackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class PackageDownload {

    private static final Logger LOG = LoggerFactory.getLogger(PackageDownload.class);

    private final DownloadManager downloadManager;
    private final Package pkg;
    private final LocalPackageRepository localPackageRepository;

    public PackageDownload(Package pkg, DownloadManager downloadManager, LocalPackageRepository localPackageRepository) {
        this.downloadManager = downloadManager;
        this.pkg = pkg;
        this.localPackageRepository = localPackageRepository;
    }

    public static String byteArrayToHexString(byte[] b) {
        final StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            final int v = b[i] & 0xff;
            if (v < 16)
                sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

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

    public void execute() throws IOException {

        final URL packageURL = new URL(pkg.getSource().getUrl(), pkg.getFilename());

        LOG.info("Downloading package {}", packageURL);

        localPackageRepository.addPackage(pkg, targetPath -> {
            downloadManager.download(packageURL, in -> {
                try (
                        final OutputStream out = Files.newOutputStream(targetPath);
                        final DigestOutputStream digestOut = new DigestOutputStream(out, getMD5Digest())) {

                    ByteStreams.copy(in, digestOut);

                    digestOut.flush();
                    final MessageDigest digest = digestOut.getMessageDigest();
                    final String md5sum = byteArrayToHexString(digest.digest());

                    LOG.info("Download of {} complete. Computed MD5 {}", packageURL, md5sum);

                    if (!Strings.isNullOrEmpty(pkg.getMD5sum()) && Objects.equals(pkg.getMD5sum(), md5sum)) {
                        LOG.error("Checksum validation failed. Exected {}, actual {}", pkg.getMD5sum(), md5sum);
                        throw new PackageChecksumVerificationFailedException(pkg, md5sum);
                    }

                }
                return null;
            });
        });

    }

}
