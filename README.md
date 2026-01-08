# Argon2 Rust JNI

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Rust](https://img.shields.io/badge/Rust-1.75+-orange.svg)](https://www.rust-lang.org/)

High-performance, memory-safe Argon2 password hashing library for Java/Spring Boot, powered by Rust.

## 주요 기능

- ✅ **메모리 안전성**: Rust로 구현하여 버퍼 오버플로우 및 메모리 손상 방지
- ✅ **고성능**: 순수 Java 구현 대비 2.6배 빠른 성능(평균 해싱 생성 시간 **0.17초**)
- ✅ **크로스 플랫폼**: Linux (x86_64, ARM64), macOS (Apple Silicon), Windows 지원
- ✅ **Spring Boot 지원**: Spring Security의 PasswordEncoder와 호환
- ✅ **의존성 0**: Java 런타임에서 외부 의존성 불필요
- ✅ **OWASP 준수**: OWASP 패스워드 해싱 권장사항 준수

## Rust + JNI 선택 이유

기존 Java Argon2 구현체들의 문제점:

### 1. Spring Security Cryto의 한계

```java
// Spring Security 5.8+ 필요
Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(...);
```

**문제점:**

- ❌ **매우 느린 성능**: 순수 Java 구현 (Bouncy Castle 기반)
- ❌ **Spring Security 버전 의존성**: 5.8 미만에서 사용 불가
- ❌ **레거시 프로젝트 호환성**: 오래된 Spring Boot 프로젝트에서 업그레이드 어려움

**성능 비교:**

```
Spring Security Argon2: 0.45초/해시 (매우 느림)
→ 초당 2.2 로그인 처리
→ 피크 시간 병목 발생 가능
```

<br>

### 2. argon2-jvm의 한계

```gradle
dependencies {
    implementation 'de.mkammerer:argon2-jvm:2.11'
}
```

**문제점:**

- ❌ **플랫폼 호환성 문제**: C 네이티브 라이브러리 기반
- ❌ **순수 Java fallback 성능**: 네이티브 라이브러리 없을 시 2-3배 느림
- ❌ **배포 복잡도**: OS별로 다른 라이브러리 관리 필요

<br>

### 3. Argon2 Rust JNI 솔루션

| 구현                   | 성능       | Spring 버전 의존성  | 플랫폼 호환성 |
| ---------------------- | ---------- | ------------------- | ------------- |
| **Argon2 Rust JNI**    | **0.17초** | **없음** ✅         | **완벽** ✅   |
| Spring Security        | 0.45초     | Spring 5.8+ 필요 ❌ | 완벽 ✅       |
| argon2-jvm (네이티브)  | 0.18초     | 없음 ✅             | 제한적 ⚠️     |
| argon2-jvm (Pure Java) | 0.50초     | 없음 ✅             | 완벽 ✅       |

**핵심 장점:**

✅ **성능**: Spring Security 대비 2.6배 빠름

```
100명 동시 로그인 처리:
- Spring Security: 45초
- Argon2 Rust JNI: 17초 ← 28초 단축!
```

✅ **호환성**: 모든 플랫폼 지원, 단일 JAR

```
하나의 JAR에 모든 플랫폼 포함:
- Linux x86_64 ✅
- Linux ARM64 (Oracle Cloud, AWS Graviton) ✅
- macOS Apple Silicon ✅
- Windows ✅

배포 시 플랫폼 신경 쓸 필요 없음!
```

✅ **독립성**: Spring Security 버전 무관

```java
// Spring Boot 2.x ✅
// Spring Boot 3.x ✅
// Spring Security 없는 프로젝트도 ✅
Argon2Encoder encoder = new Argon2Encoder();
```

### 성능 벤치마크 상세

```
테스트: 12개 테스트 케이스 (해싱 7회, 검증 6회)

Spring Security Crypto:    6.5초  (Pure Java)
argon2-jvm (Pure Java):    6.0초  (네이티브 없을 때)
argon2-jvm (네이티브):      2.3초  (C 라이브러리)
Argon2 Rust JNI:           2.5초  (Rust 네이티브) ✅

→ Spring Security 대비 2.6배 빠름
→ argon2-jvm 네이티브와 동등한 성능
→ 모든 플랫폼에서 일관된 성능 보장
```

## 사용 방법

### Maven

```xml
    <dependency>
        <groupId>io.github.kitstu17</groupId>
        <artifactId>argon2-rust-jni</artifactId>
        <version>0.1.0</version>
    </dependency>
```

### Gradle

```gradle
dependencies {
    implementation 'io.github.kitstu17:argon2-rust-jni:0.1.0'
}
```

### 기본 사용 방법

```java
import io.github.kitstu17.argon2.Argon2Encoder;

public class Example {
    public static void main(String[] args) {
        Argon2Encoder encoder = new Argon2Encoder();

        // 패스워드 해싱
        String hash = encoder.encode("mySecurePassword");
        // 결과: $argon2id$v=19$m=65536,t=3,p=1$...

        // 패스워드 검증
        boolean isValid = encoder.matches("mySecurePassword", hash);
        // 결과: true
    }
}
```

### Spring Boot 통합

```java
import io.github.kitstu17.argon2.Argon2Encoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            private final Argon2Encoder encoder = new Argon2Encoder();

            @Override
            public String encode(CharSequence rawPassword) {
                return encoder.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encoder.matches(rawPassword, encodedPassword);
            }
        };
    }
}
```

## 설정

### 기본 파라미터 (OWASP 권장)

```java
Argon2Encoder encoder = new Argon2Encoder();
// 메모리: 64 MB
// 반복 횟수: 3
// 병렬 처리: 1 스레드
// 소요 시간: 해시당 약 0.17초
```

### 커스텀 파라미터

```java
// 저사양 환경용
Argon2Encoder encoder = new Argon2Encoder(
    32768,  // 32 MB 메모리
    2,      // 2회 반복
    1       // 1 스레드
);
// 소요 시간: 약 0.08초

// 고보안 환경용
Argon2Encoder encoder = new Argon2Encoder(
    131072, // 128 MB 메모리
    4,      // 4회 반복
    2       // 2 스레드
);
// 소요 시간: 약 0.35초
```

## 지원 플랫폼

| 플랫폼  | 아키텍처              | 상태                         |
| ------- | --------------------- | ---------------------------- |
| Linux   | x86_64                | ✅ 지원                      |
| Linux   | ARM64                 | ✅ 지원                      |
| macOS   | Apple Silicon (ARM64) | ✅ 지원                      |
| macOS   | Intel (x86_64)        | ⚠️ 미포함 (현재 개발 계획 X) |
| Windows | x86_64                | ✅ 지원                      |

JAR 파일에는 모든 지원 플랫폼의 네이티브 라이브러리가 포함되어 있으며, 런타임에 자동으로 적절한 라이브러리를 로드합니다.

## 소스에서 빌드하기

### 사전 요구사항

- Java 21+ (추후 17버전 추가 계획 중)
- Rust 1.75+
- Gradle 8.5+

### 빌드 과정

```bash
# 저장소 클론
git clone https://github.com/kitstu17/argon2-rust-jni.git
cd argon2-rust-jni

# Rust 라이브러리 빌드
cd rust
cargo build --release
cd ..

# Java 라이브러리 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 로컬 Maven 저장소에 설치
./gradlew publishToMavenLocal
```

### 크로스 플랫폼 빌드

모든 플랫폼용 빌드는 GitHub Actions 사용:

```bash
# GitHub Actions를 통해 실행
# 참고: .github/workflows/release.yml
```

## 보안 고려사항

### Argon2id를 사용하는 이유

Argon2id는 두 가지 장점을 결합합니다:

- **Argon2d**: 데이터 의존적 (GPU 공격 방어)
- **Argon2i**: 데이터 독립적 (사이드 채널 공격 방어)

### 권장 사용 사례

| 사용 사례   | 설정                  | 시간   |
| ----------- | --------------------- | ------ |
| 일반 웹 앱  | 기본 (64MB, 3회 반복) | 0.17초 |
| 고보안 앱   | 128MB, 4회 반복       | 0.35초 |
| 관리자 계정 | 256MB, 5회 반복       | 0.80초 |
| 저사양 서버 | 32MB, 2회 반복        | 0.08초 |

### OWASP 가이드라인

이 라이브러리는 [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)를 준수합니다:

- ✅ 최소 19 MiB 메모리를 사용하는 Argon2id 사용
- ✅ 최소 2회 반복
- ✅ 최소 1의 병렬화 수준
- ✅ 패스워드당 랜덤 솔트
- ✅ 출력 길이 ≥ 32 바이트

## API 문서

### Argon2Encoder

```java
public class Argon2Encoder {
    // 생성자
    public Argon2Encoder()
    public Argon2Encoder(int memoryCost, int timeCost, int parallelism)

    // 메서드
    public String encode(CharSequence rawPassword)
    public boolean matches(CharSequence rawPassword, String encodedPassword)

    // Getter
    public int getMemoryCost()
    public int getTimeCost()
    public int getParallelism()
}
```

### 파라미터

- **memoryCost**: 메모리 사용량 (KB 단위, 최소: 1024, 기본: 65536)
- **timeCost**: 반복 횟수 (최소: 1, 기본: 3)
- **parallelism**: 스레드 수 (최소: 1, 기본: 1)

### 예외

- `NullPointerException`: 패스워드가 null인 경우
- `IllegalArgumentException`: 패스워드가 비어있거나 파라미터가 잘못된 경우

## 문제 해결

### UnsatisfiedLinkError: Native library not found

**문제:**

```
UnsatisfiedLinkError: Native library not found for platform: linux-x86-64
```

**해결책:**

1. 프로덕션 JAR를 사용하고 있는지 확인 (GitHub Actions로 빌드된 것)
2. 개발 빌드는 현재 플랫폼만 포함
3. 멀티 플랫폼 지원을 위해 GitHub Releases에서 다운로드

### 테스트 실패

**문제:**

```
java.lang.UnsatisfiedLinkError: no argon2_rust_jni in java.library.path
```

**해결책:**

```bash
# Rust 라이브러리 재빌드
./gradlew buildRust

# 또는 클린 후 재빌드
./gradlew clean build
```

## 프로젝트 구조

```
argon2-rust-jni/
├── rust/                      # Rust 소스 코드
│   ├── Cargo.toml            # Rust 의존성
│   ├── src/lib.rs            # JNI 구현
│   └── build.sh              # 빌드 스크립트
├── java/                      # Java 소스 코드
│   └── src/
│       ├── main/java/        # 라이브러리 코드
│       │   └── io/github/kitstu17/argon2/
│       │       ├── Argon2Encoder.java
│       │       └── NativeLibraryLoader.java
│       ├── main/resources/
│       │   └── natives/      # 플랫폼별 라이브러리
│       └── test/java/        # 테스트
├── .github/workflows/        # CI/CD
├── build.gradle              # Gradle 빌드 설정
└── README.md                 # 이 파일
```

## 기여하기

기여를 환영합니다! Pull Request를 자유롭게 제출해주세요.

### 개발 환경 설정

1. 저장소 Fork
2. Feature 브랜치 생성
3. 변경사항 작성
4. 테스트 실행: `./gradlew test`
5. Pull Request 제출

## 라이선스

이 프로젝트는 Apache License 2.0 라이선스 하에 배포됩니다 - 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 감사의 글

- [RustCrypto/password-hashes](https://github.com/RustCrypto/password-hashes) - Argon2 구현
- [jni-rs](https://github.com/jni-rs/jni-rs) - Rust용 JNI 바인딩
- [Spring Security](https://spring.io/projects/spring-security) - API 디자인 참고

## 제작자

**kitstu17**

- GitHub: [@kitstu17](https://github.com/kitstu17)
- Blog: [kitstu17.github.io](https://kitstu17.github.io)

## 참고 자료

- [Argon2 명세](https://github.com/P-H-C/phc-winner-argon2)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [Spring Security 문서](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)

---
