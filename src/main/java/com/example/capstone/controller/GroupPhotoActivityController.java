package com.example.capstone.controller;

import com.example.capstone.dto.RequestBookmarkDTO;
import com.example.capstone.dto.RequestReplyDTO;
import com.example.capstone.dto.ResponsePhotoActivity;
import com.example.capstone.entity.GroupPhotoActivity;
import com.example.capstone.entity.PhotoBookmark;
import com.example.capstone.entity.PhotoQuestion;
import com.example.capstone.entity.PhotoReply;
import com.example.capstone.service.AlbumService;
import com.example.capstone.service.GroupPhotoActivityService;
import com.example.capstone.service.PhotoBookmarkService;
import com.example.capstone.service.PhotoQuestionService;
import com.example.capstone.service.PhotoReplyService;
import com.example.capstone.service.TravelGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity")
public class GroupPhotoActivityController {

  private final PhotoBookmarkService photoBookmarkService;
  private final PhotoReplyService photoReplyService;
  private final GroupPhotoActivityService groupPhotoActivityService;
  private final PhotoQuestionService photoQuestionService;
  private final AlbumService albumService;
  private final TravelGroupService travelGroupService;

  // 사용자 사진 북마크 생성
  @PostMapping("/bookmark")
  public ResponseEntity<Void> addBookmark(@RequestBody @Valid RequestBookmarkDTO request) {
    photoBookmarkService.addBookmark(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 사용자 사진 북마크 삭제
  @DeleteMapping("/bookmark/{bookmarkId}")
  public ResponseEntity<Void> deleteBookmark(@PathVariable Long bookmarkId) {
    photoBookmarkService.deleteBookmark(bookmarkId);
    return ResponseEntity.noContent().build();
  }

  // 사용자 여행 그룹별 북마크 조회
  @GetMapping("/bookmark/user/{userId}")
  public ResponseEntity<List<PhotoBookmark>> getBookmarksByGroup(@PathVariable Long userId) {
    List<PhotoBookmark> bookmarks = photoBookmarkService.getBookmarksByUserId(userId);
    return ResponseEntity.ok(bookmarks);
  }

  // 사용자 사진 댓글 생성
  @PostMapping("/reply")
  public ResponseEntity<Void> addReply(@RequestBody RequestReplyDTO request) {
    photoReplyService.addReply(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 사용자 사진 댓글 삭제
  @DeleteMapping("/reply/{replyId}")
  public ResponseEntity<Void> deleteComment(@PathVariable Long replyId) {
    photoReplyService.deleteReply(replyId);
    return ResponseEntity.noContent().build();
  }


  /* 여행 그룹 사진별 활동 정보 조회
      - 여행 그룹 사진별 북마크 & 댓글 갯수 조회
      - 여행 그룹 사진 별 전체 댓글 조회
      - 여행 그룹 사진 별 전체 AI 질문 조회
  */
  @GetMapping("/photo/{photoId}")
  public ResponseEntity<ResponsePhotoActivity> getGroupPhotoActivity(@PathVariable Long photoId) {
    List<PhotoBookmark> bookmarks = photoBookmarkService.getBookmarksByPhotoId(photoId);
    List<PhotoReply> replies = photoReplyService.getRepliesByPhotoId(photoId);
    List<PhotoQuestion> questions = photoQuestionService.getQuestionsByPhotoId(photoId);

    ResponsePhotoActivity response = ResponsePhotoActivity.fromEntity(bookmarks, replies, questions);
    return ResponseEntity.ok(response);
  }

  /* 여행 그룹 최신 활동 내역 조회
      - 여행 그룹별 북마크 내역 조회
      - 여행 그룹별 댓글 내역 조회
      - 여행 그룹별 사진 업로드 내역 조회
      - 여행 그룹별 사진 공유 내역 조회
  */
  @GetMapping("/group/{groupId}")
  public ResponseEntity<List<GroupPhotoActivity>> getGroupRecentActivity(@PathVariable Long groupId) {
    List<GroupPhotoActivity> response = groupPhotoActivityService.getGroupRecentActivity(groupId);
    return ResponseEntity.ok(response);
  }
}
