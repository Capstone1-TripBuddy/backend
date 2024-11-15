package com.example.capstone.service;

import com.example.capstone.dto.ResponseAlbumDTO;
import com.example.capstone.entity.Album;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.repository.AlbumPhotoRepository;
import com.example.capstone.repository.AlbumRepository;
import com.example.capstone.repository.PhotoRepository;
import com.example.capstone.repository.TravelGroupRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
public class AlbumService {

  private final TravelGroupRepository travelGroupRepository;
  private final  AlbumRepository albumRepository;
  private final AlbumPhotoRepository albumPhotoRepository;
  private final PhotoRepository photoRepository;
  private final PhotoService photoService;
  private final S3Service s3Service;

  public AlbumService(
      final TravelGroupRepository travelGroupRepository,
      final AlbumRepository albumRepository,
      final AlbumPhotoRepository albumPhotoRepository,
      final PhotoRepository photoRepository,
      final PhotoService photoService,
      final S3Service s3Service
      ) {
    this.travelGroupRepository = travelGroupRepository;
    this.albumRepository = albumRepository;
    this.albumPhotoRepository = albumPhotoRepository;
    this.photoRepository = photoRepository;
    this.photoService = photoService;
    this.s3Service = s3Service;
  }


  public List<ResponseAlbumDTO> findAllAlbums(long groupId) {
    Optional<TravelGroup> group = travelGroupRepository.findById(groupId);
    if (group.isEmpty()) {
      return Collections.emptyList();
    }
    List<Album> albums = albumRepository.findAllByGroupId(group.get());

    return albums.stream()
        .map(ResponseAlbumDTO::fromEntity)
        .collect(Collectors.toList());
  }

  public Optional<List<Entry<String, ByteArrayResource>>> findAllPhotos() {
    List<Entry<String, ByteArrayResource>> photos = s3Service.getAllFilesAsByteArrayResource();
    if (photos.isEmpty()) {
      return Optional.empty(); // 사진이 없으면 빈 리스트 반환
    }

    return Optional.of(photos);
  }

  /*
  public List<Optional<byte[]>> testFindAllPhotos(RequestAlbumDTO request) {
    // request로 찾은 앨범이 null일 경우 처리
    Album album = albumRepository.findByTitle(request.getTitle());
    if (album == null) {
      return Collections.emptyList(); // album이 없으면 빈 리스트 반환
    }

    // album에 해당하는 앨범 사진 리스트가 null이거나 비어 있으면 처리
    List<AlbumPhoto> albumPhotos = albumPhotoRepository.findByAlbumId(album.getId());
    if (albumPhotos == null || albumPhotos.isEmpty()) {
      return Collections.emptyList(); // 사진이 없으면 빈 리스트 반환
    }

    return albumPhotos.stream()
        .map(AlbumPhoto::getPhoto)  // 각 AlbumPhoto에서 Photo 객체 추출
        .filter(Objects::nonNull)  // null인 Photo를 필터링
        .map(Photo::getId)  // Photo 객체에서 id 추출
        .map(photoRepository::findById)  // photoId로 Photo 조회
        .filter(Optional::isPresent)  // Optional이 존재할 경우만 필터링
        .map(Optional::get)  // Optional에서 Photo 객체 추출
        .map(Photo::getFilename)  // Photo 객체에서 filename 추출
        .map(photoService::downloadByFilename)  // filename을 이용해 사진 다운로드
        .filter(Objects::nonNull)  // null인 Photo를 필터링
        .collect(Collectors.toList());
  }

   */
}
