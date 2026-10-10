package moe.antimony.hoshi.features.audiobooks

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StudyListTest {
    @Test
    fun requestUrlCarriesTheGivenCalendarDay() {
        assertEquals(
            "https://study.example/schedule/today?account=me&date=2026-10-10",
            studyListRequestUrl(
                " https://study.example/schedule/today?account=me&date={date} ",
                LocalDate.of(2026, 10, 10),
            ),
        )
    }

    @Test
    fun readsIdsFromADayObject() {
        val json = """
            {"account":"me","date":"2026-10-10","settings":{"perDay":8},
             "day":{"date":"2026-10-10","done":true,"offered":["521","396","686"],"quiz":{"questions":8}},
             "days":{}}
        """.trimIndent()

        assertEquals(listOf("521", "396", "686"), parseStudyListIds(json))
    }

    @Test
    fun readsIdsFromPlainListsNumbersAndObjects() {
        assertEquals(listOf("521", "396"), parseStudyListIds("[521, \"396\", 521]"))
        assertEquals(listOf("7"), parseStudyListIds("""{"ids":[7]}"""))
        assertEquals(
            listOf("521", "42"),
            parseStudyListIds("""{"points":[{"externalId":"521","pattern":"ものなら"},{"id":42}]}"""),
        )
    }

    @Test
    fun rejectsAnAnswerWithoutAListOfIds() {
        assertThrows(IllegalStateException::class.java) { parseStudyListIds("""{"error":"date is required"}""") }
    }

    @Test
    fun findsChaptersByLeadingIdInStudyOrderAndReportsTheRest() {
        val dialogues = AudiobookLibraryBook(
            id = "dialogues",
            title = "Dialogues",
            chapters = listOf(
                AudiobookLibraryChapter(label = "513 万が一のために", startCharacter = 0),
                AudiobookLibraryChapter(label = "＃0521 戻れるものなら", startCharacter = 100),
                AudiobookLibraryChapter(label = "Introduction", startCharacter = 200),
            ),
        )
        val extra = AudiobookLibraryBook(
            id = "extra",
            title = "Extra",
            chapters = listOf(AudiobookLibraryChapter(label = "521 もう一度", startCharacter = 0)),
        )

        val today = studyListChapters(listOf(dialogues, extra), listOf("521", "396", "0513"))

        assertEquals(
            listOf("＃0521 戻れるものなら", "521 もう一度", "513 万が一のために"),
            today.chapters.map { it.chapter.label },
        )
        assertEquals(listOf("dialogues", "extra", "dialogues"), today.chapters.map { it.bookId })
        assertEquals(listOf("396"), today.missingIds)
    }

    @Test
    fun findsABookThatCarriesTheIdInItsOwnTitle() {
        val single = AudiobookLibraryBook(
            id = "single",
            title = "686 先生の下で",
            chapters = listOf(AudiobookLibraryChapter(label = "本文", startCharacter = 12)),
        )

        val today = studyListChapters(listOf(single), listOf("686"))

        assertEquals(
            listOf(AudiobookLibraryChapter(label = "686 先生の下で", startCharacter = 0)),
            today.chapters.map { it.chapter },
        )
        assertEquals(emptyList<String>(), today.missingIds)
    }
}
