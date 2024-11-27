package com.example.capstone.dto;

import com.example.capstone.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
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
public class RequestSignupUserDTO {

  @NotBlank
  String name;

  @NotBlank
  String email;

  @NotBlank
  String password;

  @Setter
  private MultipartFile profilePicture;


  // DTO를 Entity로 변환
  public static User toEntity(final RequestSignupUserDTO user) {
    return new User(
        user.getEmail(),
        user.getPassword(),
        user.getName());
  }

}
