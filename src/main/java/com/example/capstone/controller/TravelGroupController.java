package com.example.capstone.controller;

import com.example.capstone.dto.RequestGroupMemberDTO;
import com.example.capstone.dto.RequestTravelGroupDTO;
import com.example.capstone.dto.ResponseTravelGroupDTO;
import com.example.capstone.entity.GroupMember;
import com.example.capstone.entity.TravelGroup;
import com.example.capstone.entity.User;
import com.example.capstone.repository.GroupMemberRepository;
import com.example.capstone.service.TravelGroupService;
import com.example.capstone.service.UserService;
import jakarta.persistence.EntityExistsException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class TravelGroupController {

  @Autowired
  private TravelGroupService travelGroupService;

  @Autowired
  private UserService userService;
  @Autowired
  private GroupMemberRepository groupMemberRepository;

  // Create a new travel group
  @PostMapping
  public ResponseEntity<ResponseTravelGroupDTO> createGroup(@RequestBody RequestTravelGroupDTO travelGroupDTO) {
    Optional<ResponseTravelGroupDTO> createdGroup = travelGroupService.createGroup(travelGroupDTO);
    return createdGroup.map(
            responseTravelGroupDTO -> new ResponseEntity<>(responseTravelGroupDTO, HttpStatus.CREATED))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/{userId}")
  public ResponseEntity<List<TravelGroup>> getGroups(@PathVariable Long userId) {
    List<Optional<TravelGroup>> groups = travelGroupService.getGroupsByMemberId(userId);
    List<TravelGroup> result = groups.stream()
        .filter(Optional::isPresent).map(Optional::get).toList();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @PostMapping("/members")
  public ResponseEntity<ResponseTravelGroupDTO> joinGroup(@RequestBody RequestGroupMemberDTO requestGroupMemberDTO) {
    Optional<ResponseTravelGroupDTO> travelGroupDTO = travelGroupService.joinGroupByInviteCode(requestGroupMemberDTO);

    return travelGroupDTO.map(
            responseTravelGroupDTO -> new ResponseEntity<>(responseTravelGroupDTO, HttpStatus.CREATED))
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
    return new ResponseEntity<>(result, HttpStatus.OK);
  }


  @ExceptionHandler(EntityExistsException.class)
  public ResponseEntity<Void> entityExistsExceptionHandler(EntityExistsException e) {
    return new ResponseEntity<>(HttpStatus.CONFLICT);
  }
}