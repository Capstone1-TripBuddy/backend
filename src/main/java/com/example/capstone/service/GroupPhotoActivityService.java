package com.example.capstone.service;

import com.example.capstone.dto.ResponseGroupActivity;
import com.example.capstone.entity.GroupMember;
import com.example.capstone.entity.GroupPhotoActivity;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.PhotoActivityType;
import com.example.capstone.entity.User;
import com.example.capstone.repository.GroupMemberRepository;
import com.example.capstone.repository.GroupPhotoActivityRepository;
import com.example.capstone.repository.PhotoActivityTypeRepository;
import com.example.capstone.repository.PhotoRepository;
import com.example.capstone.repository.UserRepository;
import java.time.Instant;
import java.time.chrono.ChronoLocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupPhotoActivityService {

  private final GroupPhotoActivityRepository groupPhotoActivityRepository;
  private final PhotoActivityTypeRepository photoActivityTypeRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final PhotoRepository photoRepository;
  private final UserRepository userRepository;


  public List<ResponseGroupActivity> getGroupRecentActivity(Long groupId, Long userId) {
    // 현재 시간 기준 1시간 전 시간 계산
    Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

    // 그룹 ID에 해당하는 활동 리스트 조회
    List<GroupPhotoActivity> history = groupPhotoActivityRepository.findByGroupMemberGroupId(groupId);

    // 스트림 처리
    return history.stream()
        .filter(activity -> activity.getCreatedAt().isAfter(ChronoLocalDateTime.from(oneHourAgo))) // 1시간 이내 필터링
        .filter(activity -> !activity.getGroupMember().getUser().getId().equals(userId)) // 본인 활동 내역 필터링
        .map(activity -> {
          // User 엔티티 조회
          Optional<User> user = userRepository.findById(activity.getGroupMember().getUser().getId());

          // Optional 체크 후 ResponseGroupActivity 생성
          return user.map(value -> ResponseGroupActivity.fromEntity(
              value,
              activity.getPhoto(),
              activity.getActivityType(),
              activity.getCreatedAt()
          )).orElse(null);
        })
        .filter(Objects::nonNull) // null 값 제거
        .toList(); // 결과 리스트로 반환
  }


  public void addActivity(Long groupId, Long userId, Long photoId, String activityTypeName)
      throws BadRequestException {
    // GroupMember, PhotoActivityType 엔티티 조회
    Optional<GroupMember> groupMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
    Optional<PhotoActivityType> activityType = photoActivityTypeRepository.findByName(activityTypeName);
    Optional<Photo> photo = photoRepository.findById(photoId);

    if (activityType.isEmpty()) {
      throw new BadRequestException("PhotoActivityType not found");
    }
    // 엔티티 존재 여부 확인
    if (groupMember.isEmpty() || photo.isEmpty()) {
      throw new NoSuchElementException("GroupMember or Photo not found");
    }

    // GroupPhotoActivity 엔티티 생성 및 저장
    GroupPhotoActivity activity = new GroupPhotoActivity(groupMember.get(), photo.get(), activityType.get());
    groupPhotoActivityRepository.save(activity);
  }
}