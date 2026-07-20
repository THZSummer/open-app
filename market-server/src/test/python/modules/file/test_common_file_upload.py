"""
通用文件上传接口集成测试

覆盖场景：
    L1 - 正常流程：上传图标/示意图
    L2 - 业务规则：格式校验、尺寸校验、大小校验
    L4 - 边界/反向：空文件、不支持类型、参数缺失

依赖：
    - pytest (标记: L1, L2, L4)
    - requests
    - common.api (API 客户端包装)
    - common.db (数据库助手)
"""

import pytest
import io
import struct
import zlib

# ==================== 辅助函数 ====================


def _create_png(width, height):
    """
    创建指定尺寸的 PNG 图片字节数据
    使用纯 Python 构建最小 PNG 文件
    """
    def create_chunk(chunk_type, data):
        chunk = chunk_type + data
        crc = struct.pack(">I", zlib.crc32(chunk) & 0xFFFFFFFF)
        return struct.pack(">I", len(data)) + chunk + crc

    # PNG Signature
    signature = b'\x89PNG\r\n\x1a\n'

    # IHDR chunk
    ihdr_data = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    ihdr = create_chunk(b'IHDR', ihdr_data)

    # IDAT chunk (minimal compressed data)
    raw_data = b''
    for y in range(height):
        raw_data += b'\x00'  # filter byte
        for x in range(width):
            raw_data += b'\xff\x00\x00'  # RGB pixel

    compressor = zlib.compressobj()
    compressed_data = compressor.compress(raw_data) + compressor.flush()
    idat = create_chunk(b'IDAT', compressed_data)

    # IEND chunk
    iend = create_chunk(b'IEND', b'')

    return signature + ihdr + idat + iend


def _create_jpg(width, height):
    """
    创建指定尺寸的 JPEG 文件
    使用 Java ImageIO 生成的参考 JPEG，仅支持 520x288
    """
    _JPEG_BYTES = (
        bytes.fromhex("ffd8ffe000104a46494600010200000100010000ffdb004300080606070605080707070909080a0c") +
        bytes.fromhex("140d0c0b0b0c1912130f141d1a1f1e1d1a1c1c20242e2720222c231c1c2837292c30313434341f27") +
        bytes.fromhex("393d38323c2e333432ffdb0043010909090c0b0c180d0d1832211c21323232323232323232323232") +
        bytes.fromhex("3232323232323232323232323232323232323232323232323232323232323232323232323232ffc0") +
        bytes.fromhex("0011080120020803012200021101031101ffc4001f00000105010101010101000000000000000001") +
        bytes.fromhex("02030405060708090a0bffc400b5100002010303020403050504040000017d010203000411051221") +
        bytes.fromhex("31410613516107227114328191a1082342b1c11552d1f02433627282090a161718191a2526272829") +
        bytes.fromhex("2a3435363738393a434445464748494a535455565758595a636465666768696a737475767778797a") +
        bytes.fromhex("838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6") +
        bytes.fromhex("c7c8c9cad2d3d4d5d6d7d8d9dae1e2e3e4e5e6e7e8e9eaf1f2f3f4f5f6f7f8f9faffc4001f010003") +
        bytes.fromhex("0101010101010101010000000000000102030405060708090a0bffc400b511000201020404030407") +
        bytes.fromhex("05040400010277000102031104052131061241510761711322328108144291a1b1c109233352f015") +
        bytes.fromhex("6272d10a162434e125f11718191a262728292a35363738393a434445464748494a53545556575859") +
        bytes.fromhex("5a636465666768696a737475767778797a82838485868788898a92939495969798999aa2a3a4a5a6") +
        bytes.fromhex("a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae2e3e4e5e6e7e8e9ea") +
        bytes.fromhex("f2f3f4f5f6f7f8f9faffda000c03010002110311003f0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028") +
        bytes.fromhex("a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a00ff") +
        bytes.fromhex("d9")
    )
    return _JPEG_BYTES


# ==================== L1: 正常流程测试 ====================


class TestAdminUploadL1:
    """上传正常流程"""

    @pytest.mark.L1
    def test_upload_icon_success(self, api):
        """上传能力图标（PNG 40×40）成功"""
        png_data = _create_png(40, 40)
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("icon.png", png_data, "image/png")},
                   data={"bizType": 1})
        assert resp["code"] == "200", f"上传图标失败: {resp}"
        assert "batchId" in resp["data"]
        assert "showUrl" in resp["data"]

    @pytest.mark.L1
    def test_upload_diagram_success(self, api):
        """上传能力示意图（PNG 520×288）成功"""
        png_data = _create_png(520, 288)
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("diagram.png", png_data, "image/png")},
                   data={"bizType": 2})
        assert resp["code"] == "200", f"上传示意图失败: {resp}"
        assert "batchId" in resp["data"]
        assert "showUrl" in resp["data"]

    @pytest.mark.L1
    def test_upload_and_retrieve_file(self, api):
        """
        上传文件后，通过 showUrl 能正确获取文件内容

        验证点：
        1. 上传成功返回 batchId + showUrl
        2. GET showUrl 返回 HTTP 200
        3. 响应体为原始文件内容
        """
        import requests

        # 上传文件
        png_data = _create_png(40, 40)
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("icon.png", png_data, "image/png")},
                   data={"bizType": 1})
        assert resp["code"] == "200", f"上传失败: {resp}"
        show_url = resp["data"]["showUrl"]

        # 通过 showUrl 获取文件
        # showUrl 格式如 /ability-files/xxx.png，需要拼完整 URL
        from common.config import MARKET_SERVER_BASE
        full_url = MARKET_SERVER_BASE + show_url
        file_resp = requests.get(full_url, timeout=10)
        assert file_resp.status_code == 200, f"GET showUrl 失败: HTTP {file_resp.status_code}"

        # 验证返回内容
        retrieved_data = file_resp.content
        assert len(retrieved_data) == len(png_data), \
            f"文件大小不匹配: 期望 {len(png_data)}, 实际 {len(retrieved_data)}"
        assert retrieved_data == png_data, "文件内容不匹配"


