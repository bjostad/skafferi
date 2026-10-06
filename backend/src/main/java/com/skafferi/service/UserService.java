package com.skafferi.service;

import com.skafferi.domain.User;
import com.skafferi.dto.UserDto;
import com.skafferi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createOrUpdateUser(UserDto dto) {
        User user = dto.id() != null ? userRepository.findById(dto.id()).orElse(new User()) : new User();
        user.setUsername(dto.username().trim());
        user.setDisplayName(dto.displayName() != null && !dto.displayName().isBlank() ? dto.displayName().trim() : dto.username().trim());
        user.setEmail(dto.email());
        user.setRole(dto.role() != null ? dto.role() : "ADMIN");
        user.setAvatarColor(dto.avatarColor() != null ? dto.avatarColor() : "#8B5CF6");
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String id) {
        if (userRepository.count() > 1) {
            userRepository.deleteById(id);
        } else {
            throw new IllegalStateException("Cannot delete the only remaining user in the household.");
        }
    }
}
