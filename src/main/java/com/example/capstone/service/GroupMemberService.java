package com.example.capstone.service;

import com.example.capstone.entity.Album;
import com.example.capstone.entity.GroupMember;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.repository.AlbumRepository;
import com.example.capstone.repository.GroupMemberRepository;
import com.example.capstone.repository.TravelGroupRepository;
import com.example.capstone.repository.UserRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupMemberService {

  private final GroupMemberRepository groupMemberRepository;
  private final TravelGroupRepository travelGroupRepository;
  private final UserRepository userRepository;
  private final AlbumRepository albumRepository;

  @Autowired
  public GroupMemberService(GroupMemberRepository groupMemberRepository,
      TravelGroupRepository travelGroupRepository,
      UserRepository userRepository, final AlbumRepository albumRepository) {
    this.groupMemberRepository = groupMemberRepository;
    this.travelGroupRepository = travelGroupRepository;
    this.userRepository = userRepository;
    this.albumRepository = albumRepository;
  }

  public List<User> getAllGroupMembersByGroupId(Long groupId) {
    List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(groupId);
    return groupMembers.stream()
        .map(GroupMember::getUser)
        .map(User::getId)
        .map(userRepository::findById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  public List<TravelGroup> getAllGroupsByUserId(Long userId) {
    List<GroupMember> groupMembers = groupMemberRepository.findByUserId(userId);
    return groupMembers.stream()
        .map(GroupMember::getGroup)
        .map(TravelGroup::getId)
        .map(travelGroupRepository::findById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  public boolean isAllProfilePictureAnalyzed(Long groupId) throws NotFoundException {
    Optional<TravelGroup> travelGroup = travelGroupRepository.findById(groupId);
    List<User> users = getAllGroupMembersByGroupId(groupId);

    // 그룹에 속한 유저들이 없다면 NotFoundException 던지기
    if (users.isEmpty() || travelGroup.isEmpty()) {
      throw new NotFoundException();
    }

    // 유저 중 하나라도 프로필 사진이 분석되지 않았다면 false 반환
    for (User user : users) {
      String userName = user.getName();
      Optional<Album> userAlbum = albumRepository.findByGroupAndTitle(travelGroup.get(), userName);
      if (userAlbum.isEmpty()) {
        return false;  // 하나라도 분석되지 않았다면 false
      }
    }

    // 모든 유저의 프로필 사진이 분석된 경우
    return true;
  }


}