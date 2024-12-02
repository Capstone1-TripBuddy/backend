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

  /**
   * 앨범 정보를 활용하여 새로운 멤버가 있는지 확인합니다.
   * @param groupId 그룹 ID
   * @return 새로운 멤버가 있는지 여부
   */
  public boolean hasNewMemberByAlbum(Long groupId) {
    // 앨범 제목 목록 가져오기
    List<String> albumTitles = albumRepository.findAllByGroupId(groupId)
        .stream()
        .map(Album::getTitle)
        .toList();

    // 그룹 멤버 이름 목록 가져오기
    List<String> memberNames = groupMemberRepository.findByGroupId(groupId)
        .stream()
        .map(groupMember -> groupMember.getUser().getName())
        .toList();

    // 앨범 제목 목록과 멤버 이름 목록 비교
    for (String memberName : memberNames) {
      if (!albumTitles.contains(memberName)) {
        // 멤버 이름과 일치하는 앨범 제목이 없으면 새로운 멤버
        return true;
      }
    }
    // 모든 멤버 이름과 일치하는 앨범 제목이 있으면 새로운 멤버 없음
    return false;
  }


}