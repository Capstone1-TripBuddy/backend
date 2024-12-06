package com.example.capstone.service;

import com.example.capstone.dto.ResponseBookmarkDTO;
import com.example.capstone.dto.ResponseGroupActivity;
import com.example.capstone.dto.ResponsePhotoActivity;
import com.example.capstone.dto.ResponseQuestionDTO;
import com.example.capstone.dto.ResponseReplyDTO;
import com.example.capstone.entity.GroupMember;
import com.example.capstone.entity.GroupPhotoActivity;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.PhotoActivityType;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.repository.GroupMemberRepository;
import com.example.capstone.repository.GroupPhotoActivityRepository;
import com.example.capstone.repository.PhotoActivityTypeRepository;
import com.example.capstone.repository.PhotoRepository;
import com.example.capstone.repository.TravelGroupRepository;
import com.example.capstone.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupPhotoActivityService {

  private final GroupPhotoActivityRepository groupPhotoActivityRepository;
  private final PhotoActivityTypeRepository photoActivityTypeRepository;
  private final TravelGroupRepository travelGroupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final PhotoRepository photoRepository;
  private final UserRepository userRepository;

  private final PhotoBookmarkService photoBookmarkService;
  private final PhotoReplyService photoReplyService;
  private final PhotoQuestionService photoQuestionService;


  public List<ResponsePhotoActivity> getAllGroupPhotoActivity(Long groupId) {
    Optional<TravelGroup> travelGroup = travelGroupRepository.findById(groupId);
    if (travelGroup.isEmpty()) {
      throw new NoSuchElementException("getAllGroupPhotoActivity: Travel group not found");
    }

    List<Photo> photos = photoRepository.findAllByGroup(travelGroup.get()).stream()
        .sorted(Comparator.comparing(Photo::getUploadedAt).reversed()).toList(); // 최신 업로드 순서대로 정렬
    List<ResponsePhotoActivity> response = new ArrayList<>();
    for (Photo photo : photos) {
      Long photoId = photo.getId();

      List<ResponseBookmarkDTO> bookmarks = photoBookmarkService.getBookmarksByPhotoId(photoId).stream()
          .map((bookmark) -> new ResponseBookmarkDTO(
              bookmark.getId(),
              bookmark.getPhoto().getId(),
              bookmark.getGroupMember().getUser().getId(),
              bookmark.getCreatedAt())).toList();
      List<ResponseReplyDTO> replies = photoReplyService.getRepliesByPhotoId(photoId).stream()
          .map((reply) -> new ResponseReplyDTO(
              reply.getId(), reply.getUser().getId(),
              reply.getContent(), reply.getCreatedAt())).toList();
      List<ResponseQuestionDTO> questions = photoQuestionService.getQuestionsByPhotoId(photoId).stream()
          .map((question) -> new ResponseQuestionDTO(question.getContent(), question.getCreatedAt())).toList();

      ResponsePhotoActivity result = ResponsePhotoActivity.fromEntity(photoId, photo.getFilePath(), bookmarks, replies, questions);
      response.add(result);
    }

    return response;
  }

  public List<ResponseGroupActivity> getGroupRecentActivity(Long groupId, Long userId) {
    // 현재 시간 기준 1시간 전 시간 계산
    // Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

    // Instant를 LocalDateTime으로 변환 (ZoneId.systemDefault()을 사용하여 시스템 기본 시간대 적용)
    // LocalDateTime oneHourAgoLocalDateTime = LocalDateTime.ofInstant(oneHourAgo, ZoneId.systemDefault());


    // 그룹 ID에 해당하는 활동 리스트 조회
    List<GroupPhotoActivity> history = groupPhotoActivityRepository.findByGroupMemberGroupId(groupId);

    // photo activity type object: upload
    PhotoActivityType photoActivityType = photoActivityTypeRepository.findById(1L).orElseThrow(NoSuchElementException::new);
    List<GroupPhotoActivity> uploadHistory = history.stream()
        .filter(activity -> activity.getActivityType().equals(photoActivityType)).toList();

    // photo activity type object: reply, bookmark, share
    List<GroupPhotoActivity> etcHistory = history.stream()
        .filter(activity -> !activity.getActivityType().equals(photoActivityType))
        .filter(activity -> !activity.getGroupMember().getUser().getId().equals(userId)).toList(); // 본인 활동 내역 필터링

    return Stream.concat(uploadHistory.stream(), etcHistory.stream())
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
        .sorted(Comparator.comparing(ResponseGroupActivity::getCreatedAt).reversed()) // createdAt을 기준으로 최근순 정렬
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