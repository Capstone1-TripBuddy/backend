package com.example.capstone.dto;

import com.example.capstone.entity.PhotoBookmark;
import com.example.capstone.entity.PhotoQuestion;
import com.example.capstone.entity.PhotoReply;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class ResponsePhotoActivity {

  Long photoId;

  Integer totalBookmarks;

  Integer totalReplies;

  List<ResponseBookmarkDTO> photoBookmarks;

  List<ResponseReplyDTO> photoReplies;

  List<ResponseQuestionDTO> photoQuestions;

  public static ResponsePhotoActivity fromEntity(
      Long photoId,
      List<ResponseBookmarkDTO> photoBookmarks,
      List<ResponseReplyDTO> photoReplies,
      List<ResponseQuestionDTO> photoQuestions) {
    ResponsePhotoActivity responsePhotoActivity = new ResponsePhotoActivity();
    responsePhotoActivity.photoId = photoId;
    responsePhotoActivity.totalBookmarks = photoBookmarks.size();
    responsePhotoActivity.totalReplies = photoReplies.size();
    responsePhotoActivity.photoBookmarks = photoBookmarks;
    responsePhotoActivity.photoReplies = photoReplies;
    responsePhotoActivity.photoQuestions = photoQuestions;
    return responsePhotoActivity;
  }
}
