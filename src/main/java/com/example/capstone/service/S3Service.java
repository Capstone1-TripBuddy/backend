package com.example.capstone.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class S3Service {

  private final AmazonS3Client s3Client;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  public S3Service(final AmazonS3Client s3Client) {
    this.s3Client = s3Client;
  }

  // 모든 파일을 ByteArrayResource와 파일 이름으로 반환하는 메서드
  public List<Map.Entry<String, ByteArrayResource>> getAllFilesAsByteArrayResource() {
    List<Map.Entry<String, ByteArrayResource>> photoResources = new ArrayList<>();

    // S3에서 모든 객체 목록 가져오기
    ObjectListing objectListing = s3Client.listObjects(bucketName);
    List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();

    for (S3ObjectSummary os : objectSummaries) {
      String key = os.getKey();
      ByteArrayResource resource = getOneFileAsByteArrayResource(key);
      photoResources.add(new AbstractMap.SimpleEntry<>(key, resource));
    }

    return photoResources;
  }

  // 개별 파일을 ByteArrayResource로 변환하여 반환하는 메서드
  private ByteArrayResource getOneFileAsByteArrayResource(String key) {
    S3Object s3Object = s3Client.getObject(bucketName, key);
    S3ObjectInputStream inputStream = s3Object.getObjectContent();

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
      return new ByteArrayResource(outputStream.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("S3Service: AWS S3 다운로드 오류", e);
    }
  }

  // 파일 업로드 메소드
  public String fetchOneFile(MultipartFile file) throws IOException {
    String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
    s3Client.putObject(bucketName, filename, file.getInputStream(), new ObjectMetadata());

    String filepath = s3Client.getUrl(bucketName, filename).toString();
    if (filepath == null) {
      throw new IOException("S3Service: uploadFile: File not found");
    }
    return filepath;
  }

  // 파일 다운로드 메소드
  public byte[] getOneFileAsByteArray(String image) {
    String filename = image.substring(image.lastIndexOf('/') + 1);

    S3Object s3Object = s3Client.getObject(bucketName, filename);
    S3ObjectInputStream inputStream = s3Object.getObjectContent();

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("aws s3 다운로드 error", e);
    }
  }

  // 파일 삭제 메소드
  public void deleteOneFile(String filename) {
    // filename: 폴더명까지 포함한 파일명
    s3Client.deleteObject(new DeleteObjectRequest(bucketName, filename));
  }

  public boolean doesFileExist(String filename) {
    return s3Client.doesObjectExist(bucketName, filename);
  }

  private static String getFileExtension(String originalFileName) {
    return originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
  }

  // 프리사인드 URL 생성 메소드
  public String generatePresignedUrl(String filename, int expirationInMinutes) {
    Date expiration = new Date();
    expiration.setTime(expiration.getTime() + (long) expirationInMinutes * 60 * 1000);

    GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, filename)
        .withMethod(HttpMethod.GET)
        .withExpiration(expiration);

    URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    return url.toString();
  }

  // 파일 조회 메소드
  public S3Object findOneFileByFilename(final String filename) {
    try {
      return s3Client.getObject(bucketName, filename);
    } catch (AmazonS3Exception e) {
      throw new AmazonS3Exception(e.getMessage(), e);
    }
  }
}
