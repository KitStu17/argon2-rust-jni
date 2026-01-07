use jni::objects::{JByteArray, JClass};
use jni::sys::{jboolean, jbyteArray, jint};
use jni::JNIEnv;

use argon2::{
    password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Algorithm, Argon2, Params, Version,
};
use rand_core::OsRng;

/// 패스워드를 Argon2id로 해싱합니다.
///
/// # JNI 함수 시그니처
/// ```java
/// private static native byte[] hash(
///     byte[] password,
///     int memoryCost,
///     int timeCost,
///     int parallelism
/// );
/// ```
///
/// # 파라미터
/// - `password`: 해싱할 패스워드 (UTF-8 바이트 배열)
/// - `memory_cost`: 메모리 비용 (KB 단위)
/// - `time_cost`: 반복 횟수
/// - `parallelism`: 병렬 처리 스레드 수
///
/// # 반환값
/// Argon2 해시 문자열 (예: "$argon2id$v=19$m=65536,t=3,p=1$...")
#[no_mangle]
pub extern "system" fn Java_io_github_kitstu17_argon2_Argon2Encoder_hash<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    password: JByteArray<'local>,
    memory_cost: jint,
    time_cost: jint,
    parallelism: jint,
) -> jbyteArray {
    // JNI 에러 처리 래퍼
    match hash_internal(&mut env, password, memory_cost, time_cost, parallelism) {
        Ok(result) => result,
        Err(e) => {
            // Java 예외 발생
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Argon2 hashing failed: {}", e),
            );
            std::ptr::null_mut()
        }
    }
}

/// 패스워드와 해시를 검증합니다.
///
/// # JNI 함수 시그니처
/// ```java
/// private static native boolean verify(byte[] password, byte[] hash);
/// ```
///
/// # 파라미터
/// - `password`: 검증할 패스워드 (UTF-8 바이트 배열)
/// - `hash`: 저장된 해시 문자열 (UTF-8 바이트 배열)
///
/// # 반환값
/// - `true`: 패스워드 일치
/// - `false`: 패스워드 불일치 또는 에러
#[no_mangle]
pub extern "system" fn Java_io_github_kitstu17_argon2_Argon2Encoder_verify<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    password: JByteArray<'local>,
    hash: JByteArray<'local>,
) -> jboolean {
    match verify_internal(&env, password, hash) {
        Ok(valid) => valid as jboolean,
        Err(_) => 0, // false
    }
}

/// 해싱 내부 구현
fn hash_internal<'local>(
    env: &mut JNIEnv<'local>,
    password: JByteArray<'local>,
    memory_cost: jint,
    time_cost: jint,
    parallelism: jint,
) -> Result<jbyteArray, Box<dyn std::error::Error>> {
    // 1. Java byte[] → Rust Vec<u8>
    let password_bytes = env.convert_byte_array(&password)?;

    // 2. Argon2 파라미터 설정
    let params = Params::new(
        memory_cost as u32,
        time_cost as u32,
        parallelism as u32,
        None, // Output length (기본값 사용)
    )?;

    // 3. Argon2id 인스턴스 생성
    let argon2 = Argon2::new(
        Algorithm::Argon2id, // Argon2id (가장 안전)
        Version::V0x13,      // 버전 19 (최신)
        params,
    );

    // 4. 랜덤 Salt 생성 (OS 랜덤 소스 사용)
    let salt = SaltString::generate(&mut OsRng);

    // 5. 패스워드 해싱
    let password_hash = argon2.hash_password(&password_bytes, &salt)?.to_string();

    // 6. Rust String → Java byte[]
    let result = env.byte_array_from_slice(password_hash.as_bytes())?;

    Ok(result.into_raw())
}

/// 검증 내부 구현
fn verify_internal<'local>(
    env: &JNIEnv<'local>,
    password: JByteArray<'local>,
    hash: JByteArray<'local>,
) -> Result<bool, Box<dyn std::error::Error>> {
    // 1. Java byte[] → Rust Vec<u8>
    let password_bytes = env.convert_byte_array(&password)?;
    let hash_bytes = env.convert_byte_array(&hash)?;

    // 2. Hash 문자열 파싱
    let hash_str = std::str::from_utf8(&hash_bytes)?;
    let parsed_hash = PasswordHash::new(hash_str)?;

    // 3. 패스워드 검증
    let result = Argon2::default()
        .verify_password(&password_bytes, &parsed_hash)
        .is_ok();

    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_argon2_basic() {
        use argon2::password_hash::{PasswordHasher, PasswordVerifier, SaltString};
        use argon2::{Algorithm, Argon2, Params, Version};

        let password = b"test_password";
        let params = Params::new(19456, 2, 1, None).unwrap();
        let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

        let salt = SaltString::generate(&mut OsRng);
        let hash = argon2.hash_password(password, &salt).unwrap();

        assert!(Argon2::default().verify_password(password, &hash).is_ok());
        assert!(Argon2::default().verify_password(b"wrong", &hash).is_err());
    }
}
