package com.example.capstone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class GroupPhotoActivity {

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

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "activity_type_id", nullable = false)
  private PhotoActivityType activityType;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at")
  private LocalDateTime createdAt;


  public GroupPhotoActivity(final GroupMember groupMember, final Photo photo, final PhotoActivityType activityType) {
    this.groupMember = groupMember;
    this.photo = photo;
    this.activityType = activityType;
  }
}
