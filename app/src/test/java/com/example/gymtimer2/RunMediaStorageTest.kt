package com.example.gymtimer2

import com.example.gymtimer2.data.repository.MAX_RUN_IMAGE_SIZE_BYTES
import com.example.gymtimer2.data.repository.MAX_RUN_VIDEO_SIZE_BYTES
import com.example.gymtimer2.data.repository.copyToLimited
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RunMediaStorageTest {
    @Test
    fun copyRejectsMediaLargerThanTheLocalLimit() {
        val output = ByteArrayOutputStream()
        ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)).copyToLimited(output, 4)
        assertEquals(4, output.size())

        assertThrows(IOException::class.java) {
            ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5)).copyToLimited(output, 4)
        }
        assertEquals(4, output.size())
        assertEquals(20 * 1024 * 1024L, MAX_RUN_IMAGE_SIZE_BYTES)
        assertEquals(100 * 1024 * 1024L, MAX_RUN_VIDEO_SIZE_BYTES)
    }
}
