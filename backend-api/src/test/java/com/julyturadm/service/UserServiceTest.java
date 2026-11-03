package com.julyturadm.service;

import com.julyturadm.common.exception.BusinessException;
import com.julyturadm.domain.entity.User;
import com.julyturadm.domain.repository.UserRepository;
import com.julyturadm.dto.request.UserCreateRequest;
import com.julyturadm.dto.response.UserResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.julyturadm.common.exception.ResourceNotFoundException;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserCreateRequest request;

    @BeforeEach
    void setUp() {
        request = new UserCreateRequest(
            "Usuario Teste",
            "usuario.teste@example.com",
            "SenhaSegura123"
        );
    }

    @Test
    void shouldCreateUserSuccessfully() {
        when(userRepository.existsByEmail(request.email()))
            .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
            .thenReturn("$2a$10$senhaCodificadaDeTeste");

        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);

                assertEquals(
                    "$2a$10$senhaCodificadaDeTeste",
                    user.getPassword()
                );

                return user;
            });

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("Usuario Teste", response.name());
        assertEquals(request.email(), response.email());
        assertTrue(response.enabled());
        assertNull(response.id());

        verify(userRepository).existsByEmail(request.email());
        verify(passwordEncoder).encode(request.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail(request.email()))
            .thenReturn(true);

        assertThrows(
            BusinessException.class,
            () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void shouldEncodePasswordBeforeSavingUser() {
        when(userRepository.existsByEmail(request.email()))
            .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
            .thenReturn("senha-codificada");

        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(request);

        verify(passwordEncoder).encode(request.password());

        verify(userRepository).save(argThat(user ->
            "senha-codificada".equals(user.getPassword())
        ));
    }
          
    @Test
    void shouldNotExposePasswordInResponseDto() {
        assertThrows(
           NoSuchFieldException.class,
           () -> UserResponse.class.getDeclaredField("password")
        );
    }

    @Test
    void shouldFindUserById() {
        User user = User.create(
            "Usuario Teste",
            "usuario.teste@example.com",
            "senha-codificada"
        );
        user.setId(1L);

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        UserResponse response = userService.findUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Usuario Teste", response.name());

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldFindUserByEmail() {
        User user = User.create(
            "Usuario Teste",
            "usuario.teste@example.com",
            "senha-codificada"
        );

        when(userRepository.findByEmail(request.email()))
            .thenReturn(Optional.of(user));

        UserResponse response =
            userService.findUserByEmail(request.email());

        assertNotNull(response);
        assertEquals(request.email(), response.email());

        verify(userRepository).findByEmail(request.email());
    }

    @Test
    void shouldReturnOnlyActiveUsers() {
        User activeUser = User.create(
            "Usuario Ativo",
            "ativo@example.com",
            "senha-codificada"
        );

        when(userRepository.findAllByEnabledTrue())
            .thenReturn(List.of(activeUser));

        List<UserResponse> responses =
            userService.findAllActiveUsers();

        assertEquals(1, responses.size());
        assertEquals("Usuario Ativo", responses.get(0).name());
        assertTrue(responses.get(0).enabled());

        verify(userRepository).findAllByEnabledTrue();
    }

    @Test
    void shouldDisableUser() {
        User user = User.create(
            "Usuario Teste",
            "usuario.teste@example.com",
            "senha-codificada"
        );
        user.setId(1L);

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.disableUser(1L);

        assertNotNull(response);
        assertFalse(response.enabled());

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void shouldEnableUser() {
        User user = User.create(
            "Usuario Teste",
            "usuario.teste@example.com",
            "senha-codificada"
        );
        user.setId(1L);
        user.setEnabled(false);

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.enableUser(1L);

        assertNotNull(response);
        assertTrue(response.enabled());

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> userService.findUserById(99L)
        );

        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

}
