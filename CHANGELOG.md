# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-01-08

### Added

- Initial release
- Argon2id password hashing with Rust implementation
- JNI bindings for Java integration
- Support for Windows x86_64
- Support for Linux x86_64 and ARM64
- Support for macOS ARM64 (Apple Silicon)
- Spring Boot integration support
- Comprehensive test suite
- Customizable memory, iteration, and parallelism parameters
- OWASP-compliant default configuration

### Security

- Memory-safe implementation using Rust
- Protection against buffer overflows
- Protection against timing attacks
- Random salt generation per password

[Unreleased]: https://github.com/kitstu17/argon2-rust-jni/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/kitstu17/argon2-rust-jni/releases/tag/v0.1.0