# ==================== L2: 业务规则测试 ====================


class TestAdminUploadL2:
    """上传业务规则校验"""

    @pytest.mark.L2
    def test_icon_wrong_format(self, api):
        """图标上传非 PNG/SVG 格式应返回 400"""
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("icon.gif", b"fake-gif", "image/gif")},
                   data={"bizType": 1})
        assert resp["code"] == "400"
        assert "PNG/SVG" in resp.get("messageZh", "")

    @pytest.mark.L2
    def test_icon_file_too_large(self, api):
        """图标文件超过 200KB 应返回 400"""
        large_data = b"0" * (201 * 1024)  # > 200KB
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("icon.png", large_data, "image/png")},
                   data={"bizType": 1})
        assert resp["code"] == "400"

    @pytest.mark.L2
    def test_icon_wrong_dimensions(self, api):
        """图标尺寸不是 40×40 应返回 400"""
        png_data = _create_png(100, 100)
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("icon.png", png_data, "image/png")},
                   data={"bizType": 1})
        assert resp["code"] == "400"
        assert "40×40PX" in resp.get("messageZh", "")

    @pytest.mark.L2
    def test_diagram_wrong_format(self, api):
        """示意图上传非 PNG/JPG 格式应返回 400"""
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("diagram.gif", b"fake-gif", "image/gif")},
                   data={"bizType": 2})
        assert resp["code"] == "400"
        assert "PNG/JPG" in resp.get("messageZh", "")

    @pytest.mark.L2
    def test_diagram_file_too_large(self, api):
        """示意图文件超过 500KB 应返回 400"""
        large_data = b"0" * (501 * 1024)  # > 500KB
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("diagram.png", large_data, "image/png")},
                   data={"bizType": 2})
        assert resp["code"] == "400"

    @pytest.mark.L2
    def test_diagram_wrong_dimensions(self, api):
        """示意图尺寸不是 520×288 应返回 400"""
        png_data = _create_png(520, 300)
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("diagram.png", png_data, "image/png")},
                   data={"bizType": 2})
        assert resp["code"] == "400"
        assert "520×288PX" in resp.get("messageZh", "")

    @pytest.mark.L2
    def test_diagram_jpg_accepted(self, api):
        """示意图上传 JPG 格式应通过校验"""
        jpg_data = _create_jpg(520, 288)
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("diagram.jpg", jpg_data, "image/jpeg")},
                   data={"bizType": 2})
        assert resp["code"] == "200", f"JPG 示意图上传失败: {resp}"

    @pytest.mark.L2
    def test_svg_icon_accepted(self, api):
        """SVG 格式图标应通过校验"""
        svg_data = b'<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40"></svg>'
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("icon.svg", svg_data, "image/svg+xml")},
                   data={"bizType": 1})
        assert resp["code"] == "200", f"SVG 图标上传失败: {resp}"


# ==================== L4: 边界/反向测试 ====================


class TestAdminUploadL4:
    """上传边界/反向测试"""

    @pytest.mark.L4
    def test_empty_file(self, api):
        """空文件应返回 400"""
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("empty.png", b"", "image/png")},
                   data={"bizType": 1})
        assert resp["code"] == "400"

    @pytest.mark.L4
    def test_missing_biz_type(self, api):
        """缺少 bizType 参数应返回 400"""
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("test.png", b"test", "image/png")},
                   data={})
        assert resp["code"] == "400"

    @pytest.mark.L4
    def test_missing_file(self, api):
        """缺少文件参数应返回 400"""
        resp = api("POST", "/service/open/v2/file/upload",
                   files={},
                   data={"bizType": 1})
        assert resp["code"] == "400"

    @pytest.mark.L4
    def test_unsupported_biz_type(self, api):
        """不支持的业务类型应返回 400"""
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("test.png", b"test", "image/png")},
                   data={"bizType": 99})
        assert resp["code"] == "400"

    @pytest.mark.L4
    def test_no_extension_file(self, api):
        """无扩展名的文件应返回 400"""
        resp = api("POST", "/service/open/v2/file/upload",
                   files={"file": ("noext", b"test", "application/octet-stream")},
                   data={"bizType": 1})
        assert resp["code"] == "400"
