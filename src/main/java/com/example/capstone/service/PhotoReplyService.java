package com.example.capstone.service;

import com.example.capstone.dto.RequestReplyDTO;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.PhotoReply;
import com.example.capstone.entity.User;
import com.example.capstone.repository.PhotoReplyRepository;
import com.example.capstone.repository.PhotoRepository;
import com.example.capstone.repository.UserRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhotoReplyService {

  private final PhotoReplyRepository photoReplyRepository;
  private final UserRepository userRepository;
  private final PhotoRepository photoRepository;

  public void addReply(RequestReplyDTO request) {
    Optional<User> user = userRepository.findById(request.getId());
    Optional<Photo> photo = photoRepository.findById(request.getPhotoId());
    if (user.isEmpty() || photo.isEmpty()) {
      throw new NoSuchElementException();
    }

    PhotoReply reply = new PhotoReply(
        user.get(),
        photo.get(),
        request.getContent()
    );
    photoReplyRepository.save(reply);
  }

  public void deleteReply(Long replyId) {
    photoReplyRepository.deleteById(replyId);
  }

  public List<PhotoReply> getRepliesByPhotoId(Long photoId) {
    return photoReplyRepository.findByPhotoId(photoId);
  }
}