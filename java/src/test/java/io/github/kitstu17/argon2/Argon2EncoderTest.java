package io.github.kitstu17.argon2;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Argon2Encoder 단위 테스트
 */
public class Argon2EncoderTest {
    private static Argon2Encoder encoder;

    @BeforeAll
    static void setUp() {
        encoder = new Argon2Encoder();
    }

    @Test
    @DisplayName("패스워드를 성공적으로 해싱해야 함")
    void shouldHashPassword() {
        String password = "mySecurityPassword123!";

        String hash = encoder.encode(password);

        assertThat(hash).isNotNull();
        assertThat(hash).startsWith("$argon2id$");
        assertThat(hash.length()).isGreaterThan(50);
    }

    @Test
    @DisplayName("같은 패스워드도 매번 다른 해시를 생성해야 함 (Salt 때문)")
    void shouldGenerateDifferntHashesForSamePassword() {
        String password = "samePassword";

        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("올바른 패스워드 검증에 성공해야 함")
    void shouldVerifyCorrectPassword() {
        String password = "correctPassword";
        String hash = encoder.encode(password);

        boolean matches = encoder.matches(password, hash);

        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("잘못된 패스워드는 검증에 실패해야 함")
    void shouldRejectIncorrectPassword() {
        String password = "correctPassword";
        String wrongPassword = "wrongPassword";
        String hash = encoder.encode(password);

        boolean matches = encoder.matches(wrongPassword, hash);

        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("null 패스워드는 NullPointerException이 발생해야 함")
    void shouldThrowExceptionForNullPassword() {
        assertThatThrownBy(() -> encoder.encode(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Raw password cannot be null");
    }

    @Test
    @DisplayName("빈 패스워드는 IllegalArgumentException을 발생시켜야 함")
    void shouldThrowExceptionForEmptyPassword() {
        assertThatThrownBy(() -> encoder.encode(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Raw password cannot be empty");
    }
    
    @Test
    @DisplayName("커스텀 파라미터로 생성된 인코더도 정상 작동해야 함")
    void shouldWorkWithCustomParameters() {
        // Given
        Argon2Encoder customEncoder = new Argon2Encoder(32768, 2, 1);
        String password = "testPassword";
        
        // When
        String hash = customEncoder.encode(password);
        boolean matches = customEncoder.matches(password, hash);
        
        // Then
        assertThat(hash).startsWith("$argon2id$");
        assertThat(matches).isTrue();
    }
    
    @Test
    @DisplayName("잘못된 메모리 비용은 IllegalArgumentException을 발생시켜야 함")
    void shouldThrowExceptionForInvalidMemoryCost() {
        assertThatThrownBy(() -> new Argon2Encoder(512, 3, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Memory cost must be at least 1024 KB");
    }
    
    @Test
    @DisplayName("잘못된 반복 횟수는 IllegalArgumentException을 발생시켜야 함")
    void shouldThrowExceptionForInvalidTimeCost() {
        assertThatThrownBy(() -> new Argon2Encoder(65536, 0, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Time cost must be at least 1");
    }
    
    @Test
    @DisplayName("한글 패스워드도 정상적으로 처리해야 함")
    void shouldHandleKoreanCharacters() {
        // Given
        String password = "한글비밀번호123!";
        
        // When
        String hash = encoder.encode(password);
        boolean matches = encoder.matches(password, hash);
        
        // Then
        assertThat(hash).startsWith("$argon2id$");
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("특수문자가 포함된 패스워드도 정상적으로 처리해야 함")
    void shouldHandleSpecialCharacters() {
        // Given
        String password = "P@ssw0rd!#$%^&*()";
        
        // When
        String hash = encoder.encode(password);
        boolean matches = encoder.matches(password, hash);
        
        // Then
        assertThat(hash).startsWith("$argon2id$");
        assertThat(matches).isTrue();
    }
    
    @Test
    @DisplayName("긴 패스워드도 정상적으로 처리해야 함")
    void shouldHandleLongPasswords() {
        // Given
        String password = "a".repeat(1000);
        
        // When
        String hash = encoder.encode(password);
        boolean matches = encoder.matches(password, hash);
        
        // Then
        assertThat(hash).startsWith("$argon2id$");
        assertThat(matches).isTrue();
    }
}
