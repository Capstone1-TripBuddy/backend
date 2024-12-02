package com.example.capstone.service;

import com.example.capstone.dto.RequestBookmarkDTO;
import com.example.capstone.entity.GroupMember;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.PhotoBookmark;
import com.example.capstone.repository.GroupMemberRepository;
import com.example.capstone.repository.PhotoBookmarkRepository;
import com.example.capstone.repository.PhotoRepository;
import java.util.NoSuchElementException;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhotoBookmarkService {

  private final PhotoBookmarkRepository photoBookmarkRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final PhotoRepository photoRepository;

  public Long addBookmark(RequestBookmarkDTO request) {
    Optional<GroupMember> groupMember = groupMemberRepository.findByGroupIdAndUserId(
        request.getUserId(), request.getGroupId());
    Optional<Photo> groupPhoto = photoRepository.findById(request.getPhotoId());
    if (groupMember.isEmpty() || groupPhoto.isEmpty()) {
      throw new NoSuchElementException();
    }
    PhotoBookmark bookmark = new PhotoBookmark(groupMember.get(), groupPhoto.get());

    PhotoBookmark result = photoBookmarkRepository.save(bookmark);
    return result.getId();
  }

  public void deleteBookmark(Long bookmarkId) {
    photoBookmarkRepository.deleteById(bookmarkId);
  }

  public List<PhotoBookmark> getBookmarksByUserId(Long userId) {
    return photoBookmarkRepository.findByGroupMemberUserId(userId);
  }

  public List<PhotoBookmark> getBookmarksByPhotoId(Long photoId) {
    return photoBookmarkRepository.findByPhotoId(photoId);
  }
}