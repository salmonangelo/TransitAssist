package com.transitassist.service;

import com.transitassist.dto.CreateUserRequest;
import com.transitassist.dto.UpdateUserRequest;
import com.transitassist.dto.UserResponse;
import com.transitassist.entity.User;
import com.transitassist.enums.Role;
import com.transitassist.exception.BusinessRuleException;
import com.transitassist.exception.ResourceNotFoundException;
import com.transitassist.repository.UserRepository;
import com.transitassist.util.PasswordHasher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessRuleException("A user with this email already exists.", HttpStatus.CONFLICT);
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone());
        user.setPasswordHash(PasswordHasher.hash(request.password()));
        user.setRole(request.role() == null ? Role.USER : request.role());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findUser(id));
    }

    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("Another user already uses this email.", HttpStatus.CONFLICT);
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        if (request.password() != null) {
            user.setPasswordHash(PasswordHasher.hash(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    /** "Delete" is a soft delete: the user is deactivated so old requests keep their history. */
    public void deactivate(Long id) {
        User user = findUser(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }
}
