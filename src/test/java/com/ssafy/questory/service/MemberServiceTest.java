package com.ssafy.questory.service;

import com.ssafy.questory.common.exception.CustomException;
import com.ssafy.questory.common.exception.ErrorCode;
import com.ssafy.questory.config.jwt.CustomUserDetailsService;
import com.ssafy.questory.config.jwt.JwtService;
import com.ssafy.questory.domain.Member;
import com.ssafy.questory.dto.request.member.MemberLoginRequestDto;
import com.ssafy.questory.dto.request.member.MemberRegistRequestDto;
import com.ssafy.questory.dto.request.member.MemberSearchRequestDto;
import com.ssafy.questory.dto.response.member.MemberRegistResponseDto;
import com.ssafy.questory.dto.response.member.MemberSearchResponseDto;
import com.ssafy.questory.dto.response.member.MemberTokenResponseDto;
import com.ssafy.questory.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class MemberServiceTest {

    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private CustomUserDetailsService userDetailsService;
    private JwtService jwtService;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        jwtService = mock(JwtService.class);

        memberService = new MemberService(
                memberRepository,
                passwordEncoder,
                authenticationManager,
                userDetailsService,
                jwtService
        );
    }

    @Test
    @DisplayName("회원가입 성공")
    void registerMember_success() {
        MemberRegistRequestDto request = MemberRegistRequestDto.builder()
                .email("test@example.com")
                .password("1234")
                .nickname("nickname")
                .build();

        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("encodedPassword");
        when(memberRepository.regist(any(Member.class))).thenReturn(1);

        MemberRegistResponseDto response = memberService.regist(request);

        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("nickname");

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).regist(captor.capture());
        Member savedMember = captor.getValue();

        assertThat(savedMember.getEmail()).isEqualTo("test@example.com");
        assertThat(savedMember.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedMember.getNickname()).isEqualTo("nickname");
    }

    @Test
    @DisplayName("중복 이메일 예외 발생")
    void registerMember_duplicateEmail() {
        MemberRegistRequestDto request = MemberRegistRequestDto.builder()
                .email("duplicate@example.com")
                .password("1234")
                .nickname("nickname")
                .build();

        when(memberRepository.findByEmail("duplicate@example.com"))
                .thenReturn(Optional.of(mock(Member.class)));

        assertThatThrownBy(() -> memberService.regist(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("로그인 성공 시 토큰 반환")
    void login_success() {
        String email = "test@test.com";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";
        String expectedToken = "jwt.token.value";

        MemberLoginRequestDto request = MemberLoginRequestDto.builder()
                .email(email)
                .password(rawPassword)
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(email);
        when(userDetails.getPassword()).thenReturn(encodedPassword);

        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(expectedToken);

        MemberTokenResponseDto response = memberService.login(request);

        verify(authenticationManager).authenticate(
                argThat(authToken -> authToken instanceof UsernamePasswordAuthenticationToken &&
                        authToken.getPrincipal().equals(email) &&
                        authToken.getCredentials().equals(rawPassword))
        );

        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getAccessToken()).isEqualTo(expectedToken);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외 발생")
    void login_emailNotFound_throwsException() {
        String email = "notfound@test.com";
        String password = "password";

        MemberLoginRequestDto request = MemberLoginRequestDto.builder()
                .email(email)
                .password(password)
                .build();

        when(userDetailsService.loadUserByUsername(email))
                .thenThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않을 경우 예외 발생")
    void login_passwordMismatch_throwsException() {
        String email = "test@test.com";
        String rawPassword = "wrongpassword";
        String encodedPassword = "encodedPassword";

        MemberLoginRequestDto request = MemberLoginRequestDto.builder()
                .email(email)
                .password(rawPassword)
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(email);
        when(userDetails.getPassword()).thenReturn(encodedPassword);

        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_PASSWORD.getMessage());
    }
}
