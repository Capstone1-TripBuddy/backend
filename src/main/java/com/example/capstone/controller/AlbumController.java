package com.example.capstone.controller;

import com.example.capstone.dto.RequestPhotoDTO;
import com.example.capstone.dto.ResponseAlbumDTO;
import com.example.capstone.dto.ResponsePhotoDTO;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.service.AlbumService;
import com.example.capstone.service.FileService;

import com.example.capstone.service.TravelGroupService;
import com.example.capstone.service.UserService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

  private final FileService fileService;
  private final AlbumService albumService;
  private final UserService userService;
  private final TravelGroupService travelGroupService;

  public AlbumController(FileService fileService, AlbumService albumService,
      final UserService userService, final TravelGroupService travelGroupService) {
    this.fileService = fileService;
    this.albumService = albumService;
    this.userService = userService;
    this.travelGroupService = travelGroupService;
  }
  // 파일 업로드
  @PostMapping("/upload")
  public ResponseEntity<Void> uploadPhotos(@ModelAttribute RequestPhotoDTO request)
      throws IOException {
    Optional<User> user = userService.findUserById(request.getUserId());
    Optional<TravelGroup> travelGroup = travelGroupService.findGroupById(request.getGroupId());
    if (user.isEmpty() || travelGroup.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    fileService.storeFiles(user.get(), travelGroup.get(), request.getPhotos());
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

  @GetMapping("/{groupId}")
  public ResponseEntity<List<ResponsePhotoDTO>> getAllAlbumData(@PathVariable Long groupId) {
    Optional<List<ResponsePhotoDTO>> albums = albumService.findAllGroupAlbumPhotos(groupId);
    return albums.map(responsePhotoDTOS -> new ResponseEntity<>(responsePhotoDTOS, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/{groupId}/{userId}") // To Do : API document update
  public ResponseEntity<List<ResponsePhotoDTO>> getMemberPhotos(@PathVariable Long groupId, @PathVariable Long userId) {
    Optional<List<ResponsePhotoDTO>> response = albumService.findGroupMemberAlbumPhotos(groupId, userId);
    return response.map(responsePhotoDTOS -> new ResponseEntity<>(responsePhotoDTOS, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/{groupId}/sight")
  public ResponseEntity<List<ResponsePhotoDTO>> getSightPhotos(@PathVariable Long groupId) {
    Optional<List<ResponsePhotoDTO>> response = albumService.findGroupAlbumPhotosByTitle(groupId, "sight");
    return response.map(responsePhotoDTOS -> new ResponseEntity<>(responsePhotoDTOS, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/{groupId}/food")
  public ResponseEntity<List<ResponsePhotoDTO>> getFoodPhotos(@PathVariable Long groupId) {
    Optional<List<ResponsePhotoDTO>> response = albumService.findGroupAlbumPhotosByTitle(groupId, "food");
    return response.map(responsePhotoDTOS -> new ResponseEntity<>(responsePhotoDTOS, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/{groupId}/animal")
  public ResponseEntity<List<ResponsePhotoDTO>> getAnimalPhotos(@PathVariable Long groupId) {
    Optional<List<ResponsePhotoDTO>> response = albumService.findGroupAlbumPhotosByTitle(groupId, "animal");
    return response.map(responsePhotoDTOS -> new ResponseEntity<>(responsePhotoDTOS, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/{groupId}/{albumName}/download")
  public ResponseEntity<ByteArrayResource> downloadAllAlbumPhotos(@PathVariable Long groupId, @PathVariable String albumName)
      throws IOException {
    Optional<ResponseEntity<ByteArrayResource>> response = albumService.zipAlbum(groupId, albumName);
    return response.orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @ExceptionHandler(IOException.class)
  ResponseEntity<String> handleBadSignupRequest(Exception e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }
}

