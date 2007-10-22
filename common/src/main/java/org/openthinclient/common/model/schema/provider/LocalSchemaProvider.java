/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *******************************************************************************/
package org.openthinclient.common.model.schema.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.openthinclient.common.model.schema.Schema;


/**
 * @author Natalie Bohnert Provides schema handling from local filesystem.
 */
public class LocalSchemaProvider extends AbstractSchemaProvider {

  private static LocalSchemaProvider schemaHelper;

  private String realPath;

  private LocalSchemaProvider() {
    String dirPath = System.getProperty("netbeans.dirs");
    System.out.println(dirPath);
    if (null == dirPath || dirPath.length() == 0)
      dirPath = "."; // best guess

    String[] dirs = dirPath.split(File.pathSeparator);
    for (int i = 0; i < dirs.length; i++) {
      File file = new File(dirs[i], SCHEMA_PATH);
      if (file.exists()) {
        realPath = file.getPath();
        System.out.println(realPath);
      }
    }

    if (realPath == null)
      realPath = SCHEMA_PATH;
  }

  public static LocalSchemaProvider getInstance() {
    if (schemaHelper == null) {
      schemaHelper = new LocalSchemaProvider();
    }
    return schemaHelper;
  }

  /**
   * @param profileTypeName
   * @return
   * @throws SchemaLoadingException
   */
  @Override
protected List<Schema> loadDefaultSchema(String profileTypeName)
      throws SchemaLoadingException {
    List<Schema> schemas = new ArrayList<Schema>();

    File defaultSchema = new File(realPath, profileTypeName + ".xml");
    if (defaultSchema.exists() && defaultSchema.isFile()) {
      try {
        schemas.add(loadSchema(new FileInputStream(defaultSchema)));
      } catch (FileNotFoundException e) {
        // should not happen, since we just checked that it exists
        throw new SchemaLoadingException("Schema " + defaultSchema
            + " doesnt't exist.");
      }
    } else {
      // try to load schema from classpath
      InputStream is = getClass().getResourceAsStream(
          "/" + profileTypeName + ".xml");
      if (null != is)
        schemas.add(loadSchema(is));
    }

    return schemas;
  }

  /**
   * @param profileTypeName
   * @return
   * @throws SchemaLoadingException
   */
  @Override
protected List<Schema> loadAllSchemas(String profileTypeName)
      throws SchemaLoadingException {
    List<Schema> schemas = new ArrayList<Schema>();

    File file = new File(realPath, profileTypeName);
    if (file.exists() && file.isDirectory()) {
      File[] files = file.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".xml");
        }
      });
      if (null != files)
        for (File f : files) {
          try {
            schemas.add(loadSchema(new FileInputStream(f)));
          } catch (FileNotFoundException e) {
            // should not happen, since we just retrieved all the names.
            throw new SchemaLoadingException("Schema " + f + " doesnt't exist.");
          }
        }
    }

    return schemas;
  }
}
