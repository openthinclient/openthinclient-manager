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
 ******************************************************************************/
package org.openthinclient.common.model.schema.provider;

import org.openthinclient.common.model.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link SchemaProvider} implementation that uses the local filesystem to locate schema files.
 */
public class ServerLocalSchemaProvider extends AbstractSchemaProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerLocalSchemaProvider.class);
  private final Path baseDirectory;

  public ServerLocalSchemaProvider(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
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

    try {
      loadFromFile(schemas, baseDirectory.resolve(profileTypeName + ".xml"));
    } catch (Throwable e) {
      throw new SchemaLoadingException(
              "Could not fetch schema from file service", e);
    }
    return schemas;
  }

  /**
   * @param schemas
   * @throws IOException
   * @throws SchemaLoadingException
   */
  private void loadFromFile(List<Schema> schemas, Path f) throws SchemaLoadingException {
    LOGGER.debug("Trying to load schema from {}", f);

    if (Files.isRegularFile(f) && Files.isReadable(f))
      try (InputStream in = Files.newInputStream(f)) {
        schemas.add(loadSchema(in));
      } catch (IOException e) {
        throw new SchemaLoadingException("Schema couldn't be loaded!", e);
      }
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

    try {
      Path dir = baseDirectory.resolve(profileTypeName);
      LOGGER.debug("Trying to load all schemas for {} from {}", profileTypeName, dir);

      Files.list(dir).filter(
              (f) -> {
                return (Files.isRegularFile(f) && Files.isReadable(f) && f.endsWith(".xml"));
              }
      ).forEach((f) -> loadFromFile(schemas, f));

    } catch (FileNotFoundException | NoSuchFileException e) {
      LOGGER.debug("No schemas found for " + profileTypeName, e);
    } catch (Throwable e) {
      LOGGER.error("Could not fetch schema from file service", e);
      throw new SchemaLoadingException(
              "Could not fetch schema from file service", e);
    }

    return schemas;
  }
}
