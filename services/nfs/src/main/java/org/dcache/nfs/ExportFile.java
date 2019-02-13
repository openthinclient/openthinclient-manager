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
package org.dcache.nfs;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.dcache.nfs.v4.xdr.layouttype4;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.throwIfInstanceOf;
import static com.google.common.base.Throwables.throwIfUnchecked;

/**
 * NFS server export table.
 */
public class ExportFile {

    private static final Logger _log = LoggerFactory.getLogger(ExportFile.class);

    private volatile ImmutableMultimap<Integer, FsExport> _exports;
    private final Callable<URI[]> _exportFileProvider;

    /**
     * Construct server export table from a given file.
     *
     * @param file the file that contains the export table.
     * @throws IOException
     */
    public ExportFile(File file) throws IOException {
        this(file, null);
    }

    /**
     * /**
     * Construct server export table from a given file. The {@code dir} points
     * to a directory with extra export tables. Only files ending in .exports
     * are considered. Files beginning with a dot are ignored.
     *
     * @param file the file that contains the export table.
     * @param dir the directory with extra export tables.
     * @throws IOException
     */
    public ExportFile(File file, File dir) throws IOException {
        _exportFileProvider = () -> {
            if (dir != null && dir.exists()) {

                checkArgument(dir.isDirectory(), dir.getAbsolutePath() + " exist but not a directory");

                File[] files = dir.listFiles((File d, String n) -> n.endsWith(".exports") && n.charAt(0) != '.');
                URI[] exportFiles = new URI[files.length + 1];
                exportFiles[0] = file.toURI();
                for (int i = 0; i < files.length; i++) {
                    exportFiles[i + 1] = files[i].toURI();
                }
                return exportFiles;
            } else {
                return new URI[]{file.toURI()};
            }
        };
        rescan();
    }

    public ExportFile(URI uri) throws IOException {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 1");
        _exportFileProvider = () -> {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 2");
            return new URI[]{uri};
        };
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 3");
        rescan();
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 4");
    }

    public ExportFile(Reader reader) throws IOException {
        _exportFileProvider = () -> {
            throw new IllegalStateException("exports uri not set, rescan impossible");
        };
        _exports = parse(reader);
    }

    public Stream<FsExport> getExports() {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 5");
        return _exports.values().stream();
    }

    private static ImmutableMultimap<Integer, FsExport> parse(Reader reader) throws IOException {
        List<String> lines;
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            lines = bufferedReader.lines().collect(Collectors.toList());
        }
        return parseExportLines(lines);
    }

    private static ImmutableMultimap<Integer, FsExport> parse(URI... exportFiles) throws IOException {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 6");
        ImmutableListMultimap.Builder<Integer, FsExport> exportsBuilder = ImmutableListMultimap.builder();
        for (URI exportFile : exportFiles) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 7");
            List<String> lines = Files.readAllLines(Paths.get(exportFile));
            Multimap<Integer, FsExport> export = parseExportLines(lines);
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + export);
            exportsBuilder.putAll(export);
        }
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 8");

        /*
         * sort in reverse order to get smallest network first
         */
        return exportsBuilder
                .orderValuesBy(Ordering.from(HostEntryComparator::compare).onResultOf(FsExport::client).reverse())
                .build();
    }

    private static ImmutableMultimap<Integer, FsExport> parseExportLines(Iterable<String> lines) throws IOException {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 9");

        ImmutableListMultimap.Builder<Integer, FsExport> exportsBuilder = ImmutableListMultimap.builder();

        for (String line : lines) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 10");

            line = line.trim();
            if (line.length() == 0) {
                continue;
            }

            if (line.charAt(0) == '#') {
                continue;
            }

            if (line.charAt(0) != '/') {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": LOG???");
                _log.warn("Ignoring entry with non absolute export path: " + line);
                continue;
            }

            int pathEnd = line.indexOf(' ');

            String path;
            if (pathEnd < 0) {
                FsExport export = new FsExport.FsExportBuilder().build(line);
                exportsBuilder.put(export.getIndex(), export);
                continue;
            } else {
                path = line.substring(0, pathEnd);
            }

            Splitter splitter = Splitter.on(' ').omitEmptyStrings().trimResults();

