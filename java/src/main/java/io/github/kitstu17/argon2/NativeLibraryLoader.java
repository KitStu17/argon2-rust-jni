package io.github.kitstu17.argon2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * <p>
 * 네이티브 라이브러리 로더
 * </p>
 * JAR 내부 플랫폼별 네이티브 라이브러리를 임시 디렉토리로 추출 후 로드하는 역할
 */
public class NativeLibraryLoader {
    private static final String LIBRARY_NAME = "argon2_rust_jni";
    private static boolean loaded = false;

    /**
     * <p>
     * 네이티브 라이브러리 로드
     * </p>
     * 이미 로드된 경우 로드 X
     */
    public static synchronized void load() {
        if (loaded) {
            return;
        }

        try {
            String platform = detectPlatform();
            String libraryFileName = getLibraryFileName();
            String resourcePath = "/natives/" + platform + "/" + libraryFileName;

            // JAR 내부 리소스 확인
            InputStream libraryStream = NativeLibraryLoader.class.getResourceAsStream(resourcePath);
            if (libraryStream == null) {
                throw new UnsatisfiedLinkError(
                        "Native library not found for platform: " + platform +
                                "\nExpected path: " + resourcePath +
                                "\n\nThis may happen if:" +
                                "\n  1. You're using a development build (only contains current platform)" +
                                "\n  2. Download the production JAR from GitHub Releases" +
                                "\n  3. Or build with GitHub Actions for all platforms");
            }

            // 임시 파일로 추출
            Path tempFile = extractLibrary(libraryStream, libraryFileName);

            // 네이티브 라이브러리 로드
            System.load(tempFile.toAbsolutePath().toString());

            loaded = true;

            // 디버그 정보 출력
            if (Boolean.getBoolean("argon2.debug")) {
                System.out.println("✅ Loaded native library: " + tempFile);
                System.out.println("   Platform: " + platform);
            }
        } catch (IOException e) {
            throw new UnsatisfiedLinkError("Failed to load native library: " + e.getMessage());
        }
    }

    /**
     * 현재 플랫폼 감지
     * 
     * @return 플랫폼 문자열 (예: "linux-x86-64", "darwin-aarch64", "windows-x86-64")
     */
    private static String detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        // OS 감지
        String osName;
        if (os.contains("linux")) {
            osName = "linux";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osName = "darwin";
        } else if (os.contains("windows")) {
            osName = "windows";
        } else {
            throw new UnsatisfiedLinkError("Unsupported OS: " + os);
        }

        // 아키텍처 감지
        String archName;
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            archName = "aarch64";
        } else if (arch.contains("amd64") || arch.contains("x86_64")) {
            archName = "x86-64";
        } else {
            throw new UnsatisfiedLinkError("Unsupported architecture: " + arch);
        }

        return osName + "-" + archName;
    }

    /**
     * 플랫폼별 라이브러리 파일명 반환
     * 
     * @return 라이브러리 파일명 (예: "libargon2_rust_jni.so", "argon2_rust_jni.dll")
     */
    private static String getLibraryFileName() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("windows")) {
            return LIBRARY_NAME + ".dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            return "lib" + LIBRARY_NAME + ".dylib";
        } else {
            return "lib" + LIBRARY_NAME + ".so";
        }
    }

    /**
     * JAR 내부의 라이브러리를 임시 파일로 추출
     * 
     * @param libraryStream 라이브러리 입력 스트림
     * @param fileName      파일명
     * @return 추출퇸 임시 파일 경로
     * @throws IOException 파일 추출 실패 시 반환
     */
    private static Path extractLibrary(InputStream libraryStream, String fileName) throws IOException {
        // 임시 파일 생성
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        Path tempFile = Files.createTempFile("argon2-native-", suffix);

        // JVM 종료 시 자동 삭제 처리
        tempFile.toFile().deleteOnExit();

        // 스트림을 파일로 복사
        Files.copy(libraryStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        // 실행 권한 부여(Linux / macOS)
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            tempFile.toFile().setExecutable(true);
        }

        return tempFile;
    }

    /**
     * 라이브러리가 정상적으로 로드되었는지 확인
     * 
     * @return 로드 여부
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
