package org.openthinclient.pkgmgr.db;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * An entity meant to "track" installation operations of the package manager.
 */
@Entity
@Table(name="otc_installation")
public class Installation {

   @Column(nullable = false)
   @Id
   @GeneratedValue
   private Long id;

   /**
    * An optional user provided comment.
    */
   @Column
   @Lob
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

   public Long getId() {
      return id;
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
