package org.openthinclient.service.nfs;

import java.io.File;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

public interface NFS {
  void addExport(String exportSpec) throws UnknownHostException;

  void addExport(NFSExport export);

  boolean removeExport(String name);

  boolean moveLocalFile(File from, File to);

  boolean moveMoreFiles(HashMap<File, File> fromToMap);

  boolean removeFilesFromNFS(List<File> fileList);
}
