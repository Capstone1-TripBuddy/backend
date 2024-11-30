package com.example.capstone.repository;

import com.example.capstone.entity.PhotoQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoQuestionRepository extends JpaRepository<PhotoQuestion, Long> {

  List<PhotoQuestion> findByPhotoId(Long photoId);
}
