package com.example.capstone.service;

import com.example.capstone.dto.PhotoFaceDto;
import com.example.capstone.entity.*;
import com.example.capstone.repository.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Async
@Service
@RequiredArgsConstructor
public class PhotoAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(PhotoAnalysisService.class);

  private final GroupPhotoActivityService groupPhotoActivityService;

  private final TravelGroupRepository travelGroupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final PhotoRepository photoRepository;
  private final AlbumRepository albumRepository;
  private final AlbumPhotoRepository albumPhotoRepository;
  private final String pythonServerUrl = "http://127.0.0.1:8000/";
  private final String dataServerRootUrl = "https://photo-bucket-012.s3.ap-northeast-2.amazonaws.com/";

  private static ConcurrentHashMap<Long, Object> lockMap = new ConcurrentHashMap<>();

  public static HashMap<String, String> map;
  static {
    map = new HashMap<>();
    map.put("NATURE", "자연");
    map.put("CITY", "도시");
    map.put("FOOD", "음식");
    map.put("ANIMAL", "동물");
  }

  // groupId에 해당하는 Lock 객체 가져오기
  private Object getLock(Long groupId) {
    return lockMap.computeIfAbsent(groupId, id -> new Object());
  }

  private void removeLockIfUnused(Long groupId, Object lock) {
    lockMap.computeIfPresent(groupId, (id, existingLock) -> {
      if (existingLock == lock && Thread.holdsLock(lock)) {
        return null; // 사용되지 않는 경우 제거
      }
      return existingLock;
    });
  }

  /**
   * 프로필 사진이 이용하기 적절한지 판단한다.
   * 프로필 사진 속에는 얼굴이 하나만 있어야 한다.
   * @param file 사진 파일
   * @return 사진 속 얼굴 수
   */
  public CompletableFuture<Integer> isValidProfileImage(MultipartFile file) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // RestTemplate 인스턴스 생성
        RestTemplate restTemplate = new RestTemplate();

        // MultiValueMap 생성
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new ByteArrayResource(file.getBytes()) {
          @Override
          public String getFilename() {
            return file.getOriginalFilename(); // 파일 이름 설정
          }
        });

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // HttpEntity 생성
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // POST 요청 보내고 PhotoFaceDto[]로 응답 받기
        ResponseEntity<PhotoFaceDto[]> response = restTemplate.exchange(
            pythonServerUrl + "test/faces",
            HttpMethod.POST,
            requestEntity,
            PhotoFaceDto[].class
        );

        // 응답 상태 확인
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
          throw new RuntimeException("Response Error");
        }

        PhotoFaceDto[] faceData = response.getBody();
        return faceData.length;

      } catch (IOException e) {
        throw new RuntimeException("Failed to process image", e);
      }
    });
  }

  /**
   * 사진들의 카테고리를 판단하여 앨범에 추가한다.
   * @param groupId 분석할 사진들이 속한 여행 그룹 ID
   */
  @Async
  public void processImagesTypes(long groupId) {
    TravelGroup group = travelGroupRepository.findById(groupId).orElseThrow(
        () -> new NoSuchElementException("Travel group not found")
    );
    Object lock = getLock(groupId);

    synchronized (lock) {
      try {
        List<Photo> newPhotoList = photoRepository.findAllByGroupAndPhotoTypeIsNull(group);
        List<String> newPhotoPaths = new ArrayList<>();

        for (Photo photo : newPhotoList) {
          newPhotoPaths.add(dataServerRootUrl + photo.getFilePath());
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo_paths", newPhotoPaths);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String[]> response = restTemplate.exchange(
            pythonServerUrl + "process_photos/category",
            HttpMethod.POST,
            requestEntity,
            String[].class
        );

        String[] categories = response.getBody();

        for (int i = 0, l = categories == null ? 0 : categories.length; i < l; i++) {
          Photo photo = newPhotoList.get(i);
          photo.setPhotoType(categories[i]);
          photoRepository.save(photo);
          String categoriesString = categories[i];

          // 하나의 사진도 여러 개의 카테고리를 가질 수 있으며, 쉼표로 구분됨
          // PERSON, NATURE, CITY, FOOD, ANIMAL, OTHERS 에서 선택됨
          // PERSON은 processImagesFaces()에서 처리하면 되고
          // OTHERS는 기타이므로 무시하면 됨
          String[] ts = categoriesString.split(",");
          for (String t : ts) {
            if (Objects.equals(t, "NATURE") || Objects.equals(t, "CITY")
                || Objects.equals(t, "FOOD") || Objects.equals(t, "ANIMAL")) {
              String title = map.get(t);
              Optional<Album> albumOptional = albumRepository.findByGroupAndTitle(group, title);
              if (albumOptional.isEmpty()) {
                Album album = new Album(group, title, null);
                albumRepository.save(album);
                AlbumPhoto albumPhoto = new AlbumPhoto(album, photo);
                albumPhotoRepository.save(albumPhoto);
              }
              else {
                Album album = albumOptional.get();
                AlbumPhoto albumPhoto = new AlbumPhoto(album, photo);
                albumPhotoRepository.save(albumPhoto);
              }
            }
          }
        }
      } finally {
        // 필요에 따라 Lock 객체를 제거 (메모리 관리)
        removeLockIfUnused(groupId, lock);
      }
    }
  }

  /**
   * 사진들 속 얼굴들을 인식하여 앨범에 추가한다.
   * 그룹 구성원이 새로 추가되지 않은 상태에서 사진들만 새로 추가되어 그 사진들만 분석할 경우는 processAllPhotos를 false로 한다.
   * 새로운 그룹이거나 새로운 그룹원이 있을 경우 processAllPhotos를 true로 한다.
   * @param groupId 분석할 사진들이 속한 여행 그룹 ID
   * @param processAllPhotos 그룹 내 전체 사진들에 대해 조사할 것인지 여부
   */
  @Async
  public CompletableFuture<Long> processImagesFaces(Long groupId, boolean processAllPhotos) {
    TravelGroup group = travelGroupRepository.findById(groupId).orElseThrow(
        () -> new NoSuchElementException("Travel group not found")
    );
    Object lock = getLock(groupId);

    synchronized (lock) {
      return CompletableFuture.supplyAsync(() -> {
        Long lastPhotoId = null;
        try {
          // 현재 Group의 얼굴 데이터를 가져오기
          List<Photo> photoList;
          if (processAllPhotos) {
            photoList = photoRepository.findAllByGroup(group);
          }
          else{
            photoList = photoRepository.findAllByGroupAndAnalyzedAtIsNull(group);
          }
          List<String> photoPaths = new ArrayList<>();
          List<GroupMember> groupMemberList = groupMemberRepository.findAllByGroup(group);
          List<String> groupMemberProfilePics = new ArrayList<>();
          List<String> groupMemberNames = new ArrayList<>();

          for (Photo photo : photoList) {
            photoPaths.add(dataServerRootUrl + photo.getFilePath());
          }

          for (int i = 0; i < groupMemberList.size(); ++i) {
            groupMemberProfilePics.add(dataServerRootUrl + groupMemberList.get(i).getUser().getProfilePicture());
            // 실제 이름이 아니라 groupMemberNames에서 몇 번째인지
            groupMemberNames.add(String.valueOf(i));
          }

          // 새로 인식된 얼굴 데이터 처리
          RestTemplate restTemplate = new RestTemplate();
          HttpHeaders headers = new HttpHeaders();

          MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
          body.add("profile_image_paths", groupMemberProfilePics);
          body.add("profile_names", groupMemberNames);
          body.add("photo_paths", photoPaths);

          HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

          ResponseEntity<PhotoFaceDto[][]> response = restTemplate.exchange(
              pythonServerUrl + "process_photos/faces",
              HttpMethod.POST,
              requestEntity,
              PhotoFaceDto[][].class
          );

          // 데이터 업데이트
          PhotoFaceDto[][] photosData = response.getBody();

          for (int i = 0, lp = photosData == null ? 0 : photosData.length; i < lp; i++) {
            PhotoFaceDto[] facesData = photosData[i];
            Photo photo = photoList.get(i);
            photo.setHasFace(facesData != null && facesData.length > 0);
            photo.setAnalyzedAt(Instant.now());
            photoRepository.save(photo);
            for (int j = 0, lf = facesData == null ? 0 : facesData.length; j < lf; j++) {
              PhotoFaceDto faceData = facesData[j];
              if (faceData.getLabel() != null) {
                int idx = Integer.parseInt(faceData.getLabel());
                User user = groupMemberList.get(idx).getUser();

                String title = user.getName();
                Optional<Album> albumOptional = albumRepository.findByGroupAndTitle(group, title);
                if (albumOptional.isPresent()) {
                  // 기존에 분석된 적이 있는 이용자 얼굴임
                  Album album = albumOptional.get();
                  if (!albumPhotoRepository.existsByAlbumAndPhoto(album, photo)) {
                    // 새로 추가된 사진에서 기존 이용자 얼굴이 인식됨
                    albumPhotoRepository.save(new AlbumPhoto(album, photo));
                  }
                }
                else {
                  // 처음으로 얼굴 인식을 시도함 또는 사진에서 새롭게 추가된 이용자 얼굴이 인식됨
                  Album album = new Album(group, title, null);
                  albumRepository.save(album);
                  albumPhotoRepository.save(new AlbumPhoto(album, photo));
                }
              }
            }
          }

          // 마지막 사진 ID 저장
          lastPhotoId = photoList.get(photoList.size() - 1).getId();

        } catch (RestClientException e) {
          log.error("Failed to communicate with Python server", e);
          throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to communicate with Python server");
        } catch (NumberFormatException e) {
          log.error("Invalid face label format", e);
          throw new IllegalArgumentException("Invalid face label format", e);
        } catch (IndexOutOfBoundsException e) {
          log.error("Invalid index for group member list", e);
          throw new IndexOutOfBoundsException();
        } catch (DataAccessException e) {
          log.error("Database operation failed", e);
          throw new DataIntegrityViolationException("Database operation failed", e);
        } finally {
          // 필요에 따라 Lock 객체를 제거 (메모리 관리)
          removeLockIfUnused(groupId, lock);
        }
        return lastPhotoId; // 마지막 사진 ID 반환
      });
    }
  }

  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<String[]> getImageQuestions(MultipartFile file) throws IOException {
    // RestTemplate 인스턴스 생성
    RestTemplate restTemplate = new RestTemplate();

    // MultiValueMap 생성
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("image", new ByteArrayResource(file.getBytes()) {
      @Override
      public String getFilename() {
        return file.getOriginalFilename(); // 파일 이름 설정
      }
    });

    // 헤더 설정
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    // HttpEntity 생성
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    // POST 요청 보내고 String[]로 응답 받기
    ResponseEntity<String[]> response = restTemplate.exchange(
        pythonServerUrl + "process_photos/questions",
        HttpMethod.POST,
        requestEntity,
        String[].class
    );

    // 응답 상태 확인
    if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
      throw new RuntimeException("Response Error");
    }

    return CompletableFuture.completedFuture(response.getBody());
  }

  public void uploadPhotosAndProcess(long groupId, long userId, boolean processAllPhotos) {
    CompletableFuture<Long> processImagesFacesFuture = processImagesFaces(groupId, processAllPhotos);

    // 비동기식 사진 카테고리 분류 메소드 호출
    processImagesTypes(groupId);

    // processImagesFacesFuture 완료 후 업로드 내역 남기기
    processImagesFacesFuture.thenAcceptAsync(lastPhotoId -> {
      try {
        groupPhotoActivityService.addActivity(groupId, userId, lastPhotoId, "upload");
      } catch (BadRequestException e) {
        throw new RuntimeException(e);
      }
    });
  }

}