package io.github.kitstu17.argon2;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <p>
 * Argon2 패스워드 해싱 인코더
 * </p>
 * 
 * Rust로 구현한 Native Argon2id 알고리즘을 사용하여
 * 안전하고 빠른 패스워드 해싱을 제공
 * 
 * <p>
 * 사용 예시:
 * </p>
 * 
 * <pre>{@code
 * Argon2Encoder = encoder = new Argon2Encoder();
 * String hash = encoder.encode("myPassword");
 * boolean matches = encoder.matches("myPassword", hash);
 * }</pre>
 * 
 * <p>
 * Spring Security와 통합
 * </p>
 * 
 * <pre>{@code
 *  &#64;Bean
 * public PasswordEncoder passwordEncoder() {
 *      return new PasswordEncoder() {
 *          private final Argon2Encoder encoder = new Argon2Encoder();
 * 
 *          &#64;Override
 *          public String encode(CharSequence rawPassword) {
 *              return encoder.encode(rawPassword);
 *          }
 * 
 *          &#64;Override
 *          public boolean matches(CharSequence rawPassword, String encodedPassword) {
 *              return encoder.matches(rawPassword, encodedPassword);
 *          }
 *      }
 * }
 * }</pre>
 */
public class Argon2Encoder {
    // Argon2 기본 파라미터 (OWASP 권장 사양)
    private static final int DEFAULT_MEMORY_COST = 65536; // 64 MB
    private static final int DEFAULT_TIME_COST = 3; // 3 iterations
    private static final int DEFAULT_PARALLELISM = 1; // 1 thread

    private final int memoryCost;
    private final int timeCost;
    private final int parallelism;

    // 정적 초기화 블록: 네이티브 라이브러리 로드
    static {
        NativeLibraryLoader.load();
    }

    /**
     * 기본 파라미터로 인코더 생성
     * - Memory: 64 MB
     * - Iterations: 3
     * - Parallelism: 1
     */
    public Argon2Encoder() {
        this(DEFAULT_MEMORY_COST, DEFAULT_TIME_COST, DEFAULT_PARALLELISM);
    }

    /**
     * 커스텀 파라미터로 인코더 생성
     * 
     * @param memoryCost  메모리 비용(KB 단위, 최소 1024)
     * @param timeCost    반복 횟수(최소 1)
     * @param parallelism 병렬 처리 스레드 수(최소 1)
     */
    public Argon2Encoder(int memoryCost, int timeCost, int parallelism) {
        if (memoryCost < 1024) {
            throw new IllegalArgumentException("Memory cost must be at least 1024 KB");
        }
        if (timeCost < 1) {
            throw new IllegalArgumentException("Time cost must be at least 1");
        }
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism must be at least 1");
        }

        this.memoryCost = memoryCost;
        this.timeCost = timeCost;
        this.parallelism = parallelism;
    }

    /**
     * 패스워드 해싱
     * 
     * @param rawPassword 원본 패스워드
     * @return Argon2 해시 문자열 (예: "argon2id$v=19$m=65536,t=3,p=1$...")
     * @throws NullPointerException     rawPassword가 null인 경우
     * @throws IllegalArgumentException rawPassword가 비어있는 경우
     */
    public String encode(CharSequence rawPassword) {
        Objects.requireNonNull(rawPassword, "Raw password cannot be null");

        String password = rawPassword.toString();
        if (password.isEmpty()) {
            throw new IllegalArgumentException("Raw password cannot be empty");
        }

        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] hashBytes = hash(passwordBytes, memoryCost, timeCost, parallelism);

        return new String(hashBytes, StandardCharsets.UTF_8);
    }

    /**
     * 패스워드와 해시 일치 여부 검증
     * 
     * @param rawPassword     검증할 원본 패스워드
     * @param encodedPassword 저장된 해시 문자열
     * @return 일치 여부
     * @throws NullPointerException 인자가 null인 경우
     */
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        Objects.requireNonNull(rawPassword, "Raw password cannot be null");
        Objects.requireNonNull(encodedPassword, "Encoded password cannot be null");

        if (rawPassword.toString().isEmpty() || encodedPassword.isEmpty()) {
            return false;
        }

        byte[] passwordBytes = rawPassword.toString().getBytes(StandardCharsets.UTF_8);
        byte[] hashBytes = encodedPassword.getBytes(StandardCharsets.UTF_8);

        return verify(passwordBytes, hashBytes);
    }

    /**
     * 패스워드 해싱 (네이티브 메서드)
     * 
     * @param password    패스워드 바이트 배열
     * @param memoryCost  메모리 비용(KB)
     * @param timeCost    반복 횟수
     * @param parallelism 병렬 처리 스레드 수
     * @return 해시 문자열 바이트 배열
     */
    private static native byte[] hash(
            byte[] password,
            int memoryCost,
            int timeCost,
            int parallelism);

    /**
     * 패스워드 해시 검증 (네이티브 메서드)
     * 
     * @param password 패스워드 바이트 배열
     * @param hash     해시 문자열 바이트 배열
     * @return 일치 여부
     */
    private static native boolean verify(byte[] password, byte[] hash);

    public int getMemoryCost() {
        return this.memoryCost;
    }

    public int getTimeCost() {
        return this.timeCost;
    }

    public int getParallelism() {
        return this.parallelism;
    }
}
