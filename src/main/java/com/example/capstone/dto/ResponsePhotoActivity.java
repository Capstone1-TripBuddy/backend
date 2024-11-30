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

  Integer totalBookmarks;

  Integer totalReplies;

  List<PhotoReply> photoReplies;

  List<PhotoQuestion> photoQuestions;

  public static ResponsePhotoActivity fromEntity(
      List<PhotoBookmark> photoBookmarks,
      List<PhotoReply> photoReplies,
      List<PhotoQuestion> photoQuestions) {
    ResponsePhotoActivity responsePhotoActivity = new ResponsePhotoActivity();
    responsePhotoActivity.totalBookmarks = photoBookmarks.size();
    responsePhotoActivity.totalReplies = photoReplies.size();
    responsePhotoActivity.photoReplies = photoReplies;
    responsePhotoActivity.photoQuestions = photoQuestions;
    return responsePhotoActivity;
  }
}
