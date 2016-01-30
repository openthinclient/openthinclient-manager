package org.openthinclient.pkgmgr.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.time.LocalDateTime;

@Entity
@Table(name="otc_installation")
public class Installation {

   @Column(nullable = false)
   @Id
   private int id;

   @Column
   private String comment;

   @Column(nullable = false)
   @Temporal(TemporalType.TIMESTAMP)
   private LocalDateTime start;

   @Column
   @Temporal(TemporalType.TIMESTAMP)
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
