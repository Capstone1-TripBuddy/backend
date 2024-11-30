package com.example.capstone.repository;

import com.example.capstone.entity.PhotoActivityType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoActivityTypeRepository extends JpaRepository<PhotoActivityType, Long> {

  Optional<PhotoActivityType> findByName(String activityTypeName);
}
