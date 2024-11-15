package com.example.capstone.controller;

import com.example.capstone.dto.RequestPhotoDTO;
import com.example.capstone.service.PhotoService;
import com.example.capstone.service.S3Service;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/photos")
public class PhotoController {

  private final S3Service s3Service;
  private final PhotoService photoService;

  public PhotoController(S3Service s3Service, PhotoService photoService) {
    this.s3Service = s3Service;
    this.photoService = photoService;
  }

  // 파일 업로드
  @PostMapping("/upload")
  public ResponseEntity<Void> uploadFilesWithIds(@ModelAttribute RequestPhotoDTO requestPhotoDTO)
      throws IOException {
    photoService.savePhoto(requestPhotoDTO);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  // 파일 삭제
  @DeleteMapping("/delete")
  public ResponseEntity<Void> deletePhoto(@RequestParam("filename") String filename) {
    if (!s3Service.doesFileExist(filename)) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    photoService.deletePhoto(filename);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  // 파일 다운로드
  /*
  @GetMapping("/download")
  public ResponseEntity<ByteArrayResource> downloadFile(@RequestParam("filename") String filename) {

    //  ex. image=https://board-example.s3.ap-northeast-2.amazonaws.com/2b8359b2-de59-4765-8da0-51f5d4e556c3.jpg

    Optional<byte[]> data = photoService.downloadByFilename(filename);
    if (data.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    ByteArrayResource resource = new ByteArrayResource(data.get());
    return ResponseEntity
        .ok()
        .contentLength(data.get().length)
        .header("Content-type", "application/octet-stream")
        .header("Content-disposition", "attachment; filename=\"" + filename + "\"")
        .body(resource);
  }

   */

  // 프리사인드 URL 발급
  @GetMapping("/presigned-url")
  public ResponseEntity<String> getPresignedUrl(@RequestParam("filename") String filename,
      @RequestParam("expiresInMinutes") int expiresInMinutes) {
    String presignedUrl = s3Service.generatePresignedUrl(filename, expiresInMinutes);
    return new ResponseEntity<>(presignedUrl, HttpStatus.OK);
  }

  @ExceptionHandler
  public ResponseEntity<String> handleException(Exception ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}


