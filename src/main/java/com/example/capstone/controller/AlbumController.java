package com.example.capstone.controller;

import com.example.capstone.dto.ResponseAlbumDTO;
import com.example.capstone.service.AlbumService;
import com.example.capstone.service.PhotoService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

  private final PhotoService photoService;
  private final AlbumService albumService;

  public AlbumController(PhotoService photoService, AlbumService albumService) {
    this.photoService = photoService;
    this.albumService = albumService;
  }

  @GetMapping("/{groupId}")
  public ResponseEntity<List<ResponseAlbumDTO>> getAllAlbums(@PathVariable("groupId") long groupId) {
    List<ResponseAlbumDTO> albums = albumService.findAllAlbums(groupId);
    return new ResponseEntity<>(albums, HttpStatus.OK);
  }

  // 여러 이미지를 한번에 보내는 API
  @PostMapping("/download")
  public ResponseEntity<ByteArrayResource> getAlbumPhotosAsZip() {
    Optional<List<Entry<String, ByteArrayResource>>> photoResources = albumService.findAllPhotos();
    if (photoResources.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos)) {

      for (Map.Entry<String, ByteArrayResource> entry : photoResources.get()) {
        String filename = entry.getKey();
        ByteArrayResource resource = entry.getValue();

        ZipEntry zipEntry = new ZipEntry(filename);
        zipEntry.setSize(resource.contentLength());
        zos.putNextEntry(zipEntry);
        zos.write(resource.getByteArray());
        zos.closeEntry();
      }
      zos.finish();

      ByteArrayResource zipResource = new ByteArrayResource(baos.toByteArray());

      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"album_photos.zip\"")
          .body(zipResource);

    } catch (IOException e) {
      throw new IllegalStateException("Failed to create zip file", e);
    }
  }


  // 특정 앫범의 정보를 가져오도록 수정해야 됨
  /*
  @PostMapping("/download/test")
  public ResponseEntity<List<ByteArrayResource>> testGetAlbumPhotos(@RequestBody RequestAlbumDTO request) {
    List<ByteArrayResource> byteArrayResources = new ArrayList<>();

    List<Optional<byte[]>> photoResources = albumService.testFindAllPhotos(request);
    for (Optional<byte[]> photoResource : photoResources) {
      if (photoResource.isEmpty()) {
        return ResponseEntity.noContent().build();
      }
      byteArrayResources.add(new ByteArrayResource(photoResource.get()));
    }

    return ResponseEntity.ok()
        .contentType(MediaType.MULTIPART_FORM_DATA) // 여러 파일을 보내기 위한 설정
        .body(byteArrayResources);
  }
   */
}

