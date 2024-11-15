package com.example.capstone.service;

import com.example.capstone.dto.RequestPhotoDTO;
import com.example.capstone.entity.Photo;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.repository.PhotoRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoService {

  private final PhotoRepository photoRepository;
  private final S3Service s3Service;
  private final UserService userService;
  private final TravelGroupService travelGroupService;

  @Autowired
  public PhotoService(PhotoRepository photoRepository, S3Service s3Service, UserService userService, TravelGroupService travelGroupService) {
    this.photoRepository = photoRepository;
    this.s3Service = s3Service;
    this.userService = userService;
    this.travelGroupService = travelGroupService;
  }

  public void savePhoto(RequestPhotoDTO requestPhotoDTO) throws IOException {
    long userId = requestPhotoDTO.getUserId();
    long groupId = requestPhotoDTO.getGroupId();
    List<MultipartFile> photos = requestPhotoDTO.getPhotos();

    // Prepare photo entity
    Optional<User> user = userService.findUserById(userId);
    Optional<TravelGroup> group = travelGroupService.findGroupById(groupId);

    if (user.isEmpty()) {
      throw new IOException("PhotoService: User not found");
    }
    if (group.isEmpty()) {
      throw new IOException("PhotoService: Group not found");
    }

    for (MultipartFile photo : photos) {
      // Upload file to S3
      String filepath = s3Service.fetchOneFile(photo);

      // Save photo entity to database
      Photo uploadedPhoto = new Photo(
          user.get(),
          group.get(),
          photo.getOriginalFilename(),
          filepath,
          photo.getSize(),
          photo.getContentType()
      );
    }
  }

  public void deletePhoto(String filename) {
    s3Service.deleteOneFile(filename);
    photoRepository.deleteByFilename(filename);
  }
}
