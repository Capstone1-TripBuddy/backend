package com.example.capstone.controller;

import com.example.capstone.dto.RequestPhotoDTO;
import com.example.capstone.dto.ResponseAlbumDTO;
import com.example.capstone.dto.ResponsePhotoDTO;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.service.AlbumService;
import com.example.capstone.service.FileService;

import com.example.capstone.service.GroupPhotoActivityService;
import com.example.capstone.service.TravelGroupService;
import com.example.capstone.service.UserService;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import java.util.stream.Collectors;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

  private final FileService fileService;
  private final AlbumService albumService;
  private final UserService userService;
  private final TravelGroupService travelGroupService;
  private final GroupPhotoActivityService groupPhotoActivityService;

  public AlbumController(FileService fileService, AlbumService albumService,
      final UserService userService, final TravelGroupService travelGroupService,
      final GroupPhotoActivityService groupPhotoActivityService) {
    this.fileService = fileService;
    this.albumService = albumService;
    this.userService = userService;
    this.travelGroupService = travelGroupService;
    this.groupPhotoActivityService = groupPhotoActivityService;
  }
  // 파일 업로드
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
    Long lastPhotoId = result.get(result.size() - 1);
    groupPhotoActivityService.addActivity(request.getGroupId(), request.getUserId(), lastPhotoId,
        "upload");
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

  /*
  @GetMapping("/{groupId}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getAllGroupAlbums(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findAllGroupAlbumPhotos(groupId, page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
  }
  */

  @GetMapping("/{groupId}/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getAllGroupPhotos(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findAllGroupAlbumPhotos(groupId, page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{groupId}/{userId}/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getMemberPhotos(
      @PathVariable Long groupId, @PathVariable Long userId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findGroupMemberAlbumPhotos(groupId, userId, page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{groupId}/sight/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getSightPhotos(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findGroupAlbumPhotosByTitle(groupId, "sight", page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{groupId}/food/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getFoodPhotos(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findGroupAlbumPhotosByTitle(groupId, "food", page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{groupId}/animal/{page}")
  public ResponseEntity<Page<ResponsePhotoDTO>> getAnimalPhotos(@PathVariable Long groupId, @PathVariable Integer page) {
    Page<ResponsePhotoDTO> response = albumService.findGroupAlbumPhotosByTitle(groupId, "animal", page);
    if (response.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{groupId}/{albumName}/download")
  public ResponseEntity<ByteArrayResource> downloadAllAlbumPhotos(@PathVariable Long groupId, @PathVariable String albumName)
      throws IOException {
    Optional<ResponseEntity<ByteArrayResource>> response = albumService.zipAlbum(groupId, albumName);
    return response.orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @ExceptionHandler(NoSuchElementException.class)
  ResponseEntity<String> handleBadSearchRequest(Exception e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }

  @ExceptionHandler(IOException.class)
  ResponseEntity<String> handleBadSignupRequest(Exception e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }
}

