package com.example.capstone.service;

import com.example.capstone.dto.LoginUserDTO;
import com.example.capstone.dto.RequestSignupUserDTO;
import com.example.capstone.dto.RequestUpdateProfileDTO;
import com.example.capstone.dto.ResponseUserDTO;
import com.example.capstone.entity.User;
import com.example.capstone.repository.UserRepository;
import java.io.IOException;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  FileService fileService;

  // Create a new user
  public Optional<User> createUser(RequestSignupUserDTO user, String profilePath) throws IOException {
    User createdUser = RequestSignupUserDTO.toEntity(user, profilePath);
    userRepository.save(createdUser);

    return Optional.of(createdUser);
  }

  // Validate a user
  public ResponseUserDTO validateUser(LoginUserDTO user) throws NotFoundException, BadRequestException {
    User foundUser = userRepository.findByEmail(user.getEmail());
    if (foundUser == null) {
      throw new NotFoundException();
    }
    if (!foundUser.getPassword().equals(user.getPassword())) {
      throw new BadRequestException();
    }

    return new ResponseUserDTO(
        foundUser.getId(),
        foundUser.getName(),
        foundUser.getProfilePicture()
    );
  }

  // Create a new user
  public Optional<User> updateUserProfile(RequestUpdateProfileDTO request, String filePath)
      throws IOException, NotFoundException {
    Optional<User> user = getUserById(request.getUserId());
    if (user.isEmpty()) {
      throw new NotFoundException();
    }

    User updatedUser = new User(
        request.getUserId(),
        user.get().getEmail(),
        user.get().getPassword(),
        user.get().getName(),
        filePath
    );
    userRepository.save(updatedUser);

    return Optional.of(updatedUser);
  }

  // Get user by ID
  public Optional<User> getUserById(Long id) {
    return userRepository.findById(id);
  }

  // Get user by Email
  public User getUserByEmail(final String email) {
    return userRepository.findByEmail(email);
  }

  // Get all users
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  // Update user details
  /*
  public Optional<User> updateUser(Long id, User updatedUser) {
    return userRepository.findById(id).map(existingUser -> {
      User updated = new User(
          existingUser.getId(),  // ID는 변경되지 않음
          updatedUser.getEmail(),
          updatedUser.getPassword(),
          updatedUser.getName(),
          updatedUser.getProfilePicture(),
          existingUser.getCreatedAt() // created_at 필드는 변경하지 않음
      );
      return userRepository.save(updated);
    });
  }

   */

  // Delete user by ID
  public boolean deleteUser(Long id) {
    if (userRepository.existsById(id)) {
      userRepository.deleteById(id);
      return true;
    }
    return false;
  }

  public Optional<User> findUserById(final Long id) {
    return userRepository.findById(id);
  }
}

