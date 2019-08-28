package org.openthinclient.pkgmgr.db;

import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

import javax.persistence.*;

/**
 * An entity meant to "track" installation operations of the package manager.
 */
@Entity
@Table(name="otc_installation")
public class Installation {

   @Column(nullable = false)
   @Id
   @GeneratedValue(strategy= GenerationType.TABLE)
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
    * Note: this is named to "END_" because ApacheDerby cannot deal with 'end'-keyword
    */
   @Column(name="END_")
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
