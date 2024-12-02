package com.example.capstone.service;

import com.example.capstone.entity.Photo;
import com.example.capstone.entity.PhotoQuestion;
import com.example.capstone.repository.PhotoQuestionRepository;
import com.example.capstone.repository.PhotoRepository;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PhotoQuestionService {

  private final PhotoQuestionRepository photoQuestionRepository;
  private final PhotoRepository photoRepository;
  private final PhotoAnalysisService photoAnalysisService;
  private final FileService fileService;

  public List<PhotoQuestion> getQuestionsByPhotoId(Long photoId) {
    return photoQuestionRepository.findByPhotoId(photoId);
  }

  @Async
  public void generateQuestions(List<Long> photoIds) throws IOException {
    List<Photo> photos = photoRepository.findAllById(photoIds);

    for (Photo photo : photos) {
      // 질문 생성
      Optional<ByteArrayResource> file = fileService.loadFile(photo.getFilePath());
      if (file.isPresent()) {
        // MockMultipartFile 생성
        MultipartFile multipartFile = new MockMultipartFile(
            "file" + photo.getId(), // 파일 이름
            photo.getFileName(), // 원본 파일 이름
            photo.getImageFormat(), // 콘텐츠 유형
            file.get().getInputStream() // InputStream
        );
        CompletableFuture<String[]> questions = photoAnalysisService.getImageQuestions(multipartFile);

        // 질문 저장
        questions.thenApply(questionsArray -> Arrays.stream(questionsArray)
                .map(question -> new PhotoQuestion(photo, question))
                .collect(Collectors.toList()))
            .thenAccept(photoQuestionRepository::saveAll);
      }
    }
  }
}