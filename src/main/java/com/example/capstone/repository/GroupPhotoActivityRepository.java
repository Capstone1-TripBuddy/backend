package com.example.capstone.repository;

import com.example.capstone.entity.GroupPhotoActivity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupPhotoActivityRepository extends JpaRepository<GroupPhotoActivity, Long> {

  List<GroupPhotoActivity> findByGroupMemberGroupId(Long groupId);
}
