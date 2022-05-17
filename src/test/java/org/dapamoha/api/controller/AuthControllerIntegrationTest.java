package org.dapamoha.api.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import org.dapamoha.api.ApiApplication;
import org.dapamoha.api.service.interf.AuthService;
import org.dapamoha.api.service.interf.EmailService;
import org.dapamoha.api.service.mq.producer.EmailRequestProducer;
import org.dapamoha.api.testUtil.GeneratorUtil;
import org.dapamoha.shared.domain.dto.user.JwtTokenDto;
import org.dapamoha.shared.domain.dto.user.SendCodeToEmailDto;
import org.dapamoha.shared.domain.util.CodeUtil;
import org.dapamoha.shared.domain.util.UuidUtil;
import org.dapamoha.shared.kafka.model.emailRequest.EmailRequestKey;
import org.dapamoha.shared.kafka.model.emailRequest.EmailRequestValue;
import org.dapamoha.shared.posgresql.entity.EmailCode;
import org.dapamoha.shared.posgresql.repository.email.EmailRepository;
import org.dapamoha.shared.posgresql.repository.emailCode.EmailCodeRepository;
import org.dapamoha.shared.posgresql.repository.user.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(classes = {ApiApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    private final String AUTH_CONTROLLER_PATH = "/api/v1/auth";
    private final String SIGN_IN_AUTH_CONTROLLER_PATH = AUTH_CONTROLLER_PATH + "/signIn";
    private final String REQUEST_EMAIL_CODE_AUTH_CONTROLLER_PATH = AUTH_CONTROLLER_PATH  + "/requestEmailCode";

    @Value("${org.dapamoha.api.auth.token.validityTime}")
    private Long tokenValidityInMs;

    @MockBean
    private EmailRequestProducer emailRequestProducer;

    @MockBean
    private UuidUtil uuidUtil;
    @MockBean
    private CodeUtil codeUtil;

    @Autowired
    private AuthService authService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailCodeRepository emailCodeRepository;

    String ADDRESS;

    @BeforeEach
    void setUp() {
        ADDRESS = GeneratorUtil.generateEmail(5, "gmail.com");
    }

    @SneakyThrows
    @Test
    void signInWhenCodeAndAddressExistShouldReturnAuthTokenDto() {
        // GIVEN
        EmailCode emailCode = authService.sendCodeToEmail(ADDRESS);
        Mockito.when(uuidUtil.randomUUID()).thenReturn(UUID.randomUUID());

        final String requestUri = SIGN_IN_AUTH_CONTROLLER_PATH
                + "?code=" + emailCode.getCode()
                + "&address=" + ADDRESS;

        // WHEN
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get(requestUri))
                .andExpect(status().isOk())
                .andReturn();

        // THEN
        MockHttpServletResponse response = result.getResponse();

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getContentAsString());

        JwtTokenDto jwtTokenDto = objectMapper.readValue(response.getContentAsString(), JwtTokenDto.class);

        Assertions.assertNotNull(jwtTokenDto);

        Assertions.assertNotNull(jwtTokenDto.getToken());
        Assertions.assertTrue(jwtTokenDto.getToken().length() > 0);

        Assertions.assertNotNull(jwtTokenDto.getTokenValidityInMs());
        Assertions.assertEquals(jwtTokenDto.getTokenValidityInMs(), tokenValidityInMs);
    }

    @SneakyThrows
    @Test
    void requestEmailCodeWhenEmailNotExistShouldCreatEmailAndSendEmail(){
        // GIVEN
        Assertions.assertEquals(0, userRepository.count());
        Assertions.assertEquals(0, emailCodeRepository.count());
        Assertions.assertEquals(0, emailRepository.count());

        emailService.create(ADDRESS);

        Assertions.assertEquals(0, userRepository.count());
        Assertions.assertEquals(1, emailCodeRepository.count());
        Assertions.assertEquals(1, emailRepository.count());

        final UUID uuid = UUID.randomUUID();
        Mockito.when(uuidUtil.randomUUID()).thenReturn(uuid);

        final Integer code = 123456;
        Mockito.when(codeUtil.generateCodeAsNumber()).thenReturn(code);

        final String contentBody = objectMapper.writeValueAsString(
                SendCodeToEmailDto.builder()
                        .email(ADDRESS)
                        .build()
        );

        // WHEN
        MvcResult result = mockMvc
                .perform(
                        MockMvcRequestBuilders

                                .post(REQUEST_EMAIL_CODE_AUTH_CONTROLLER_PATH)
                                .accept(MediaType.APPLICATION_JSON_UTF8)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(contentBody)
                )
                .andExpect(status().isOk())
                .andReturn();

        // THEN
        MockHttpServletResponse response = result.getResponse();
        Assertions.assertNotNull(response);

        Assertions.assertEquals(0, userRepository.count());
        Assertions.assertEquals(1, emailCodeRepository.count());
        Assertions.assertEquals(1, emailRepository.count());

        Assertions.assertTrue(emailRepository.isExistEmailByAddress(ADDRESS));

        EmailRequestKey key = EmailRequestKey.builder()
                .uuid(uuid)
                .build();

        EmailRequestValue value = EmailRequestValue.builder()
                .code(code)
                .address(ADDRESS)
                .build();

        Mockito.verify(emailRequestProducer, Mockito.times(1))
                .produceRequest(key, value);
    }

    @SneakyThrows
    @Test
    void requestEmailCodeWhenEmailExistShouldSendEmail(){
        // GIVEN
        Assertions.assertEquals(0, userRepository.count());
        Assertions.assertEquals(0, emailCodeRepository.count());
        Assertions.assertEquals(0, emailRepository.count());

        final UUID uuid = UUID.randomUUID();
        Mockito.when(uuidUtil.randomUUID()).thenReturn(uuid);

        final Integer code = 123456;
        Mockito.when(codeUtil.generateCodeAsNumber()).thenReturn(code);

        final String contentBody = objectMapper.writeValueAsString(
                SendCodeToEmailDto.builder()
                        .email(ADDRESS)
                        .build()
        );

        // WHEN
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .post(REQUEST_EMAIL_CODE_AUTH_CONTROLLER_PATH)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(contentBody))
                .andExpect(status().isOk())
                .andReturn();

        // THEN
        MockHttpServletResponse response = result.getResponse();
        Assertions.assertNotNull(response);

        Assertions.assertEquals(0, userRepository.count());
        Assertions.assertEquals(1, emailCodeRepository.count());
        Assertions.assertEquals(1, emailRepository.count());

        Assertions.assertTrue(emailRepository.isExistEmailByAddress(ADDRESS));

        EmailRequestKey key = EmailRequestKey.builder()
                .uuid(uuid)
                .build();

        EmailRequestValue value = EmailRequestValue.builder()
                .code(code)
                .address(ADDRESS)
                .build();

        Mockito.verify(emailRequestProducer, Mockito.times(1))
                .produceRequest(key, value);
    }
}