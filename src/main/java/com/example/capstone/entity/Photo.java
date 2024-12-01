package com.example.capstone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
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
@Table(name = "photo", schema = "minbak_db")
public class Photo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)  // ID를 자동으로 생성
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "group_id", nullable = false)
  private TravelGroup group;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "file_path")
  private String filePath;

  @Column(name = "taken_at")
  private LocalDateTime takenAt;

  @Column(name = "image_size")
  private Long imageSize;

  @Column(name = "image_format", length = 50)
  private String imageFormat;

  @Setter
  @Column(name = "photo_type", length = 50)
  private String photoType;

  @Setter
  @ColumnDefault("0")
  @Column(name = "has_face")
  private Boolean hasFace;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "uploaded_at")
  private LocalDateTime uploadedAt;

  @Setter
  @Column(name = "analyzed_at")
  private Instant analyzedAt;

  public Photo(final User user, final TravelGroup group,
      final String fileName, final String filePath, final LocalDateTime takenAt,
      final long size, final String contentType) {
    this.user = user;
    this.group = group;
    this.fileName = fileName;
    this.filePath = filePath;
    this.takenAt = takenAt;
    this.imageSize = size;
    this.imageFormat = contentType;
  }

}