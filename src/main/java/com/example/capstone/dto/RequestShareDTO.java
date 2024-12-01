package com.example.capstone.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RequestShareDTO {

  @NotNull
  private Long groupId;

  @NotNull
  private Long userId;

  @NotNull
  private Long photoId;

}

