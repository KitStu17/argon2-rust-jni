#!/bin/bash
set -e

echo "🦀 Building Argon2 Rust JNI for multiple platforms..."
echo ""

# 현재 플랫폼만 빌드 (기본)
if [ "$1" == "--local" ]; then
    echo "Building for current platform only..."
    cargo build --release
    echo "✅ Build complete!"
    exit 0
fi

# 타겟 추가 (아직 없으면)
echo "📦 Adding build targets..."
rustup target add x86_64-unknown-linux-gnu 2>/dev/null || true
rustup target add aarch64-unknown-linux-gnu 2>/dev/null || true
rustup target add x86_64-apple-darwin 2>/dev/null || true
rustup target add aarch64-apple-darwin 2>/dev/null || true
rustup target add x86_64-pc-windows-gnu 2>/dev/null || true

echo ""
echo "🔨 Building for multiple platforms..."

# Linux x86_64
if command -v x86_64-linux-gnu-gcc &> /dev/null; then
    echo "  → Linux x86_64..."
    cargo build --release --target x86_64-unknown-linux-gnu
else
    echo "  ⚠️  Skipping Linux x86_64 (cross-compiler not found)"
fi

# Linux ARM64
if command -v aarch64-linux-gnu-gcc &> /dev/null; then
    echo "  → Linux ARM64..."
    cargo build --release --target aarch64-unknown-linux-gnu
else
    echo "  ⚠️  Skipping Linux ARM64 (cross-compiler not found)"
fi

# macOS (현재 플랫폼이 macOS인 경우만)
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "  → macOS x86_64..."
    cargo build --release --target x86_64-apple-darwin
    
    echo "  → macOS ARM64 (Apple Silicon)..."
    cargo build --release --target aarch64-apple-darwin
else
    echo "  ⚠️  Skipping macOS builds (not on macOS)"
fi

# Windows (크로스 컴파일)
if command -v x86_64-w64-mingw32-gcc &> /dev/null; then
    echo "  → Windows x86_64..."
    cargo build --release --target x86_64-pc-windows-gnu
else
    echo "  ⚠️  Skipping Windows (cross-compiler not found)"
fi

echo ""
echo "✅ Build complete!"
echo ""
echo "📂 Built libraries:"
find target -name "*.so" -o -name "*.dylib" -o -name "*.dll" | grep release