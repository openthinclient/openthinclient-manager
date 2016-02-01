package org.openthinclient.pkgmgr.db;

import org.openthinclient.util.dpkg.PackageReferenceList;
import org.openthinclient.util.dpkg.PackageReferenceListParser;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class PackageReferenceListToStringConverter implements AttributeConverter<PackageReferenceList, String> {

   @Override
   public String convertToDatabaseColumn(PackageReferenceList attribute) {

      if (attribute == null)
         return "";

      return attribute.toString();

   }

   @Override
   public PackageReferenceList convertToEntityAttribute(String dbData) {
      return new PackageReferenceListParser().parse(dbData);
   }
}