System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 11");
            for (String hostAndOptions : splitter.split(line.substring(pathEnd + 1))) {

System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 12");
                try {
                    FsExport.FsExportBuilder exportBuilder = new FsExport.FsExportBuilder();

                    Iterator<String> s = Splitter
                            .on(CharMatcher.anyOf("(,)"))
                            .omitEmptyStrings()
                            .trimResults()
                            .split(hostAndOptions).iterator();

                    String host = s.next();

                    exportBuilder.forClient(host);
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 13");
                    while (s.hasNext()) {
                        String option = s.next();

                        if (option.equals("rw")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 14");
                            exportBuilder.rw();
                            continue;
                        }

                        if (option.equals("ro")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 15");
                            exportBuilder.ro();
                            continue;
                        }

                        if (option.equals("root_squash")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 16");
                            exportBuilder.notTrusted();
                            continue;
                        }

                        if (option.equals("no_root_squash")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 17");
                            exportBuilder.trusted();
                            continue;
                        }

                        if (option.equals("acl")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 18");
                            exportBuilder.withAcl();
                            continue;
                        }

                        if (option.equals("noacl") || option.equals("no_acl")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 19");
                            exportBuilder.withoutAcl();
                            continue;
                        }

                        if (option.equals("all_squash")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 20");
                            exportBuilder.allSquash();
                            continue;
                        }

                        if (option.startsWith("sec=")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 21");
                            String secFlavor = option.substring(4);
                            exportBuilder.withSec(FsExport.Sec.valueOf(secFlavor.toUpperCase()));
                            continue;
                        }

                        if (option.startsWith("anonuid=")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 22");
                            int anonuid = Integer.parseInt(option.substring(8));
                            exportBuilder.withAnonUid(anonuid);
                            continue;
                        }

                        if (option.startsWith("anongid=")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 23");
                            int anongid = Integer.parseInt(option.substring(8));
                            exportBuilder.withAnonGid(anongid);
                            continue;
                        }

                        if (option.equals("dcap")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 24");
                            exportBuilder.withDcap();
                            continue;
                        }

                        if (option.equals("no_dcap")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 25");
                            exportBuilder.withoutDcap();
                            continue;
                        }

                        if (option.equals("all_root")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 26");
                            exportBuilder.withAllRoot();
                            continue;
                        }

                        if (option.equals("pnfs")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 27");
                            exportBuilder.withPnfs();
                            continue;
                        }

                        if (option.equals("nopnfs") || option.equals("no_pnfs")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 28");
                            exportBuilder.withoutPnfs();
                            continue;
                        }

                        if (option.startsWith("lt=")) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 29");
                            Iterable<String> lt = Splitter.on(":")
                                    .omitEmptyStrings()
                                    .split(option.substring(3));

                            StreamSupport.stream(lt.spliterator(), false)
                                    .map(String::toUpperCase)
                                    .map(t -> "LAYOUT4_" + t)
                                    .map(layouttype4::valueOf)
                                    .forEach(exportBuilder::withLayoutType);
                            continue;
                        }

System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 30");
                        throw new IllegalArgumentException("Unsupported option: " + option);
                    }
                    FsExport export = exportBuilder.build(path);
                    exportsBuilder.put(export.getIndex(), export);
                } catch (IllegalArgumentException e) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": LOG???");
                    _log.error("Invalid export entry [" + hostAndOptions + "] : " + e.getMessage());
                }
            }
        }
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 31");

        /*
         * sort in reverse order to get smallest network first
         */
        return exportsBuilder
                .orderValuesBy(Ordering.from(HostEntryComparator::compare).onResultOf(FsExport::client).reverse())
                .build();
    }

    public FsExport getExport(String path, InetAddress client) {
        String normalizedPath = FsExport.normalize(path);
        return getExport(FsExport.getExportIndex(normalizedPath), client);
    }

    public FsExport getExport(int index, InetAddress client) {
        for (FsExport export : _exports.get(index)) {
            if (export.isAllowed(client)) {
                return export;
            }
        }
        return null;
    }

    public Stream<FsExport> exportsFor(InetAddress client) {
        return _exports.values().stream().filter(e -> e.isAllowed(client));
    }

    public final void rescan() throws IOException {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 32");
        try {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 33");
            _exports = parse(_exportFileProvider.call());
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 34");
        } catch (Exception e) {
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 35");
            throwIfInstanceOf(e, IOException.class);
            throwIfUnchecked(e);
            throw new RuntimeException("Unhandled exception", e);
        }
System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": 36");
    }
}
