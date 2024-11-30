package com.example.capstone.service;

import com.example.capstone.entity.GroupMember;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.repository.GroupMemberRepository;
import com.example.capstone.repository.TravelGroupRepository;
import com.example.capstone.repository.UserRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupMemberService {

  private final GroupMemberRepository groupMemberRepository;
  private final TravelGroupRepository travelGroupRepository;
  private final UserRepository userRepository;

  @Autowired
  public GroupMemberService(GroupMemberRepository groupMemberRepository,
      TravelGroupRepository travelGroupRepository,
      UserRepository userRepository) {
    this.groupMemberRepository = groupMemberRepository;
    this.travelGroupRepository = travelGroupRepository;
    this.userRepository = userRepository;
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
}