package com.example.capstone.controller;

import com.example.capstone.dto.RequestGroupMemberDTO;
import com.example.capstone.dto.RequestTravelGroupDTO;
import com.example.capstone.dto.ResponseTravelGroupDTO;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.service.TravelGroupService;
import jakarta.persistence.EntityExistsException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class TravelGroupController {

  MediaType mediaType = new MediaType("application", "json", StandardCharsets.UTF_8);

  @Autowired
  private TravelGroupService travelGroupService;


  // Create a new travel group
  @PostMapping
  public ResponseEntity<ResponseTravelGroupDTO> createGroup(
      @RequestBody RequestTravelGroupDTO request) {
    // 그룹 생성
    Optional<ResponseTravelGroupDTO> createdGroup = travelGroupService.createGroup(request);
    if (createdGroup.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    // 그룹 멤버에 추가
    RequestGroupMemberDTO addGroupMember = RequestGroupMemberDTO.createResponseTravelGroupDTO(
        request.getCreatorId(),
        createdGroup.get().getInviteCode()
    );
    Optional<ResponseTravelGroupDTO> response = travelGroupService.joinGroupByInviteCode(
        addGroupMember);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return response.map(responseTravelGroupDTO -> ResponseEntity.status(HttpStatus.CREATED)
            .headers(headers)
            .body(responseTravelGroupDTO))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.BAD_REQUEST));
  }

  @GetMapping("/{userId}")
  public ResponseEntity<List<TravelGroup>> getGroups(@PathVariable Long userId) {
    List<Optional<TravelGroup>> groups = travelGroupService.getGroupsByMemberId(userId);
    List<TravelGroup> result = groups.stream()
        .filter(Optional::isPresent).map(Optional::get).toList();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(result);
  }

  @PostMapping("/members")
  public ResponseEntity<ResponseTravelGroupDTO> joinGroup(@RequestBody RequestGroupMemberDTO requestGroupMemberDTO) {
    Optional<ResponseTravelGroupDTO> result = travelGroupService.joinGroupByInviteCode(requestGroupMemberDTO);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return result.map(
            responseTravelGroupDTO -> ResponseEntity.status(HttpStatus.OK).headers(headers).body(result.get()))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.BAD_REQUEST));
  }

 @GetMapping("/members/{groupId}")
  public ResponseEntity<List<User>> getAllMembers(@PathVariable Long groupId) {
   List<Optional<User>> members = travelGroupService.getGroupsByGroupId(groupId);
   if (members.isEmpty()) {
     return new ResponseEntity<>(HttpStatus.NOT_FOUND);
   }
   List<User> result = members.stream()
       .filter(Optional::isPresent)
       .map(Optional::get).toList();

   HttpHeaders headers = new HttpHeaders();
   headers.setContentType(mediaType);
   return ResponseEntity.status(HttpStatus.OK).headers(headers).body(result);
  }


  @ExceptionHandler(EntityExistsException.class)
  public ResponseEntity<String> entityExistsExceptionHandler(EntityExistsException e) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return ResponseEntity.status(HttpStatus.CONFLICT).headers(headers).body(e.getMessage());
  }
}