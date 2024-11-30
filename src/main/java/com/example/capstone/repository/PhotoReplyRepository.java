package com.example.capstone.repository;

import com.example.capstone.entity.PhotoReply;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface PhotoReplyRepository extends CrudRepository<PhotoReply, Long> {

  List<PhotoReply> findByPhotoId(Long photoId);
}
