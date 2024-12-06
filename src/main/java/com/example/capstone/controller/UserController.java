package com.example.capstone.controller;

import com.example.capstone.dto.LoginUserDTO;
import com.example.capstone.dto.RequestSignupUserDTO;
import com.example.capstone.dto.RequestUpdateProfileDTO;
import com.example.capstone.dto.ResponseUserDTO;
import com.example.capstone.entity.User;
import com.example.capstone.repository.UserRepository;
import com.example.capstone.service.FileService;
import com.example.capstone.service.UserService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {

  MediaType mediaType = new MediaType("application", "json", StandardCharsets.UTF_8);

  @Autowired
  private UserService userService;

  @Autowired
  private FileService fileService;

  @Autowired
  private UserRepository userRepository;

  @GetMapping("/example")
  public ResponseEntity<String> getExample() {
    String response = "한글 테스트";
    return ResponseEntity.ok().body(response);
  }

  // 회원가입
  @PostMapping("/signup")
  public ResponseEntity<Void> signup(@ModelAttribute @Valid RequestSignupUserDTO request)
      throws IOException, ExecutionException, InterruptedException {
    String profilePath = fileService.storeProfilePicture(request.getProfilePicture());
    Optional<User> createdUser = userService.createUser(request, profilePath);
    if (createdUser.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    userRepository.save(createdUser.get());
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  // 로그인
  @PostMapping("/login")
  public ResponseEntity<ResponseUserDTO> login(@RequestBody @Valid LoginUserDTO user)
      throws NotFoundException, BadRequestException {
    ResponseUserDTO validatedUser = userService.validateUser(user);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(validatedUser);
  }

  @PostMapping("/profile")
  public ResponseEntity<Void> updateProfile(@ModelAttribute @Valid RequestUpdateProfileDTO request)
      throws NotFoundException, IOException, ExecutionException, InterruptedException {
    String profilePath = fileService.storeProfilePicture(request.getProfilePicture());
    Optional<User> updatedUser = userService.updateUserProfile(request, profilePath);
    if (updatedUser.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }


  @ExceptionHandler({MethodArgumentNotValidException.class, NotFoundException.class, BadRequestException.class})
  ResponseEntity<String> handleBadSignupRequest(Exception e) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.badRequest().headers(headers).body(e.getMessage());
  }
}
