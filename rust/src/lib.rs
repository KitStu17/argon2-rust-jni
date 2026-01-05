use jni::objects::{JByteArray, JClass};
use jni::sys::{jboolean, jbyteArray, jint};
use jni::JNIEnv;

use argon2::{
    password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Algorithm, Argon2, Params, Version,
};
use password_hash::rand_core::OsRng;

/// 패스워드 해싱
///
/// Java 시그니처:
/// public static native byte[] hash(byte[] password, int memoryCost, int timeCost, int parallelism)
///
#[no_mangle]
pub extern "system" fn Java_io_github_kitstu17_argon2_Argon2Encoder_hash<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    password: JByteArray<'local>,
    memory_cost: jint,
    time_cost: jint,
    parallelism: jint,
) -> jbyteArray {
    // 1. Java byte[]를 Rust Vec<u8>로 변환
    let password_bytes = match env.convert_byte_array(&password) {
        Ok(bytes) => bytes,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Failed to read password: {}", e),
            );
            return std::ptr::null_mut();
        }
    };

    // 2. Argon2 파라미터 설정
    let params = match Params::new(
        memory_cost as u32,
        time_cost as u32,
        parallelism as u32,
        None,
    ) {
        Ok(p) => p,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid parameters: {}", e),
            );
            return std::ptr::null_mut();
        }
    };

    let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    // 3. Salt 생성
    let salt = SaltString::generate(&mut OsRng);

    // 4. 해싱
    let password_hash = match argon2.hash_password(&password_bytes, &salt) {
        Ok(hash) => hash.to_string(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Hashing failed: {}", e),
            );
            return std::ptr::null_mut();
        }
    };

    // 5. 결과를 Java byte[]로 변환
    let hash_bytes = password_hash.as_bytes();
    match env.byte_array_from_slice(hash_bytes) {
        Ok(array) => array.into_raw(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to create result: {}", e),
            );
            std::ptr::null_mut()
        }
    }
}

/// 패스워드 검증
///
/// Java 시그니처:
/// public static native boolean verify(byte[] password, byte[] hash)
#[no_mangle]
pub extern "system" fn Java_io_github_kitstu17_argon2_Argon2Encoder_verify<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    password: JByteArray<'local>,
    hash: JByteArray<'local>,
) -> jboolean {
    // 1. Java byte[] 변환
    let password_bytes = match env.convert_byte_array(&password) {
        Ok(bytes) => bytes,
        Err(_) => return jni::sys::JNI_FALSE,
    };
    let hash_bytes = match env.convert_byte_array(&hash) {
        Ok(bytes) => bytes,
        Err(_) => return jni::sys::JNI_FALSE,
    };

    // 2. Hash 파싱
    let hash_str = match std::str::from_utf8(&hash_bytes) {
        Ok(s) => s,
        Err(_) => return jni::sys::JNI_FALSE,
    };

    let parsed_hash = match PasswordHash::new(hash_str) {
        Ok(s) => s,
        Err(_) => return jni::sys::JNI_FALSE,
    };

    // 3. 검증
    let argon2 = Argon2::default();
    match argon2.verify_password(&password_bytes, &parsed_hash) {
        Ok(_) => jni::sys::JNI_TRUE,
        Err(_) => jni::sys::JNI_FALSE,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_argon2_basic() {
        let password = b"test_password";
        let salt = SaltString::generate(&mut OsRng);
        let argon2 = Argon2::default();

        let hash = argon2.hash_password(password, &salt).unwrap();
        assert!(argon2.verify_password(password, &hash).is_ok());
    }
}
