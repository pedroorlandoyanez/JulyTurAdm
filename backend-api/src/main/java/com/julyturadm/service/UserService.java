package com.julyturadm.service;

import com.julyturadm.domain.entity.User;
import com.julyturadm.domain.repository.UserRepository;
import com.julyturadm.dto.request.UserCreateRequest;
import com.julyturadm.dto.response.UserResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.julyturadm.common.exception.BusinessException;

import java.util.List;
import com.julyturadm.common.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreateRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                    "Já existe um usuário cadastrado com este e-mail.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(request.name(), request.email(), encodedPassword);

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com o ID: " + id));

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse findUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com o e-mail: " + email));

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAllActiveUsers() {
        return userRepository.findAllByEnabledTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com o ID: " + id));

        user.setEnabled(false);

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    public UserResponse enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com o ID: " + id));

        user.setEnabled(true);

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }
    
    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

}
