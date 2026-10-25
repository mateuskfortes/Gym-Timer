package com.example.gymtimer2.domain.model

/** Define o mesmo limite e a mesma normalização para a entrada e a persistência do título. */
object RunTitle {
    /** Mantém nomes curtos e legíveis nos cartões e detalhes. */
    const val MAX_LENGTH = 80

    /** Remove espaços externos e trata o título vazio como ausência de nome. */
    fun normalize(title: String?): String? = title?.trim()?.take(MAX_LENGTH)?.takeIf { it.isNotBlank() }
}
