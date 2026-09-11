package com.example.shikagera1.domain

/**
 * Модель рабочего дня: 4 часа работы → 45 минут перерыва → 4 часа работы.
 *
 * «Присутствие» — время от прихода до ухода за вычетом исключённых минут.
 * «Работа» — присутствие без перерыва. Перерыв вычитается только если
 * присутствие дотянуло до него: ушёл через 3 часа — отработал 3 часа.
 *
 * Базовые функции параметризованы единицей измерения, чтобы одна логика
 * служила и балансу в минутах, и живому таймеру в секундах.
 */
object WorkDayPhases {
    enum class Phase { FIRST_HALF, BREAK, SECOND_HALF }

    fun phase(presence: Long, firstHalf: Long, breakLength: Long): Phase = when {
        presence < firstHalf -> Phase.FIRST_HALF
        presence < firstHalf + breakLength -> Phase.BREAK
        else -> Phase.SECOND_HALF
    }

    fun worked(presence: Long, firstHalf: Long, breakLength: Long): Long =
        when (phase(presence, firstHalf, breakLength)) {
            Phase.FIRST_HALF -> presence
            Phase.BREAK -> firstHalf
            Phase.SECOND_HALF -> presence - breakLength
        }

    /** Сколько присутствия нужно, чтобы набрать [work] чистой работы. */
    fun presenceFor(work: Long, firstHalf: Long, breakLength: Long): Long =
        if (work <= firstHalf) work else work + breakLength

    fun phaseMinutes(presenceMinutes: Int): Phase =
        phase(presenceMinutes.toLong(), FIRST_HALF_MIN, BREAK_MIN)

    fun workedMinutes(presenceMinutes: Int): Int =
        worked(presenceMinutes.coerceAtLeast(0).toLong(), FIRST_HALF_MIN, BREAK_MIN).toInt()

    fun presenceForWorkMinutes(workMinutes: Int): Int =
        presenceFor(workMinutes.coerceAtLeast(0).toLong(), FIRST_HALF_MIN, BREAK_MIN).toInt()

    fun phaseSeconds(presenceSeconds: Long): Phase =
        phase(presenceSeconds, FIRST_HALF_SEC, BREAK_SEC)

    fun workedSeconds(presenceSeconds: Long): Long =
        worked(presenceSeconds.coerceAtLeast(0), FIRST_HALF_SEC, BREAK_SEC)

    /** Секунд до конца текущей фазы; для второй половины — 0. */
    fun secondsLeftInPhase(presenceSeconds: Long): Long = when (phaseSeconds(presenceSeconds)) {
        Phase.FIRST_HALF -> FIRST_HALF_SEC - presenceSeconds
        Phase.BREAK -> FIRST_HALF_SEC + BREAK_SEC - presenceSeconds
        Phase.SECOND_HALF -> 0
    }

    private const val FIRST_HALF_MIN = WorkConstants.FIRST_HALF_MINUTES.toLong()
    private const val BREAK_MIN = WorkConstants.BREAK_MINUTES.toLong()
    private const val FIRST_HALF_SEC = FIRST_HALF_MIN * 60
    private const val BREAK_SEC = BREAK_MIN * 60
}
