package com.example.capstone.repository;

import com.example.capstone.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

  void deleteByFilePath(String filePath);
}
