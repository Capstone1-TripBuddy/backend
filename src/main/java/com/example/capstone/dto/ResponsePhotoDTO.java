package com.example.capstone.dto;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ResponsePhotoDTO {

  private Long photoId;
  private String fileUrl;
  private LocalDateTime uploadDate;
  // private Integer totalPages;

}
