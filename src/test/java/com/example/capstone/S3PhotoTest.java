package com.example.capstone;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.capstone.controller.PhotoController;
import com.example.capstone.dto.RequestPhotoDTO;
import com.example.capstone.service.PhotoService;
import com.example.capstone.service.S3Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(PhotoController.class)
public class S3PhotoTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PhotoService photoService;

  @MockBean
  private S3Service s3Service;

  private RequestPhotoDTO requestPhotoDTO;
  private String testFilename;

  @BeforeEach
  void setUp() {
    requestPhotoDTO = new RequestPhotoDTO(1L, 1L, new ArrayList<>());
    testFilename = "1/1/sample.jpg";
  }

  @Test
  @DisplayName("Upload Photo - Success")
  void testUploadFilesWithIds_Success() throws Exception {
    List<String> filenames = List.of(testFilename);
    when(photoService.savePhoto(any(RequestPhotoDTO.class))).thenReturn(filenames);

    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/photos/upload")
            .param("userId", "1")
            .param("groupId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value(testFilename));
  }

  @Test
  @DisplayName("Upload Photo - Failure")
  void testUploadFilesWithIds_Failure() throws Exception {
    when(photoService.savePhoto(any(RequestPhotoDTO.class))).thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/photos/upload")
            .param("userId", "1")
            .param("groupId", "1"))
        .andExpect(status().isInternalServerError());
  }

  @Test
  @DisplayName("Delete Photo - Success")
  void testDeletePhoto_Success() throws Exception {
    when(s3Service.doesFileExist(testFilename)).thenReturn(true);

    mockMvc.perform(MockMvcRequestBuilders.delete("/api/photos/delete")
            .param("filename", testFilename))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Delete Photo - Failure")
  void testDeletePhoto_Failure() throws Exception {
    when(s3Service.doesFileExist(testFilename)).thenReturn(false);

    mockMvc.perform(MockMvcRequestBuilders.delete("/api/photos/delete")
            .param("filename", testFilename))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Download Photo - Success")
  void testDownloadPhoto_Success() throws Exception {
    byte[] data = "file data".getBytes();
    when(photoService.downloadByFilename(testFilename)).thenReturn(Optional.of(data));

    mockMvc.perform(MockMvcRequestBuilders.get("/api/photos/download")
            .param("filename", testFilename))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-type", "application/octet-stream"))
        .andExpect(header().string("Content-disposition", "attachment; filename=\"" + testFilename + "\""));
  }

  @Test
  @DisplayName("Download Photo - Failure")
  void testDownloadPhoto_Failure() throws Exception {
    when(photoService.downloadByFilename(testFilename)).thenReturn(Optional.empty());

    mockMvc.perform(MockMvcRequestBuilders.get("/api/photos/download")
            .param("filename", testFilename))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Generate Presigned URL")
  void testGetPresignedUrl() throws Exception {
    String presignedUrl = "https://example.com/presigned-url";
    when(s3Service.generatePresignedUrl(testFilename, 60)).thenReturn(presignedUrl);

    mockMvc.perform(MockMvcRequestBuilders.get("/api/photos/presigned-url")
            .param("filename", testFilename)
            .param("expiresInMinutes", "60"))
        .andExpect(status().isOk())
        .andExpect(content().string(presignedUrl));
  }
}

