package com.soundtube.app

import com.soundtube.app.data.YouTubeExtractor
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeExtractorTest {

    @Test
    fun testQuerySeedsNonEmpty() {
        assertTrue(YouTubeExtractor.GENERAL_SEEDS.isNotEmpty())
        assertTrue(YouTubeExtractor.AUDIOBOOK_SEEDS.isNotEmpty())
        assertTrue(YouTubeExtractor.MUSIC_SEEDS.isNotEmpty())
        assertTrue(YouTubeExtractor.PLAYLIST_SEEDS.isNotEmpty())
        assertTrue(YouTubeExtractor.PODCAST_SEEDS.isNotEmpty())
        assertTrue(YouTubeExtractor.NEWS_SEEDS.isNotEmpty())
    }

    @Test
    fun testExtractVideosAndContinuationInitialSearch() {
        val rootJson = JSONObject().apply {
            put("contents", JSONObject().apply {
                put("twoColumnSearchResultsRenderer", JSONObject().apply {
                    put("primaryContents", JSONObject().apply {
                        put("sectionListRenderer", JSONObject().apply {
                            val sections = JSONArray().apply {
                                val itemSection = JSONObject().apply {
                                    put("itemSectionRenderer", JSONObject().apply {
                                        val items = JSONArray().apply {
                                            val video = JSONObject().apply {
                                                put("videoRenderer", JSONObject().apply {
                                                    put("videoId", "test_video_123")
                                                    put("title", JSONObject().apply {
                                                        put("simpleText", "Tiêu đề truyện audio test")
                                                    })
                                                    put("ownerText", JSONObject().apply {
                                                        put("runs", JSONArray().apply {
                                                            put(JSONObject().apply {
                                                                put("text", "Kênh Truyện Hay")
                                                            })
                                                        })
                                                    })
                                                    put("lengthText", JSONObject().apply {
                                                        put("simpleText", "45:30")
                                                    })
                                                })
                                            }
                                            put(video)
                                        }
                                        put("contents", items)
                                    })
                                }
                                put(itemSection)

                                val contSection = JSONObject().apply {
                                    put("continuationItemRenderer", JSONObject().apply {
                                        put("continuationEndpoint", JSONObject().apply {
                                            put("continuationCommand", JSONObject().apply {
                                                put("token", "token_next_page_abc")
                                            })
                                        })
                                    })
                                }
                                put(contSection)
                            }
                            put("contents", sections)
                        })
                    })
                })
            })
        }

        val result = YouTubeExtractor.extractVideosAndContinuation(rootJson)
        assertEquals(1, result.videos.size)
        assertEquals("test_video_123", result.videos[0].id)
        assertEquals("Tiêu đề truyện audio test", result.videos[0].title)
        assertEquals("Kênh Truyện Hay", result.videos[0].uploaderName)
        assertEquals(45 * 60 + 30L, result.videos[0].durationSeconds)
        assertEquals("token_next_page_abc", result.continuationToken)
    }

    @Test
    fun testExtractVideosAndContinuationContinuationResponse() {
        val rootJson = JSONObject().apply {
            val commands = JSONArray().apply {
                val cmd = JSONObject().apply {
                    put("appendContinuationItemsAction", JSONObject().apply {
                        val items = JSONArray().apply {
                            val videoItem = JSONObject().apply {
                                put("videoRenderer", JSONObject().apply {
                                    put("videoId", "test_video_456")
                                    put("title", JSONObject().apply {
                                        put("runs", JSONArray().apply {
                                            put(JSONObject().apply {
                                                put("text", "Tập 2: Đấu La Đại Lục")
                                            })
                                        })
                                    })
                                    put("ownerText", JSONObject().apply {
                                        put("runs", JSONArray().apply {
                                            put(JSONObject().apply {
                                                put("text", "Kênh Đọc Truyện")
                                            })
                                        })
                                    })
                                })
                            }
                            put(videoItem)

                            val nextContItem = JSONObject().apply {
                                put("continuationItemRenderer", JSONObject().apply {
                                    put("continuationEndpoint", JSONObject().apply {
                                        put("continuationCommand", JSONObject().apply {
                                            put("token", "token_page_3_xyz")
                                        })
                                    })
                                })
                            }
                            put(nextContItem)
                        }
                        put("continuationItems", items)
                    })
                }
                put(cmd)
            }
            put("onResponseReceivedCommands", commands)
        }

        val result = YouTubeExtractor.extractVideosAndContinuation(rootJson)
        assertEquals(1, result.videos.size)
        assertEquals("test_video_456", result.videos[0].id)
        assertEquals("Tập 2: Đấu La Đại Lục", result.videos[0].title)
        assertEquals("token_page_3_xyz", result.continuationToken)
    }
}
