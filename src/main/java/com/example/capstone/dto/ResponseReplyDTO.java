package com.example.capstone.dto;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ResponseReplyDTO {

  private Long replyId;

  private Long userId;

  private String content;

  private LocalDateTime createdAt;
}
