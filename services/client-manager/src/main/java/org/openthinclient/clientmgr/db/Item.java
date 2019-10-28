package org.openthinclient.clientmgr.db;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * An entity for items.
 */
@Entity
@Table(name="otc_item")
public class Item {

   @Column(nullable = false)
   @Id
   @GeneratedValue(strategy= GenerationType.AUTO, generator="native")
   @GenericGenerator(name = "native", strategy = "native")
   private Long id;

   /**
    *  Name
    */
   @Column(length = 1024, columnDefinition = "char")
   private String name;

   /**
    * An optional user provided comment.
    */
   @Column
   @Lob
   private String comment;

   /**
    *  Type
    */
   @Column(length = 100, columnDefinition = "char")
   @Enumerated(EnumType.STRING)
   private Type type;

   public Long getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getComment() {
      return comment;
   }

   public void setComment(String comment) {
      this.comment = comment;
   }

   public Type getType() {
      return type;
   }

   public void setType(Type type) {
      this.type = type;
   }

   public enum Type {
      CLIENT, DEVICE, APPLICATION, HARDWARE_TYPE, LOCATION, PRINTER, REALM, CLIENT_GROUP, APPLICATION_GROUP
   }
}
