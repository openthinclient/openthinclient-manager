package org.openthinclient.clientmgr.db;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

   @Column(length = 1024, columnDefinition = "char")
   private String name;

   @Column
   @Lob
   private String comment;

   @Column(length = 100, columnDefinition = "char")
   @Enumerated(EnumType.STRING)
   private Type type;

   @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
   @JoinTable(name = "otc_item_member",
       joinColumns = @JoinColumn(name = "item_id", referencedColumnName = "id"),
       inverseJoinColumns = @JoinColumn(name = "member_id", referencedColumnName = "id"))
   private Set<Item> members;

   public Item() {};

   public Item(String name, String comment, Type type) {
      this.name = name;
      this.comment = comment;
      this.type = type;
   }

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

   public Set<Item> getMembers() {
      return members;
   }

   public void setMembers(Set<Item> members) {
      this.members = members;
   }

   public enum Type {
      CLIENT, DEVICE, APPLICATION, HARDWARE_TYPE, LOCATION, PRINTER, REALM, CLIENT_GROUP, APPLICATION_GROUP
   }
}
