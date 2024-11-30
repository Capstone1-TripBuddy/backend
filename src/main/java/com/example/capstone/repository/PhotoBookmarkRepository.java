package com.example.capstone.repository;

import com.example.capstone.entity.PhotoBookmark;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoBookmarkRepository extends JpaRepository<PhotoBookmark, Long> {

  List<PhotoBookmark> findByGroupMemberUserId(Long userId);

  List<PhotoBookmark> findByGroupMemberGroupId(Long userId);

  List<PhotoBookmark> findByPhotoId(Long photoId);
}
