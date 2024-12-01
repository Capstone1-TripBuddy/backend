package com.example.capstone.repository;

import com.example.capstone.entity.GroupMember;
import com.example.capstone.entity.TravelGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

  List<GroupMember> findByGroupId(Long groupId);

  List<GroupMember> findByUserId(Long userId);

  Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

  List<GroupMember> findAllByGroup(TravelGroup group);
}
