package com.example.gymtimer2.domain.model

/** Metadados locais informados ao salvar uma corrida. */
data class RunSaveDetails(
    /** Título opcional exibido no histórico. */
    val title: String? = null,
    /** Descrição opcional da atividade. */
    val description: String? = null,
    /** Indica que a corrida foi usada como deslocamento. */
    val isCommute: Boolean = false,
    /** Esforço percebido informado na escala de 1 a 10. */
    val perceivedExertion: Int? = null,
    /** Tipo de atividade alterado ao editar uma corrida concluída. */
    val activityType: RunActivityType? = null,
    /** Marcadores opcionais da atividade. */
    val activityTags: Set<RunActivityTag> = emptySet(),
    /** Nome local do equipamento usado nesta corrida. */
    val gearName: String? = null,
    /** Nota pessoal guardada apenas no dispositivo. */
    val privateNotes: String? = null,
    /** ID do equipamento local, quando selecionado no catálogo. */
    val gearId: Long? = null
) {
    /** Normaliza os campos antes de atravessar o serviço ou chegar ao banco. */
    fun normalized() = copy(
        title = RunTitle.normalize(title),
        description = description?.take(MAX_DESCRIPTION_LENGTH)?.trim()?.takeIf(String::isNotBlank),
        gearName = gearName?.take(MAX_GEAR_NAME_LENGTH)?.trim()?.takeIf(String::isNotBlank),
        gearId = gearId?.takeIf { it > 0 && !gearName.isNullOrBlank() },
        privateNotes = privateNotes?.take(MAX_PRIVATE_NOTES_LENGTH)?.trim()?.takeIf(String::isNotBlank),
        perceivedExertion = perceivedExertion?.takeIf { it in MIN_PERCEIVED_EXERTION..MAX_PERCEIVED_EXERTION }
    )

    companion object {
        /** Keeps the title aligned with the history card limit. */
        const val MAX_TITLE_LENGTH = RunTitle.MAX_LENGTH

        /** Limita o texto opcional salvo com cada atividade. */
        const val MAX_DESCRIPTION_LENGTH = 2_000

        /** Limita o nome de equipamento salvo com cada atividade. */
        const val MAX_GEAR_NAME_LENGTH = 80

        /** Limita a nota pessoal salva com cada atividade. */
        const val MAX_PRIVATE_NOTES_LENGTH = 2_000

        const val MIN_PERCEIVED_EXERTION = 1
        const val MAX_PERCEIVED_EXERTION = 10
    }
}
