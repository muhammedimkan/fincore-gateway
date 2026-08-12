package com.imkan.fincore.user;

import com.imkan.fincore.user.dto.CreateUserRequest;
import com.imkan.fincore.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/api/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request){
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        User newUser = repository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromEntity(newUser));
    }
}
