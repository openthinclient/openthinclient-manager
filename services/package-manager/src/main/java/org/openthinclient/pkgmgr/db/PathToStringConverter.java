package org.openthinclient.pkgmgr.db;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.file.Path;
import java.nio.file.Paths;

@Converter(autoApply = true)
public class PathToStringConverter implements AttributeConverter<Path, String> {

   @Override public String convertToDatabaseColumn(final Path attribute) {
      return attribute.toString();
   }

   @Override public Path convertToEntityAttribute(final String dbData) {
      return Paths.get(dbData);
   }
}
