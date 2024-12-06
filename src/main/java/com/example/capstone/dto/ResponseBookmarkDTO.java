package com.example.capstone.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ResponseBookmarkDTO {

  private Long bookmarkId;

  private Long photoId;

  private Long userId;

  private LocalDateTime createdAt;

}
