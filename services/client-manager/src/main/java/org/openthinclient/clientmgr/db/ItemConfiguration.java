package org.openthinclient.clientmgr.db;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * An entity for item-configuration.
 */
@Entity
@Table(name="otc_item_configuration")
public class ItemConfiguration {

   @Column(nullable = false)
   @Id
   @GeneratedValue(strategy= GenerationType.AUTO, generator="native")
   @GenericGenerator(name = "native", strategy = "native")
   private Long id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "item_id")
   private Item item;

   @Column
   @Lob
   private String name;

   @Column
   @Lob
   private String value;

   @Column(length = 100, columnDefinition = "char")
   @Enumerated(EnumType.STRING)
   private Type type;

   public ItemConfiguration() {};

   public ItemConfiguration(Item item, String name, String value, Type type) {
      this.item = item;
      this.name = name;
      this.value = value;
      this.type = type;
   }

   public enum Type {
      STRING, INTEGER, BOOLEAN
   }

   public Long getId() {
      return id;
   }

   public Item getItem() {
      return item;
   }

   public void setItem(Item item) {
      this.item = item;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public Type getType() {
      return type;
   }

   public void setType(Type type) {
      this.type = type;
   }
}
