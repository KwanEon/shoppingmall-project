package com.example.project.service;

import com.example.project.dto.RegisterDTO;
import com.example.project.model.User;
import com.example.project.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public User findUserById(Long id) {  // 유저 조회
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
    }

    public List<User> getUserList() {   // 유저 리스트 조회
        return userRepository.findAll();
    }

    public RegisterDTO getUserDTOById(Long id) {    // 유저 DTO 변환
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        return RegisterDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public Long saveUser(RegisterDTO RegisterDTO) {      // 유저 등록
        if (userRepository.existsByUsername(RegisterDTO.getUsername())) {
            throw new IllegalArgumentException("이미 등록된 아이디입니다.");
        }
        if (userRepository.existsByEmail(RegisterDTO.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }
        if (userRepository.existsByPhoneNumber(RegisterDTO.getPhoneNumber())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }

        User.Role role = User.Role.USER;
        if ("admin".equals(RegisterDTO.getUsername())) {  // 아이디가 admin인 경우 ADMIN 권한 부여
            role = User.Role.ADMIN;
        }

        String token = UUID.randomUUID().toString(); // 이메일 인증 토큰 생성

        User user = User.builder()
                .username(RegisterDTO.getUsername())
                .password(bCryptPasswordEncoder.encode(RegisterDTO.getPassword()))
                .name(RegisterDTO.getName())
                .email(RegisterDTO.getEmail())
                .phoneNumber(RegisterDTO.getPhoneNumber())
                .role(role)
                .enabled(false)
                .verificationToken(token)
                .build();

        userRepository.save(user);

        // 이메일 전송
        String link = "http://localhost:3000/auth/verify?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), link);

        return user.getId();
    }

    public void updateName(Long userId, String newName) {   // 이름 변경
        User user = findUserById(userId);
        if (user.getName().equals(newName)) {
            throw new IllegalArgumentException("새 이름이 현재 이름과 같습니다.");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 비어 있을 수 없습니다.");
        }
        if (newName.length() < 2 || newName.length() > 5) {
            throw new IllegalArgumentException("이름은 2자 이상, 5자 이하로 입력해주세요.");
        }
        user.setName(newName);
        userRepository.save(user);
    }

    public void updatePassword(Long userId, String currentPassword, String newPassword) {   // 비밀번호 변경
        User user = findUserById(userId);
        if (!bCryptPasswordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (bCryptPasswordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호가 현재 비밀번호와 같습니다.");
        }
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void updatePhoneNumber(Long userId, String newPhoneNumber) {     // 전화번호 변경
        User user = findUserById(userId);
        if (user.getPhoneNumber().equals(newPhoneNumber)) {
            throw new IllegalArgumentException("새 전화번호가 현재 전화번호와 같습니다.");
        }
        if (userRepository.existsByPhoneNumber(newPhoneNumber)) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }
        user.setPhoneNumber(newPhoneNumber);
        userRepository.save(user);
    }

    public void updateEmail(Long userId, String newEmail) {    // 이메일 변경
        User user = findUserById(userId);
        if (user.getEmail().equals(newEmail)) {
            throw new IllegalArgumentException("새 이메일이 현재 이메일과 같습니다.");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }
        user.setEmail(newEmail);
        userRepository.save(user);
    }

    public void deleteUser(Long id) {   // 유저 삭제
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {   // 모든 유저 조회
        return userRepository.findAll();
    }
}
