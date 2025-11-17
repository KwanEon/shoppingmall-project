package com.example.project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import jakarta.persistence.EntityManager;
import com.example.project.model.User;
import com.example.project.model.Product;
import com.example.project.dto.RegisterDTO;
import com.example.project.dto.ProductDTO;
import com.example.project.dto.ReviewDTO;
import com.example.project.service.UserService;
import com.example.project.service.ProductService;
import com.example.project.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.example.project.repository.UserRepository;
import com.example.project.service.EmailService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

	@Autowired
	private EntityManager em;

	@Autowired
	private UserService userService;

	@Autowired
	private ProductService productService;

	@Autowired
	private ReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;
	
	@MockitoBean
	private EmailService emailService;

    @Test
    void 회원가입_성공() throws Exception {
        // given
        RegisterDTO request = RegisterDTO.builder()
				.username("testuser")
				.name("홍길동")
				.password("password123")
				.email("email@email.com")
				.phoneNumber("01012345678")
				.build();

		String requestJson = objectMapper.writeValueAsString(request);

        // when
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(content().string("회원가입 성공"));

        // then
        User savedUser = userRepository.findByUsername("testuser").orElseThrow();
		assertThat(savedUser.getUsername()).isEqualTo("testuser");
		assertThat(savedUser.getName()).isEqualTo("홍길동");
        assertThat(savedUser.getEmail()).isEqualTo("email@email.com");
        assertThat(savedUser.isEnabled()).isFalse();  // 이메일 인증 전
        assertThat(savedUser.getVerificationToken()).isNotBlank();
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();

        verify(emailService, times(1))
                .sendVerificationEmail(eq("email@email.com"), contains("/auth/verify?token="));
    }

	@Test
	void 회원가입_중복회원예외() throws Exception {
		// given
		RegisterDTO request1 = RegisterDTO.builder()
				.username("testuser")
				.name("홍길동")
				.password("password123")
				.email("email@email.com")
				.phoneNumber("01012345678")
				.build();

		RegisterDTO request2 = RegisterDTO.builder()
				.username("testuser")
				.name("김철수")
				.password("password456")
				.email("email@email.com")
				.phoneNumber("01087654321")
				.build();

		String requestJson1 = objectMapper.writeValueAsString(request1);
		String requestJson2 = objectMapper.writeValueAsString(request2);

		// when
		mockMvc.perform(post("/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson1))
			.andExpect(status().isOk())
			.andExpect(content().string("회원가입 성공"));

		// then
		mockMvc.perform(post("/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson2))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("error"))
			.andExpect(jsonPath("$.message").value("이미 등록된 아이디입니다."));

		verify(emailService, times(1))
				.sendVerificationEmail(eq("email@email.com"), contains("/auth/verify?token="));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			userService.saveUser(request2);
		});
		assertThat(exception.getMessage()).isEqualTo("이미 등록된 아이디입니다.");
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	void 상품_상세정보_조회() throws Exception {
		// given
		MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);
		userService.saveUser(RegisterDTO.builder()
				.username("testuser")
				.name("홍길동")
				.password("password123")
				.email("email@email.com")
				.phoneNumber("01012345678")
				.build());

		userService.saveUser(RegisterDTO.builder()
				.username("testuser2")
				.name("김철수")
				.password("password456")
				.email("email2@email.com")
				.phoneNumber("01087654321")
				.build());

		productService.addProduct(ProductDTO.builder()
				.name("테스트 상품")
				.description("테스트 상품 설명")
				.price(10000)
				.stock(50)
				.category(Product.Category.ELECTRONICS)
				.build(), image);

		reviewService.addReview(1L, 1L, ReviewDTO.builder()
				.rating(5)
				.reviewText("첫번째 리뷰 내용")
				.build());

		reviewService.addReview(1L, 2L, ReviewDTO.builder()
				.rating(4)
				.reviewText("두번째 리뷰 내용")
				.build());

		em.flush();
		em.clear();

		// when & then
		mockMvc.perform(get("/products/1")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.name").value("테스트 상품"))
			.andExpect(jsonPath("$.description").value("테스트 상품 설명"))
			.andExpect(jsonPath("$.price").value(10000))
			.andExpect(jsonPath("$.stock").value(50))
			.andExpect(jsonPath("$.category").value("ELECTRONICS"))
			.andExpect(jsonPath("$.reviews[0].reviewer").value("홍길동"))
			.andExpect(jsonPath("$.reviews[0].reviewText").value("첫번째 리뷰 내용"))
			.andExpect(jsonPath("$.reviews[0].rating").value(5))
			.andExpect(jsonPath("$.reviews[1].reviewer").value("김철수"))
			.andExpect(jsonPath("$.reviews[1].reviewText").value("두번째 리뷰 내용"))
			.andExpect(jsonPath("$.reviews[1].rating").value(4));
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	void 상품_상세정보_조회_NOFETCH() throws Exception {
		// given
		MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);
		userService.saveUser(RegisterDTO.builder()
				.username("testuser")
				.name("홍길동")
				.password("password123")
				.email("email@email.com")
				.phoneNumber("01012345678")
				.build());

		userService.saveUser(RegisterDTO.builder()
				.username("testuser2")
				.name("김철수")
				.password("password456")
				.email("email2@email.com")
				.phoneNumber("01087654321")
				.build());

		productService.addProduct(ProductDTO.builder()
				.name("테스트 상품")
				.description("테스트 상품 설명")
				.price(10000)
				.stock(50)
				.category(Product.Category.ELECTRONICS)
				.build(), image);

		reviewService.addReview(1L, 1L, ReviewDTO.builder()
				.rating(5)
				.reviewText("첫번째 리뷰 내용")
				.build());

		reviewService.addReview(1L, 2L, ReviewDTO.builder()
				.rating(4)
				.reviewText("두번째 리뷰 내용")
				.build());

		em.flush();
		em.clear();

		// when
		Product product = productService.getProductById(1L);
		String userName1 = product.getReviews().get(0).getUser().getName();
		String userName2 = product.getReviews().get(1).getUser().getName();

		// then
		assertThat(userName1).isEqualTo("홍길동");
		assertThat(userName2).isEqualTo("김철수");
	}
}
