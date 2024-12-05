package com.example.capstone.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.repository.PhotoRepository;
import jakarta.validation.ValidationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileService {

  private final PhotoAnalysisService photoAnalysisService;
  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  private final String rootFolder = "photos/";
  //private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  //private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

  private final AmazonS3Client s3Client;
  private final PhotoRepository photoRepository;

  @Autowired
  public FileService(
      final PhotoRepository photoRepository, final AmazonS3Client s3Client,
      final PhotoAnalysisService photoAnalysisService) {
    this.photoRepository = photoRepository;
    this.s3Client = s3Client;
    this.photoAnalysisService = photoAnalysisService;
  }


  private String generateFilePath(MultipartFile file, Long groupId, Long userId) {
    return rootFolder
        + groupId + "/"
        + userId + "/"
        + UUID.randomUUID() + file.getOriginalFilename();
  }
  private String generateFilePath(MultipartFile file, String directory) {
    return rootFolder
        + directory + "/"
        + UUID.randomUUID() + file.getOriginalFilename();
  }

  public String storeProfilePicture(MultipartFile profilePicture)
      throws IOException, ExecutionException, InterruptedException {
    CompletableFuture<Integer> facesFuture = photoAnalysisService.isValidProfileImage(profilePicture);

    try {
      // 비동기 작업 결과를 1분까지 기다림 (Timeout 설정 가능)
      Integer faces = facesFuture.get(1, TimeUnit.MINUTES);

      if (faces == 1) {
        String filePath = generateFilePath(profilePicture, "profile");
        return storeSingleFile(profilePicture, filePath);
      }
      throw new ValidationException("유효한 프로필 사진이 아닙니다.");

    } catch (TimeoutException e) {
      throw new ValidationException("프로필 사진 분석에 시간 초과로 실패했습니다.");
    }
  }

  public String storeSingleFile(MultipartFile file, String filePath) throws IOException {
    // set metadata
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(file.getSize());
    metadata.setContentType(file.getContentType());

    // store to S3
    try (InputStream inputStream = file.getInputStream()) {
      s3Client.putObject(new PutObjectRequest(bucketName, filePath, inputStream, metadata));
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FileService: storeSingleFile: " + e);
    }

    return filePath;
  }

  public List<Long> storeFiles(User user, TravelGroup travelGroup, List<MultipartFile> files, List<String> filesTakenAt) throws IOException {
    if (files.size() != filesTakenAt.size()) {
      System.out.println(filesTakenAt);
      throw new BadRequestException(String.format("FileService: storeFiles: file size %d != takenAt size %d", files.size(), filesTakenAt.size()));
    }

    List<Long> response = new ArrayList<>();
    for (int i = 0; i < files.size(); i++) {
      // get when photo is takenLocalDateTime.parse(filesTakenAt.get(i), formatter)
      Timestamp takenAt = Timestamp.valueOf(LocalDateTime.parse(filesTakenAt.get(i), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      MultipartFile file = files.get(i);

      // generate file name
      String filePath = generateFilePath(file, travelGroup.getId(), user.getId());

      // store to S3
      storeSingleFile(file, filePath);

      // save to DB
      Photo photo = new Photo(
          user,
          travelGroup,
          file.getOriginalFilename(),
          filePath,
          takenAt.toLocalDateTime(),
          file.getSize(),
          file.getContentType());
      Photo savedPhoto = photoRepository.save(photo);
      response.add(savedPhoto.getId());
    }

    return response;
  }

/*
  public String generateSignedUrl(String filePath) {
    try {
      // Signed URL 요청 생성
      int duration = 30 * 60 * 1000; // millisecond
      GeneratePresignedUrlRequest generatePresignedUrlRequest =
          new GeneratePresignedUrlRequest(bucketName, filePath)
              .withMethod(HttpMethod.GET) // HTTP GET 요청용 Signed URL
              .withExpiration(new Date(System.currentTimeMillis() + duration));

      // Signed URL 생성
      URL signedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

      // debug print
      System.out.println("signedUrl length: " + signedUrl.toString().length());
      return signedUrl.toString();

    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to generate signed URL: " + e.getMessage(), e);
    }
  }
  */

  public Optional<ByteArrayResource> loadFile(String filePath) throws IOException {
    S3Object s3Object = s3Client.getObject(bucketName, filePath);
    InputStream inputStream = s3Object.getObjectContent();

    // InputStream을 ByteArrayResource로 변환
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    while ((len = inputStream.read(buffer)) > -1 ) {
      baos.write(buffer, 0, len);
    }
    baos.flush();
    ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());

    return Optional.of(resource);
  }

  /*
  public Optional<List<ByteArrayResource>> loadFiles(Long groupId, String albumTitle) throws IOException {
    Optional<TravelGroup> travelGroup = travelGroupService.findGroupById(groupId);

    if (travelGroup.isEmpty()) {
      throw new IOException("PhotoService: Travel Group not found");
    }

    try {
      // 1. 폴더의 객체 리스트 가져오기
      String folderPath = groupId + "/" + albumTitle;
      ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request()
          .withBucketName(bucketName)
          .withPrefix(folderPath)
          .withDelimiter("/");
      ListObjectsV2Result result = s3Client.listObjectsV2(listObjectsV2Request);

      List<ByteArrayResource> photoResources = new ArrayList<>();

      // 2. 파일 읽어서 ByteArrayResource로 변환
      for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
        S3Object s3Object = s3Client.getObject(bucketName, objectSummary.getKey());
        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        byte[] content = inputStream.readAllBytes(); // Java 9+ API

        // ByteArrayResource로 변환
        ByteArrayResource resource = new ByteArrayResource(content);
        photoResources.add(resource);
      }
      return Optional.of(photoResources);

    } catch (Exception e) {
      return Optional.empty();
    }
  }
 */

  public boolean doesFileExist(String filePath) {
    S3Object s3Object = s3Client.getObject(bucketName, filePath);
    return s3Object.getObjectMetadata().getContentLength() > 0;
  }

  public void deleteFile(String filePath) {
    s3Client.deleteObject(bucketName, filePath);
    photoRepository.deleteByFilePath(filePath);
  }

}
