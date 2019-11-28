package org.openthinclient.clientmgr.db;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

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
   private String description;

   @Column(length = 100, columnDefinition = "char")
   @Enumerated(EnumType.STRING)
   private Type type;

   /**
    * Member of Item: this item has members i.e.:
    * <li>Item=Application has members=[AppGroup1, AppGroup2]</li>
    * <li>Item=Device has members=[ClientAutoLogin, ClientX]</li>
    * <li>Item=HardwareType has members=[ClientShowRoomPC]</li>
    * <li>Item=AppGroup1 has members=[AppGroup2, AppGroup3, ClientShowRoomPC]</li>
    */
   @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.DETACH}, fetch = FetchType.LAZY)
   @JoinTable(name = "otc_item_member",
       joinColumns = @JoinColumn(name = "item_id", referencedColumnName = "id"),
       inverseJoinColumns = @JoinColumn(name = "member_id", referencedColumnName = "id"))
   private Set<Item> members = new HashSet<>();

   public Item() {}

   public Item(String name, String description, Type type) {
      this.name = name;
      this.description = description;
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

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
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
      CLIENT, DEVICE, APPLICATION, HARDWARETYPE, LOCATION, PRINTER, REALM, CLIENT_GROUP, APPLICATION_GROUP
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("Item id=")
          .append(id)
          .append(", name=").append(name)
          .append(", description=").append(description)
          .append(", type=").append(type);
      return sb.toString();
   }
}
