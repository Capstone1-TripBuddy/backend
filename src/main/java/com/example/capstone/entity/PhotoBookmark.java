package com.example.capstone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "photo_bookmark", schema = "minbak_db")
public class PhotoBookmark {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)  // ID를 자동으로 생성
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumns({
      @JoinColumn(name = "group_id", referencedColumnName = "group_id", nullable = false),
      @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
  })
  private GroupMember groupMember;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "photo_id", nullable = false)
  private Photo photo;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  public PhotoBookmark(final GroupMember groupMember, final Photo photo) {
    this.groupMember = groupMember;
    this.photo = photo;
  }

}