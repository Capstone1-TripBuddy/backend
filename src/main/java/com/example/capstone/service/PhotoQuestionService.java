package com.example.capstone.service;

import com.example.capstone.entity.PhotoQuestion;
import com.example.capstone.repository.PhotoQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhotoQuestionService {

  private final PhotoQuestionRepository photoQuestionRepository;

  public List<PhotoQuestion> getQuestionsByPhotoId(Long photoId) {
    return photoQuestionRepository.findByPhotoId(photoId);
  }
}