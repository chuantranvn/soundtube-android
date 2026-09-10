package com.soundtube.app

import com.soundtube.app.data.EpisodeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeManagerTest {

    @Test
    fun testParseStandardVietnameseEpisode() {
        val title = "VĂN PHÒNG BẮT QUỶ - TẬP 1 | TÁO LINH DỊ"
        val info = EpisodeManager.parseEpisode(title)
        assertNotNull(info)
        assertEquals(1, info?.episodeNumber)
        assertEquals("VĂN PHÒNG BẮT QUỶ", info?.seriesTitle)
    }

    @Test
    fun testParseChuongAndHoi() {
        val titleChuong = "Đấu La Đại Lục - Chương 152: Thần Cấp Vũ Hồn"
        val infoChuong = EpisodeManager.parseEpisode(titleChuong)
        assertNotNull(infoChuong)
        assertEquals(152, infoChuong?.episodeNumber)

        val titleHoi = "Tam Quốc Diễn Nghĩa - Hồi 45: Quần Anh Hội Tưởng Cán Trúng Kế"
        val infoHoi = EpisodeManager.parseEpisode(titleHoi)
        assertNotNull(infoHoi)
        assertEquals(45, infoHoi?.episodeNumber)
    }

    @Test
    fun testParsePartAndEp() {
        val titlePart = "Audiobook: Đắc Nhân Tâm - Part 3"
        val infoPart = EpisodeManager.parseEpisode(titlePart)
        assertNotNull(infoPart)
        assertEquals(3, infoPart?.episodeNumber)

        val titleEp = "Thần Thoại Hy Lạp - Ep 08"
        val infoEp = EpisodeManager.parseEpisode(titleEp)
        assertNotNull(infoEp)
        assertEquals(8, infoEp?.episodeNumber)
    }

    @Test
    fun testParseSo() {
        val titleSo = "FULL AUDIO| BÁO THỦ SỐ 1 | GÃ ĐIÊN TRỊ KẺ QUẤY RỐI"
        val infoSo = EpisodeManager.parseEpisode(titleSo)
        assertNotNull(infoSo)
        assertEquals(1, infoSo?.episodeNumber)
    }

    @Test
    fun testNonEpisodeVideoReturnsNull() {
        val title = "Sơn Tùng M-TP | Chạy Ngay Đi | Official Music Video"
        val info = EpisodeManager.parseEpisode(title)
        assertNull(info)
    }
}
