package com.example.capstone.dto;

import com.example.capstone.entity.Photo;
import com.example.capstone.entity.PhotoActivityType;
import com.example.capstone.entity.User;
import java.time.LocalDateTime;

public class ResponseGroupActivity {

  private Long userId;

  private String userName;

  private Long photoId;

  private String activityType;

  private LocalDateTime createdAt;

  public static ResponseGroupActivity fromEntity(User user, Photo photo, PhotoActivityType activityType,
      LocalDateTime createdAt) {
    ResponseGroupActivity responseGroupActivity = new ResponseGroupActivity();
    responseGroupActivity.userId = user.getId();
    responseGroupActivity.userName = user.getName();
    responseGroupActivity.photoId = photo.getId();
    responseGroupActivity.activityType = activityType.getName();
    responseGroupActivity.createdAt = createdAt;
    return responseGroupActivity;
  }
}
