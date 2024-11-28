package com.example.capstone.repository;

import com.example.capstone.entity.GroupMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

  List<GroupMember> findByGroupId(Long groupId);

  List<GroupMember> findByUserId(Long userId);
}
