package com.example.capstone.dto;

import com.example.capstone.entity.Photo;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class RequestPhotoDTO {

  @NotBlank
  private Long userId;

  @NotBlank
  private Long groupId;

  @NotEmpty
  private List<MultipartFile> photos;

  @NotEmpty
  private List<String> takenAt;

}
