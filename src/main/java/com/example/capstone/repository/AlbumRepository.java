package com.example.capstone.repository;

import com.example.capstone.entity.Album;
import com.example.capstone.entity.TravelGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {

  Optional<Album> findByGroupIdAndTitle(final Long group_id, final String title);

  List<Album> findAllByGroupId(final Long groupId);

  Optional<Album> findByGroupAndTitle(TravelGroup group, String title);
}
