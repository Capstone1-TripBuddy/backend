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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlbumService {

  private final Integer pageSize = 12;

  private final TravelGroupRepository travelGroupRepository;
  private final  AlbumRepository albumRepository;
  private final AlbumPhotoRepository albumPhotoRepository;
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

  // 그룹의 전체 사진(url) 조회 (페이징 및 최신 순 정렬)
  public Page<ResponsePhotoDTO> findAllGroupAlbumPhotos(Long groupId, Integer page) {
    // 1. 그룹에 속한 모든 앨범 조회
    List<Album> albums = albumRepository.findAllByGroupId(groupId);
    if (albums.isEmpty()) {
      throw new NoSuchElementException("No albums found for group ID: " + groupId);
    }

    // 2. 앨범 ID 리스트 추출
    List<Long> albumIds = albums.stream()
        .map(Album::getId)
        .collect(Collectors.toList());

    // 3. Pageable 객체 생성 (최신 순 정렬)
    Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "photo.uploadedAt"));

    // 4. 페이징 처리된 사진 데이터 조회
    Page<AlbumPhoto> pagedAlbumPhotos = albumPhotoRepository.findByAlbumIdIn(albumIds, pageable);
    if (pagedAlbumPhotos.isEmpty()) {
      throw new NoSuchElementException("No photos found for group ID: " + groupId);
    }

    // 5. AlbumPhoto → ResponsePhotoDTO 변환
    return pagedAlbumPhotos.map(albumPhoto -> {
      Photo photo = albumPhoto.getPhoto();
      return new ResponsePhotoDTO(
          photo.getId(),
          photo.getFileName(),
          photo.getFilePath(),
          photo.getImageSize(),
          photo.getUploadedAt());
    });
  }


  // 그룹의 특정 앨범의 전체 사진(url) 조회
  public Page<ResponsePhotoDTO> findGroupAlbumPhotosByTitle(Long groupId, String albumTitle, Integer page) {
    Optional<Album> album = getGroupAlbumByTitle(groupId, albumTitle);
    if (album.isEmpty()) {
      throw new NoSuchElementException("No album found for title: " + albumTitle);
    }

    // Pageable 객체 생성
    Pageable pageable = PageRequest.of(page, pageSize);

    // 페이징 처리된 AlbumPhoto 데이터 조회
    Page<AlbumPhoto> pagedAlbumPhotos = albumPhotoRepository.findByAlbumId(album.get().getId(), pageable);
    if (pagedAlbumPhotos.isEmpty()) {
      throw new NoSuchElementException("No photos found for this album: " + albumTitle);
    }

    // DTO로 변환
    return pagedAlbumPhotos.map(albumPhoto -> {
      Photo photo = albumPhoto.getPhoto();
      return new ResponsePhotoDTO(
          photo.getId(),
          photo.getFileName(),
          photo.getFilePath(),
          photo.getImageSize(),
          photo.getUploadedAt());
    });
  }

  // 그룹의 특정 멤버의 전체 사진(url) 조회
  public Page<ResponsePhotoDTO> findGroupMemberAlbumPhotos(Long groupId, Long userId, Integer page) {
    Optional<User> member = userRepository.findById(userId);
    if (member.isEmpty()) {
      throw new NoSuchElementException("No member found for user: " + userId);
    }

    String memberName = member.get().getName();
    return findGroupAlbumPhotosByTitle(groupId, memberName, page);
  }

  // 그룹의 특정 앨범의 전체 사진 zip 다운로드
  public Optional<ResponseEntity<ByteArrayResource>> zipAlbum(Long groupId, String albumTitle) throws IOException {
    Optional<Album> album = getGroupAlbumByTitle(groupId, albumTitle);
    if (album.isEmpty()) {
      return Optional.empty();
    }

    List<AlbumPhoto> albumPhotos = albumPhotoRepository.findByAlbumId(album.get().getId());
    if (albumPhotos.isEmpty()) {
      return Optional.empty();
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zipOut = new ZipOutputStream(baos);

    for (AlbumPhoto albumPhoto : albumPhotos) {
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

  // 그룹의 특정 앨범 엔티티 조회
  private Optional<Album> getGroupAlbumByTitle(Long groupId, String albumTitle) {
    Optional<TravelGroup> travelGroup = travelGroupRepository.findById(groupId);
    if (travelGroup.isEmpty()) {
      return Optional.empty();
    }

    Album album = albumRepository.findByGroupIdAndTitle(groupId, albumTitle);
    return Optional.of(album);
  }

}
