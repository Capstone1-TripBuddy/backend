package com.example.capstone.service;

import com.example.capstone.dto.ResponseAlbumDTO;
import com.example.capstone.dto.ResponsePhotoDTO;
import com.example.capstone.entity.Album;
import com.example.capstone.entity.AlbumPhoto;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.repository.AlbumPhotoRepository;
import com.example.capstone.repository.AlbumRepository;
import com.example.capstone.repository.PhotoRepository;
import com.example.capstone.repository.TravelGroupRepository;
import com.example.capstone.repository.UserRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlbumService {

  private final TravelGroupRepository travelGroupRepository;
  private final  AlbumRepository albumRepository;
  private final AlbumPhotoRepository albumPhotoRepository;
  private final PhotoRepository photoRepository;
  private final FileService fileService;
  private final UserRepository userRepository;

  public AlbumService(
      final TravelGroupRepository travelGroupRepository,
      final AlbumRepository albumRepository,
      final AlbumPhotoRepository albumPhotoRepository,
      final PhotoRepository photoRepository,
      final FileService fileService,
      final UserRepository userRepository) {
    this.travelGroupRepository = travelGroupRepository;
    this.albumRepository = albumRepository;
    this.albumPhotoRepository = albumPhotoRepository;
    this.photoRepository = photoRepository;
    this.fileService = fileService;
    this.userRepository = userRepository;
  }


  // 그룹의 전체 앨범의 정보 조회
  @Transactional(readOnly = true)
  public List<ResponseAlbumDTO> findAllAlbums(Long groupId) {
    // Fetch the TravelGroup entity based on the provided groupId
    TravelGroup group = travelGroupRepository.findById(groupId)
        .orElse(null);
    if (group == null) {
      return Collections.emptyList();
    }

    List<Album> albums = albumRepository.findAllByGroupId(groupId);

    // Map the albums to DTOs and return
    return albums.stream()
        .map(ResponseAlbumDTO::fromEntity)
        .collect(Collectors.toList());
  }

  // 그룹의 전체 사진(url) 조회
  public Optional<List<ResponsePhotoDTO>> findAllGroupAlbumPhotos(Long groupId) {
    List<Album> albums = albumRepository.findAllByGroupId(groupId);
    if (albums.isEmpty()) {
      return Optional.empty();
    }

    List<ResponsePhotoDTO> response = new ArrayList<>();
    for (Album album : albums) {
      List<AlbumPhoto> albumPhotos = albumPhotoRepository.findByAlbumId(album.getId());
      if (!albumPhotos.isEmpty()) {
        albumPhotos.stream().map(AlbumPhoto::getPhoto)
            .forEach((photo -> {
              ResponsePhotoDTO foundPhoto = new ResponsePhotoDTO(
                  photo.getFileName(),
                  fileService.generateSignedUrl(photo.getFilePath()),
                  photo.getImageSize(),
                  photo.getUploadDate());
              response.add(foundPhoto);
            }));
      }
    }
    return Optional.of(response);
  }

  // 그룹의 특정 앨범의 전체 사진(url) 조회
  public Optional<List<ResponsePhotoDTO>> findGroupAlbumPhotosByTitle(Long groupId, String albumTitle) {
    Optional<List<AlbumPhoto>> albumPhotos = getAllGroupAlbumPhotosByTitle(groupId, albumTitle);
    if (albumPhotos.isEmpty()) {
      return Optional.empty();
    }

    List<ResponsePhotoDTO> result = new ArrayList<>();
    for (AlbumPhoto albumPhoto : albumPhotos.get()) {
      Photo photo = albumPhoto.getPhoto();

      ResponsePhotoDTO tmp = new ResponsePhotoDTO(
          photo.getFileName(),
          fileService.generateSignedUrl(photo.getFilePath()),
          photo.getImageSize(),
          photo.getUploadDate()
      );
      result.add(tmp);
    }

    return Optional.of(result);
  }

  // 그룹의 특정 멤버의 전체 사진(url) 조회
  public Optional<List<ResponsePhotoDTO>> findGroupMemberAlbumPhotos(Long groupId, Long userId) {
    Optional<User> member = userRepository.findById(userId);
    if (member.isEmpty()) {
      return Optional.empty();
    }

    String memberName = member.get().getName();
    return findGroupAlbumPhotosByTitle(groupId, memberName);
  }

  // 그룹의 특정 앨범의 전체 사진 zip 다운로드
  public Optional<ResponseEntity<ByteArrayResource>> zipAlbum(Long groupId, String albumTitle) throws IOException {
    Optional<List<AlbumPhoto>> albumPhotos = getAllGroupAlbumPhotosByTitle(groupId, albumTitle);
    if (albumPhotos.isEmpty()) {
      return Optional.empty();
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zipOut = new ZipOutputStream(baos);

    for (AlbumPhoto albumPhoto : albumPhotos.get()) {
      Photo photo = albumPhoto.getPhoto();
      Optional<ByteArrayResource> resource = fileService.loadFile(photo.getFilePath());
      if (resource.isPresent()) {
        ZipEntry zipEntry = new ZipEntry(photo.getFileName()); // 파일 이름 설정 (필요에 따라 수정)
        zipOut.putNextEntry(zipEntry);
        zipOut.write(resource.get().getByteArray());
        zipOut.closeEntry();
      }
    }
    zipOut.close();

    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Disposition", "attachment; filename=images.zip");
    headers.add("Content-Type", "application/zip");

    return Optional.of(ResponseEntity.ok()
        .headers(headers)
        .body(new ByteArrayResource(baos.toByteArray())));
  }

  // 그룹의 전체 사진 엔티티 조회
  private Optional<List<AlbumPhoto>> getAllGroupAlbumPhotosByTitle(Long groupId, String albumTitle) {
    Optional<TravelGroup> travelGroup = travelGroupRepository.findById(groupId);
    if (travelGroup.isEmpty()) {
      return Optional.empty();
    }

    Album album = albumRepository.findByGroupIdAndTitle(groupId, albumTitle);
    List<AlbumPhoto> albumPhotos = albumPhotoRepository.findByAlbumId(album.getId());
    if (albumPhotos.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(albumPhotos);
  }

}
