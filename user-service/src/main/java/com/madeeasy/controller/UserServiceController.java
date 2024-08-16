package com.madeeasy.controller;

import com.madeeasy.dto.request.UserRequestDTO;
import com.madeeasy.dto.response.UserAuthResponseDTO;
import com.madeeasy.dto.response.UserResponseDTO;
import com.madeeasy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/user-service")
@RequiredArgsConstructor
public class UserServiceController {

    private final UserService userService;

    @PostMapping(path = "/create")
    public ResponseEntity<?> createUser(@RequestBody UserRequestDTO user) {
        System.out.println("UserRequestDTO inside User-service :" + user);
        UserAuthResponseDTO savedUser = this.userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
    @PatchMapping(path = "/partial-update/{emailId}")
    public ResponseEntity<?> partiallyUpdateUser(@PathVariable("emailId") String emailId, @RequestBody UserRequestDTO user) {
        UserAuthResponseDTO updatedUser = this.userService.partiallyUpdateUser(emailId, user);
        return ResponseEntity.status(HttpStatus.OK).body(updatedUser);
    }


    @DeleteMapping(path = "/delete/{emailId}")
    public ResponseEntity<?> deleteUser(@PathVariable("emailId") String emailId) {
        this.userService.deleteUser(emailId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping(path = "/{emailId}")
    public ResponseEntity<?> getUserByEmailId(@PathVariable("emailId") String emailId) {
        UserResponseDTO user = this.userService.getUserByEmailId(emailId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }
}
