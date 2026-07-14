"""
自动获取 ngrok 公网 URL 并写入 Android 源码
被 start.bat 调用，无需手动运行

原理：ngrok 在本地 4040 端口暴露了管理 API，
      通过 GET /api/tunnels 获取当前隧道信息，
      提取 public_url 并替换 RemoteSegmentationService.kt 中的 SAM2_SERVER_URL
"""
import json
import re
import sys
from pathlib import Path
from urllib.request import urlopen
from urllib.error import URLError


def get_ngrok_url(timeout: int = 3) -> str | None:
    """从 ngrok 本地 API 获取公网 HTTPS URL"""
    try:
        with urlopen("http://127.0.0.1:4040/api/tunnels", timeout=timeout) as resp:
            data = json.loads(resp.read())
    except URLError:
        return None

    tunnels = data.get("tunnels", [])
    # 优先 HTTPS，其次 HTTP
    for t in tunnels:
        if t.get("proto") == "https":
            return t["public_url"]
    for t in tunnels:
        if t.get("proto") == "http":
            return t["public_url"]
    return None


def update_kotlin_file(file_path: str, new_url: str) -> bool:
    """替换 Kotlin 文件中的 SAM2_SERVER_URL"""
    p = Path(file_path)
    if not p.exists():
        print(f"  [ERROR] 文件不存在: {file_path}")
        return False

    content = p.read_text(encoding="utf-8")

    # 确保 URL 以 / 结尾
    if not new_url.endswith("/"):
        new_url += "/"

    # 匹配模式: private const val SAM2_SERVER_URL = "..."
    pattern = r'(private const val SAM2_SERVER_URL\s*=\s*")[^"]*(")'
    new_content = re.sub(pattern, rf'\1{new_url}\2', content)

    if new_content == content:
        print("  [WARN] 未找到 SAM2_SERVER_URL 声明，文件可能已变更")
        return False

    p.write_text(new_content, encoding="utf-8")
    return True


def main():
    print("  查询 ngrok 隧道信息...")
    url = get_ngrok_url()

    if url is None:
        print()
        print("  [FAIL] 无法获取 ngrok 公网地址，请检查：")
        print("    1. ngrok 是否已启动")
        print("    2. 等待 5 秒后重试")
        print()
        print("  手动操作：打开 http://127.0.0.1:4040 复制 Forwarding 地址")
        print("  然后填入 RemoteSegmentationService.kt 的 SAM2_SERVER_URL")
        sys.exit(1)

    print(f"  获取到公网地址: {url}")

    # Android 源码路径（相对于 sam2_server 目录向上两级）
    script_dir = Path(__file__).parent
    kotlin_file = script_dir.parent / "app" / "src" / "main" / "java" / "com" / "example" / "lexiscan" / "data" / "Service" / "RemoteSegmentationService.kt"

    if update_kotlin_file(str(kotlin_file), url):
        print(f"  [OK] 已自动更新 RemoteSegmentationService.kt")
        print(f"  [OK] URL = {url}")
    else:
        print(f"  [FAIL] 自动更新失败，请手动修改")
        sys.exit(1)


if __name__ == "__main__":
    main()
