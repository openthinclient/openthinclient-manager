package org.openthinclient.pkgmgr.db;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * An entity meant to "track" installation operations of the package manager.
 */
@Entity
@Table(name="otc_installation")
public class Installation {

   @Column(nullable = false)
   @Id
   private int id;

   /**
    * An optional user provided comment.
    */
   @Column
   private String comment;

   /**
    * The point in time at which this installation started.
    */
   @Column(nullable = false)
   private LocalDateTime start;

   /**
    * The point in time at which this installation ended.
    */
   @Column
//   @Temporal(TemporalType.TIMESTAMP)
   private LocalDateTime end;

   public int getId() {
      return id;
   }

   public void setId(final int id) {
      this.id = id;
   }

   public String getComment() {
      return comment;
   }

   public void setComment(final String comment) {
      this.comment = comment;
   }

   public LocalDateTime getStart() {
      return start;
   }

   public void setStart(final LocalDateTime start) {
      this.start = start;
   }

   public LocalDateTime getEnd() {
      return end;
   }

   public void setEnd(final LocalDateTime end) {
      this.end = end;
   }
}
