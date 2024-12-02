package com.example.capstone.controller;

import com.example.capstone.dto.RequestBookmarkDTO;
import com.example.capstone.dto.RequestReplyDTO;
import com.example.capstone.dto.RequestShareDTO;
import com.example.capstone.dto.ResponseBookmarkDTO;
import com.example.capstone.dto.ResponseGroupActivity;
import com.example.capstone.dto.ResponsePhotoActivity;
import com.example.capstone.dto.ResponseQuestionDTO;
import com.example.capstone.dto.ResponseReplyDTO;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.PhotoBookmark;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.repository.PhotoRepository;
import com.example.capstone.repository.TravelGroupRepository;
import com.example.capstone.service.GroupPhotoActivityService;
import com.example.capstone.service.PhotoBookmarkService;
import com.example.capstone.service.PhotoQuestionService;
import com.example.capstone.service.PhotoReplyService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
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
  private final PhotoRepository photoRepository;
  private final TravelGroupRepository travelGroupRepository;


  @PostMapping("/share")
  public ResponseEntity<Void> addBookmark(@RequestBody @Valid RequestShareDTO request)
      throws BadRequestException {
    groupPhotoActivityService.addActivity(request.getGroupId(), request.getUserId(), request.getPhotoId(),
        "share");
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 사용자 사진 북마크 생성
  @PostMapping("/bookmark")
  public ResponseEntity<Long> addBookmark(@RequestBody @Valid RequestBookmarkDTO request)
      throws BadRequestException {
    Long result = photoBookmarkService.addBookmark(request);
    groupPhotoActivityService.addActivity(request.getGroupId(), request.getUserId(), request.getPhotoId(),
        "bookmark");
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
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
  public ResponseEntity<Long> addReply(@RequestBody RequestReplyDTO request)
      throws BadRequestException {
    Long result = photoReplyService.addReply(request);
    groupPhotoActivityService.addActivity(request.getGroupId(), request.getUserId(), request.getPhotoId(),
        "reply");
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  // 사용자 사진 댓글 삭제
  @DeleteMapping("/reply/{replyId}")
  public ResponseEntity<Void> deleteComment(@PathVariable Long replyId) {
    photoReplyService.deleteReply(replyId);
    return ResponseEntity.noContent().build();
  }

  // 사진별 AI 질문 조회
  @GetMapping("/question/{photoId}")
  public ResponseEntity<List<ResponseQuestionDTO>> getQuestionsByPhotoId(@PathVariable Long photoId) {
    List<ResponseQuestionDTO> questions = photoQuestionService.getQuestionsByPhotoId(photoId).stream()
        .map((question) -> new ResponseQuestionDTO(question.getContent(), question.getCreatedAt())).toList();
    return ResponseEntity.ok(questions);
  }


  /** 여행 그룹 사진별 활동 정보 조회
      - 여행 그룹 사진별 북마크 & 댓글 갯수 조회
      - 여행 그룹 사진 별 전체 댓글 조회
      - 여행 그룹 사진 별 전체 AI 질문 조회
  */
  @GetMapping("/photo/{photoId}")
  public ResponseEntity<ResponsePhotoActivity> getGroupPhotoActivity(@PathVariable Long photoId) {
    Optional<Photo> photo = photoRepository.findById(photoId);
    if (photo.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    List<ResponseBookmarkDTO> bookmarks = photoBookmarkService.getBookmarksByPhotoId(photoId).stream()
        .map((bookmark) -> new ResponseBookmarkDTO(
            bookmark.getId(), bookmark.getGroupMember().getUser().getId(), bookmark.getCreatedAt())).toList();
    List<ResponseReplyDTO> replies = photoReplyService.getRepliesByPhotoId(photoId).stream()
        .map((reply) -> new ResponseReplyDTO(
            reply.getId(), reply.getUser().getId(),
            reply.getContent(), reply.getCreatedAt())).toList();
    List<ResponseQuestionDTO> questions = photoQuestionService.getQuestionsByPhotoId(photoId).stream()
        .map((question) -> new ResponseQuestionDTO(question.getContent(), question.getCreatedAt())).toList();

    ResponsePhotoActivity response = ResponsePhotoActivity.fromEntity(
        photoId, photo.get().getFilePath(), bookmarks, replies, questions);
    return ResponseEntity.ok(response);
  }

  /** 여행 그룹 전체 사진에 대한 사진별 활동 정보 조회
      - 여행 그룹 사진별 북마크 & 댓글 갯수 조회
      - 여행 그룹 사진 별 전체 댓글 조회
      - 여행 그룹 사진 별 전체 AI 질문 조회
  */
  @GetMapping("/photo/{groupId}")
  public ResponseEntity<List<ResponsePhotoActivity>> getAllGroupPhotoActivity(@PathVariable Long groupId) {
    Optional<TravelGroup> travelGroup = travelGroupRepository.findById(groupId);
    if (travelGroup.isEmpty()) {
      return ResponseEntity.noContent().build();
    }

    List<Photo> photos = photoRepository.findAllByGroup(travelGroup.get());
    List<ResponsePhotoActivity> response = new ArrayList<>();
    for (Photo photo : photos) {
      Long photoId = photo.getId();

      List<ResponseBookmarkDTO> bookmarks = photoBookmarkService.getBookmarksByPhotoId(photoId).stream()
          .map((bookmark) -> new ResponseBookmarkDTO(
              bookmark.getId(), bookmark.getGroupMember().getUser().getId(), bookmark.getCreatedAt())).toList();
      List<ResponseReplyDTO> replies = photoReplyService.getRepliesByPhotoId(photoId).stream()
          .map((reply) -> new ResponseReplyDTO(
              reply.getId(), reply.getUser().getId(),
              reply.getContent(), reply.getCreatedAt())).toList();
      List<ResponseQuestionDTO> questions = photoQuestionService.getQuestionsByPhotoId(photoId).stream()
          .map((question) -> new ResponseQuestionDTO(question.getContent(), question.getCreatedAt())).toList();

      ResponsePhotoActivity result = ResponsePhotoActivity.fromEntity(photoId, photo.getFilePath(), bookmarks, replies, questions);
      response.add(result);
    }

    return ResponseEntity.ok(response);
  }

  /** 여행 그룹 최신 활동 내역 조회
      - 여행 그룹별 북마크 내역 조회
      - 여행 그룹별 댓글 내역 조회
      - 여행 그룹별 사진 업로드 내역 조회
      - 여행 그룹별 사진 공유 내역 조회
      - 본인 활동 내역 제외
  */
  @GetMapping("/group/{groupId}/user/{userId}")
  public ResponseEntity<List<ResponseGroupActivity>> getGroupRecentActivity(
      @PathVariable Long groupId, @PathVariable Long userId) {
    List<ResponseGroupActivity> response = groupPhotoActivityService.getGroupRecentActivity(groupId, userId);
    return ResponseEntity.ok(response);
  }
}
