package com.example.capstone.repository;

import com.example.capstone.entity.Photo;
import com.example.capstone.entity.TravelGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

  void deleteByFilePath(String filePath);

  List<Photo> findAllByGroupAndPhotoTypeIsNull(TravelGroup group);

  List<Photo> findAllByGroup(TravelGroup group);

  List<Photo> findAllByGroupAndAnalyzedAtIsNull(TravelGroup group);

}