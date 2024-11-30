package com.example.capstone.service;

import com.example.capstone.entity.GroupMember;
import com.example.capstone.entity.GroupPhotoActivity;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.PhotoActivityType;
import com.example.capstone.repository.GroupMemberRepository;
import com.example.capstone.repository.GroupPhotoActivityRepository;
import com.example.capstone.repository.PhotoActivityTypeRepository;
import com.example.capstone.repository.PhotoRepository;
import java.util.NoSuchElementException;
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
  private final AlbumService albumService;
  private final PhotoRepository photoRepository;

  public List<GroupPhotoActivity> getGroupRecentActivity(Long groupId) {
    return groupPhotoActivityRepository.findByGroupMemberGroupId(groupId);
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