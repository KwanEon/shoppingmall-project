package com.example.project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.example.project.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.project.dto.RegisterDTO;
import com.example.project.dto.UserDTO;
import com.example.project.security.CustomUserDetails;
import com.example.project.repository.UserRepository;
import jakarta.validation.Valid;
import com.example.project.model.User;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserRepository userRepository;
  private final UserService userService;

    @PostMapping("/register")   // 회원가입
    public ResponseEntity<?> Register(@RequestBody @Valid RegisterDTO registerDTO, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, List<FieldError>> grouped = new LinkedHashMap<>();
            for (FieldError fe : result.getFieldErrors()) {
                grouped.computeIfAbsent(fe.getField(), k -> new ArrayList<>()).add(fe);
            }

            Map<String, String> fieldErrors = new HashMap<>();
            for (Map.Entry<String, List<FieldError>> entry : grouped.entrySet()) {
                List<FieldError> list = entry.getValue();
                FieldError primary = list.stream()
                                        .filter(e -> {
                                            String code = e.getCode();
                                            return "NotBlank".equals(code) || "NotEmpty".equals(code) || "NotNull".equals(code);
                                        })
                                        .findFirst()
                                        .orElse(list.get(0));
                fieldErrors.put(entry.getKey(), primary.getDefaultMessage());
            }

            return ResponseEntity.badRequest().body(Map.of(
                "status", "validation_error",
                "errors", fieldErrors
            ));
        }

        try {
            userService.saveUser(registerDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

  @GetMapping("/admin/userlist")	// 유저 리스트 불러오기
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<User>> ShowUserList(@AuthenticationPrincipal CustomUserDetails principal) {
    if (principal == null || !principal.hasRole("ROLE_ADMIN")) {  // 로그인 안했거나 관리자 권한이 없는 경우
        return ResponseEntity.status(403).body(null);  // 권한 없음
    }
    List<User> userList = userService.getUserList();
    return ResponseEntity.ok(userList);
  }

  @PostMapping("/user/name")	// 유저 수정 (이름)
  public ResponseEntity<?> EditUsername(@AuthenticationPrincipal CustomUserDetails principal,
                             @RequestParam("newName") String newName) {
    try {
        Long userId = principal.getUserId();
        userService.updateName(userId, newName);
    } catch (IllegalArgumentException e) {   // 예외 처리
        return ResponseEntity.badRequest().body("유저 이름 수정 실패: " + e.getMessage());
    }
    return ResponseEntity.ok("유저 이름 수정 성공");
  }

  @DeleteMapping("/user")    // 유저 삭제
  public ResponseEntity<?> DeleteUser(@AuthenticationPrincipal CustomUserDetails principal) {
    try {
        Long userId = principal.getUserId();
        userService.deleteUser(userId);  // 유저 삭제
    } catch (IllegalArgumentException e) {   // 예외 처리
        return ResponseEntity.badRequest().body("유저 삭제 실패: " + e.getMessage());
    }
    return ResponseEntity.ok("유저 삭제 성공");
  }

  @GetMapping("/auth/user")   // 현재 로그인한 유저 정보 조회
  public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails principal) {
    if (principal == null) {
        return ResponseEntity.status(401).body("로그인 하지 않은 사용자입니다.");
    }
    User user = principal.getUser();
    UserDTO dto = new UserDTO(user.getId(), user.getUsername(), user.getName(),
                               user.getEmail(), user.getPhoneNumber(), user.getAddress());
    return ResponseEntity.ok(dto);
  }

  @GetMapping("/auth/role")   // 현재 로그인한 유저의 권한 조회
  public ResponseEntity<?> getCurrentUserRole(@AuthenticationPrincipal CustomUserDetails principal) {
    String role = principal.getAuthorities().isEmpty()
                  ? "ANONYMOUS"
                  : principal.getAuthorities().iterator().next().getAuthority();

    return ResponseEntity.ok(role);
  }

  @GetMapping("/auth/verify")  // 이메일 인증
  public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
    User user = userRepository.findByVerificationToken(token)
        .orElseThrow(() -> new RuntimeException("유효하지 않은 토큰"));

    user.setEnabled(true);
    user.setVerificationToken(null); // 재사용 방지
    userRepository.save(user);

    return ResponseEntity.ok("이메일 인증 성공");
  }
}