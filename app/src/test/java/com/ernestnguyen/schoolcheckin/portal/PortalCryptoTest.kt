package com.ernestnguyen.schoolcheckin.portal

import org.junit.Assert.assertEquals
import org.junit.Test

/** Expected values were produced by running the portal's own `AE` (crypto-js.js) under Node. */
class PortalCryptoTest {
    @Test
    fun encryptMatchesPortal() {
        assertEquals(REQUEST_ENCRYPTED, PortalCrypto.encrypt(REQUEST_JSON, "DSA4BRINKCIpAiAPKSAv"))
    }

    @Test
    fun decryptMatchesPortal() {
        assertEquals(RESPONSE_JSON, PortalCrypto.decrypt(RESPONSE_ENCRYPTED, PortalCrypto.IM))
    }

    @Test
    fun roundTripsVietnameseText() {
        val text = "Từ khóa điểm danh: Lập trình di động"
        assertEquals(text, PortalCrypto.decrypt(PortalCrypto.encrypt(text, "AzzSystem"), "AzzSystem"))
    }
}

internal const val REQUEST_JSON =
    """{"action":"SV_ThongTin_MH/DSA4BRINKCIpAiAPKSAv","func":"pkg_congthongtin_hssv_thongtin.LayDSLichCaNhan","iM":"AzzSystem","strQLSV_NguoiHoc_Id":"ABC123","strNgayBatDau":"12/10/2026","strNgayKetThuc":"18/10/2026"}"""

internal const val REQUEST_ENCRYPTED =
    "P3EgVzY7JiBpeWsjFzYVOCQ9JiItPR55Cn0NHQp3CyIIJwoTAiMAHwUDCmcDJGtiaSU8HiJLe3I7OCYpJzwvUzY6JiAsNyAeHgEyIz0MNR4rPSZAKzxnAio6DSMNACI4CDIPHiU9YxhgOwRscWEICjs6OCM/NixUaHEyQDADBR0dHAcXNAYoGCQwHj8gcXsWAxAKf3lwa1xjGjUiBTQgDwYyNXAjJ2t0aXJ7X3BZbmJ7YXdUaHEyQDAcLi8yCCwEFQE0M2lpY0d8fHAEbWB5fH1hNA=="

internal const val RESPONSE_JSON =
    """[{"TENHOCPHAN":"Lập trình di động","TENPHONGHOC":"Phòng 301","GIOBATDAU":7,"NGAYHOC":"13/10/2026"}]"""

internal const val RESPONSE_ENCRYPTED =
    "GgFYBzw9PCouETI7HVtJVinhu4AxWg4hwpUdHEUJKFrFq+G6ihcUVklPFT80AzE8OiIlDjlYaVsjHMKXAyZaSWNIUVhHKgg1OBItNzUwT3tNVnE3NDU8JQ45WGlbQkdKXHFVSGNLRVYYMA=="
