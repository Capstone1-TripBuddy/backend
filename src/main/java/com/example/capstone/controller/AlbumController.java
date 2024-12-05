package com.example.capstone.controller;

import com.example.capstone.dto.RequestPhotoDTO;
import com.example.capstone.dto.ResponseAlbumDTO;
import com.example.capstone.dto.ResponsePhotoDTO;
import com.example.capstone.entity.Album;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.service.AlbumService;
import com.example.capstone.service.FileService;

import com.example.capstone.service.GroupMemberService;
import com.example.capstone.service.GroupPhotoActivityService;
import com.example.capstone.service.PhotoAnalysisService;
import com.example.capstone.service.PhotoQuestionService;
import com.example.capstone.service.TravelGroupService;
import com.example.capstone.service.UserService;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import java.util.stream.Collectors;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

  MediaType mediaType = new MediaType("application", "json", StandardCharsets.UTF_8);

  private final FileService fileService;
  private final AlbumService albumService;
  private final UserService userService;
  private final TravelGroupService travelGroupService;
  private final PhotoAnalysisService photoAnalysisService;
  private final GroupMemberService groupMemberService;
  private final PhotoQuestionService photoQuestionService;

  public AlbumController(FileService fileService, AlbumService albumService,
      final UserService userService, final TravelGroupService travelGroupService,
      final PhotoAnalysisService photoAnalysisService, final GroupMemberService groupMemberService,
      final PhotoQuestionService photoQuestionService) {
    this.fileService = fileService;
    this.albumService = albumService;
    this.userService = userService;
    this.travelGroupService = travelGroupService;
    this.photoAnalysisService = photoAnalysisService;
    this.groupMemberService = groupMemberService;
    this.photoQuestionService = photoQuestionService;
  }


  /* 파일 업로드
    - S3 업로드
    - photo 테이블에 저장
    - 비동기식 사진 분류
    - album 테이블에 앨범 추가 (없는 경우)
    - album_photo 테이블에 앨범과 포토 연결 관계 추가
    - upload 내역은 분류 완료시?
   */
  @PostMapping("/upload")
  public ResponseEntity<Void> uploadPhotos(@ModelAttribute RequestPhotoDTO request)
      throws IOException {
    Optional<User> user = userService.findUserById(request.getUserId());
    Optional<TravelGroup> travelGroup = travelGroupService.findGroupById(request.getGroupId());
    if (user.isEmpty() || travelGroup.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    List<Long> result = fileService.storeFiles(user.get(), travelGroup.get(), request.getPhotos(), request.getTakenAt());
    if (result.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // 비동기식 이미지 분류 프로세스 호출
    photoAnalysisService.uploadPhotosAndProcess(
        request.getGroupId(),
        request.getUserId(),
        groupMemberService.hasNewMemberByAlbum(request.getGroupId())
    );

    // 비동기식 사진 AI 질문 생성 메소드 호출
    photoQuestionService.generateQuestions(result);

    return new ResponseEntity<>(HttpStatus.OK);
  }

  // 파일 삭제
  @DeleteMapping("/delete/{filePath}")
  public ResponseEntity<Void> deletePhoto(@PathVariable String filePath) {
    if (!fileService.doesFileExist(filePath)) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    fileService.deleteFile(filePath);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("/{groupId}/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getAllGroupPhotos(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findAllGroupAlbumPhotos(groupId, page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
  }

  @GetMapping("/{groupId}/{userId}/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getMemberPhotos(
      @PathVariable Long groupId, @PathVariable Long userId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findGroupMemberAlbumPhotos(groupId, userId, page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
  }

  @GetMapping("/{groupId}/nature/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getNaturePhotos(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findGroupAlbumPhotosByTitle(groupId, "자연", page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
  }

  @GetMapping("/{groupId}/city/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getCityPhotos(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findGroupAlbumPhotosByTitle(groupId, "도시", page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
  }

  @GetMapping("/{groupId}/food/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getFoodPhotos(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findGroupAlbumPhotosByTitle(groupId, "음식", page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
  }

  @GetMapping("/{groupId}/animal/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getAnimalPhotos(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findGroupAlbumPhotosByTitle(groupId, "동물", page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
  }

  @GetMapping("/{groupId}/{albumName}/download")
  public ResponseEntity<ByteArrayResource> downloadAllAlbumPhotos(@PathVariable Long groupId, @PathVariable String albumName)
      throws IOException {
    Optional<ResponseEntity<ByteArrayResource>> response = albumService.zipAlbum(groupId, albumName);
    return response.orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @ExceptionHandler({NoSuchElementException.class, IllegalArgumentException.class, IndexOutOfBoundsException.class})
  ResponseEntity<String> handleBadSearchRequest(Exception e) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.badRequest().headers(headers).body(e.getMessage());
  }

  @ExceptionHandler({IOException.class, HttpServerErrorException.class,
      DataIntegrityViolationException.class})
  ResponseEntity<String> handleInputOutputFailureRequest(Exception e) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.internalServerError().headers(headers).body(e.getMessage());
  }
}

