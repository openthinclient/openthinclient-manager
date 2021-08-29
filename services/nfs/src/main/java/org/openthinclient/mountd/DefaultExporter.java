/**
 * ****************************************************************************
 * openthinclient.org ThinClient suite
 * <p/>
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * <p/>
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * *****************************************************************************
 */

/*
 * This code is based on:
 * JNFSD - Free NFSD. Mark Mitchell 2001 markmitche11@aol.com
 * http://hometown.aol.com/markmitche11
 */
package org.openthinclient.mountd;

import org.openthinclient.service.nfs.NFSExport;

import java.io.File;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The default exporter simply exports all filesystem roots to anybody.
 *
 * @author levigo
 */
public class DefaultExporter implements Exporter {

  public NFSExport getExport(InetAddress peer, String mountRequest) {
    final String lowerCaseMountRequest = mountRequest.toLowerCase();

    final Optional<NFSExport> nfsExport = Stream.of(File.listRoots())
            .filter(root -> root.getAbsolutePath().toLowerCase().startsWith(lowerCaseMountRequest))
            .findFirst().map(this::createExport);
    return nfsExport.orElse(null);
  }

  private NFSExport createExport(File root) {
    final NFSExport export = new NFSExport();
    export.setName(root.getAbsolutePath());
    export.setRoot(root);
    return export;
  }

  public List<NFSExport> getExports() {
    return Stream.of(File.listRoots())
            .map(this::createExport)
            .collect(Collectors.toList());
  }
}
