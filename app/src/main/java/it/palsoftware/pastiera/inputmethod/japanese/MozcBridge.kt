package it.palsoftware.pastiera.inputmethod.japanese

import android.content.Context
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands

/** Small synchronous bridge for the Mozc session command ABI. */
class MozcBridge(private val context: Context) {
    private var sessionId = 0L
    private var initialized = false
    private var activeComposition = false

    fun hasActiveComposition(): Boolean = activeComposition

    data class CandidateItem(val value: String, val id: Int, val index: Int)

    fun resetContext() {
        if (!initialized || sessionId == 0L) return
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
            .setId(sessionId)
            .setCommand(
                ProtoCommands.SessionCommand.newBuilder()
                    .setType(ProtoCommands.SessionCommand.CommandType.RESET_CONTEXT)
                    .setId(sessionId.toInt())
                    .build()
            ).build()
        eval(ProtoCommands.Command.newBuilder().setInput(input).build())
        activeComposition = false
    }

    fun ensureInitialized() {
        if (initialized) return
        MozcBackend.load(context)
        val response = eval(
            ProtoCommands.Command.newBuilder().setInput(
                ProtoCommands.Input.newBuilder()
                    .setType(ProtoCommands.Input.CommandType.CREATE_SESSION)
                    .setId(0L)
                    .build()
            ).build()
        )
        sessionId = response.output.id
        if (sessionId == 0L) {
            throw IllegalStateException("Mozc CREATE_SESSION did not return a session ID")
        }
        initialized = true
    }

    fun reset() {
        sessionId = 0L
        initialized = false
        activeComposition = false
    }

    fun sendText(
        text: String,
        modifiers: List<ProtoCommands.KeyEvent.ModifierKey> = emptyList()
    ): State {
        val codePoint = if (text.codePointCount(0, text.length) == 1) {
            text.codePointAt(0)
        } else {
            return sendKey(keyString = text)
        }
        // Mozc's key_code is a printable UCS4 value, not Android's key code.
        return sendKey(keyCode = codePoint, modifiers = modifiers)
    }

    fun sendSpecial(key: ProtoCommands.KeyEvent.SpecialKey): State {
        return sendKey(specialKey = key)
    }

    /** Production segment resize path for the active MS-IME-compatible keymap. */
    fun sendSegmentResize(right: Boolean): State = sendKey(
        specialKey = if (right) {
            ProtoCommands.KeyEvent.SpecialKey.RIGHT
        } else {
            ProtoCommands.KeyEvent.SpecialKey.LEFT
        },
        modifiers = listOf(ProtoCommands.KeyEvent.ModifierKey.SHIFT)
    )

    /** Production absolute cursor move within an active Mozc preedit. */
    fun sendMoveCursor(position: Int): State {
        ensureInitialized()
        val activeBefore = activeComposition
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
            .setId(sessionId)
            .setCommand(
                ProtoCommands.SessionCommand.newBuilder()
                    .setType(ProtoCommands.SessionCommand.CommandType.MOVE_CURSOR)
                    .setCursorPosition(position)
                    .build()
            ).build()
        val state = toState(eval(ProtoCommands.Command.newBuilder().setInput(input).build()))
        if (!state.consumed) activeComposition = activeBefore
        return state
    }

    data class PartialCommitOutcome(
        val success: Boolean,
        val committedText: String? = null,
        val finalState: State? = null,
        val failureReason: String? = null,
        val recoveryAttempted: Boolean = false,
        val recoverySucceeded: Boolean = false
    )

    /** Commit the first conversion segment and keep Mozc's remaining conversion active. */
    fun commitFirstSegment(
        expectedKeyText: String?,
        expectedSegmentCount: Int
    ): PartialCommitOutcome {
        ensureInitialized()
        val commitKey = ProtoCommands.KeyEvent.newBuilder()
            .setInputStyle(ProtoCommands.KeyEvent.InputStyle.AS_IS)
            .setSpecialKey(ProtoCommands.KeyEvent.SpecialKey.DOWN)
            .addModifierKeys(ProtoCommands.KeyEvent.ModifierKey.CTRL)
            .build()
        val commitResponse = eval(
            ProtoCommands.Command.newBuilder().setInput(
                ProtoCommands.Input.newBuilder()
                    .setType(ProtoCommands.Input.CommandType.SEND_KEY)
                    .setId(sessionId)
                    .setKey(commitKey)
                    .build()
            ).build()
        )
        val commitOutput = commitResponse.output
        val committedText = if (commitOutput.hasResult()) commitOutput.result.value else null
        val partialState = partialConversionState(commitResponse)
        val commitAccepted = commitOutput.hasConsumed() && commitOutput.consumed &&
            !committedText.isNullOrEmpty() && partialState != null &&
            partialState.phase == Phase.CONVERSION &&
            partialState.composition.isNotEmpty() && partialState.segmentCount > 0
        if (!commitAccepted) {
            // A consumed CommitOnlyFirstSegment may already have mutated the
            // native session even when one of the required output fields is
            // missing. Treat it as a possible partial commit and restore the
            // pre-command conversion context via Mozc's one-level UNDO.
            val partialCommitMayHaveOccurred =
                (commitOutput.hasConsumed() && commitOutput.consumed) ||
                    !committedText.isNullOrEmpty()
            val recoveryState = if (partialCommitMayHaveOccurred) {
                sendUndoForPartialCommitRecovery()
            } else null
            val recoverySucceeded = recoveryState != null && recoveryState.consumed &&
                recoveryState.phase == Phase.CONVERSION && activeComposition &&
                recoveryState.segmentCount == expectedSegmentCount &&
                (expectedKeyText == null || recoveryState.compositionKeyText == expectedKeyText)
            return PartialCommitOutcome(
                success = false,
                committedText = committedText,
                failureReason = "commit_response_invalid",
                recoveryAttempted = partialCommitMayHaveOccurred,
                recoverySucceeded = recoverySucceeded
            )
        }
        return PartialCommitOutcome(
            success = true,
            committedText = committedText,
            finalState = partialState
        )
    }

    /** Parse CommitOnlyFirstSegment without applying global result->inactive semantics. */
    private fun partialConversionState(command: ProtoCommands.Command): State? {
        val output = command.output
        if (!output.hasPreedit()) return null
        val segments = output.preedit.segmentList
        if (segments.isEmpty()) return null
        val hasHighlight = segments.any {
            it.hasAnnotation() &&
                it.annotation == ProtoCommands.Preedit.Segment.Annotation.HIGHLIGHT
        }
        val hasUnderline = segments.any {
            it.hasAnnotation() &&
                it.annotation == ProtoCommands.Preedit.Segment.Annotation.UNDERLINE
        }
        if (!hasHighlight || (!hasUnderline && segments.size > 1)) return null
        val candidateWindow = output.takeIf { it.hasCandidateWindow() }?.candidateWindow
        val windowItems = candidateWindow?.candidateList.orEmpty().map {
            CandidateItem(it.value, it.id, it.index)
        }
        val allItems = if (output.hasAllCandidateWords()) {
            output.allCandidateWords.candidatesList.map {
                CandidateItem(it.value, it.id, it.index)
            }
        } else emptyList()
        val candidateItems = if (allItems.isNotEmpty()) allItems else windowItems
        val keyText = segments.takeIf { it.all { segment -> segment.hasKey() } }
            ?.joinToString("") { it.key }
        val focusedKeyLength = segments.firstOrNull {
            it.hasAnnotation() &&
                it.annotation == ProtoCommands.Preedit.Segment.Annotation.HIGHLIGHT
        }?.takeIf { it.hasKey() }?.key?.let { key ->
            key.codePointCount(0, key.length)
        }
        return State(
            composition = segments.joinToString("") { if (it.hasValue()) it.value else "" },
            candidates = candidateItems.map { it.value },
            result = if (output.hasResult()) output.result.value else null,
            focusedCandidateIndex = candidateWindow?.focusedIndex ?: -1,
            candidateItems = candidateItems,
            preeditCursor = output.preedit.takeIf { it.hasCursor() }?.cursor,
            consumed = output.hasConsumed() && output.consumed,
            phase = Phase.CONVERSION,
            compositionKeyLength = keyText?.codePointCount(0, keyText.length),
            focusedSegmentKeyLength = focusedKeyLength,
            compositionKeyText = keyText,
            segmentCount = segments.size
        )
    }

    private fun sendUndoForPartialCommitRecovery(): State {
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
            .setId(sessionId)
            .setCommand(
                ProtoCommands.SessionCommand.newBuilder()
                    .setType(ProtoCommands.SessionCommand.CommandType.UNDO)
                    .build()
            ).build()
        return toState(eval(ProtoCommands.Command.newBuilder().setInput(input).build()))
    }

    fun submitCandidate(candidateId: Int): State {
        ensureInitialized()
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
            .setId(sessionId)
            .setCommand(ProtoCommands.SessionCommand.newBuilder()
                .setType(ProtoCommands.SessionCommand.CommandType.SUBMIT_CANDIDATE)
                .setId(candidateId).build()).build()
        val response = eval(ProtoCommands.Command.newBuilder().setInput(input).build())
        val parsedState = toState(response)
        val state = candidateSubmitContinuationState(response, parsedState)
        return state
    }

    /** Preserve only an explicit raw Mozc continuation returned by candidate submission. */
    private fun candidateSubmitContinuationState(
        response: ProtoCommands.Command,
        parsedState: State
    ): State {
        val output = response.output
        if (!output.hasConsumed() || !output.consumed || !output.hasResult() ||
            output.result.value.isEmpty() || !output.hasPreedit()
        ) return parsedState

        val segments = output.preedit.segmentList
        val continuationSegments = segments.filter { it.hasValue() && it.value.isNotEmpty() }
        if (continuationSegments.isEmpty()) return parsedState

        val highlight = ProtoCommands.Preedit.Segment.Annotation.HIGHLIGHT
        val underline = ProtoCommands.Preedit.Segment.Annotation.UNDERLINE
        val phase = when {
            continuationSegments.any { it.hasAnnotation() && it.annotation == highlight } -> Phase.CONVERSION
            continuationSegments.any { it.hasAnnotation() && it.annotation == underline } -> Phase.COMPOSITION
            else -> Phase.UNKNOWN
        }
        val keyText = segments
            .takeIf { it.isNotEmpty() && it.all { segment -> segment.hasKey() } }
            ?.joinToString(separator = "") { it.key }
        val keyLength = keyText?.codePointCount(0, keyText.length)
        val focusedSegmentKeyLength = continuationSegments.firstOrNull {
            it.hasAnnotation() && it.annotation == highlight
        }?.takeIf { it.hasKey() }?.key?.let { key ->
            key.codePointCount(0, key.length)
        }

        activeComposition = true
        return parsedState.copy(
            composition = continuationSegments.joinToString(separator = "") { it.value },
            phase = phase,
            compositionKeyLength = keyLength,
            focusedSegmentKeyLength = focusedSegmentKeyLength,
            compositionKeyText = keyText,
            segmentCount = continuationSegments.size
        )
    }

    fun highlightCandidate(candidateId: Int): State {
        ensureInitialized()
        val activeBefore = activeComposition
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
            .setId(sessionId)
            .setCommand(
                ProtoCommands.SessionCommand.newBuilder()
                    .setType(ProtoCommands.SessionCommand.CommandType.HIGHLIGHT_CANDIDATE)
                    .setId(candidateId)
                    .build()
            ).build()
        val response = eval(ProtoCommands.Command.newBuilder().setInput(input).build())
        val parsedState = toState(
            response,
            preferAllCandidateWords = true
        )
        val highlightedComposition = highlightedComposition(
            response = response,
            candidateId = candidateId
        )
        val state = if (parsedState.consumed &&
            parsedState.phase == Phase.CONVERSION &&
            !highlightedComposition.isNullOrEmpty()
        ) {
            parsedState.copy(composition = highlightedComposition)
        } else parsedState
        if (!state.consumed) activeComposition = activeBefore
        return state
    }

    /**
     * Mozc can move CandidateWindow focus while leaving the highlighted
     * preedit segment's value unchanged in the same response. For a
     * HIGHLIGHT_CANDIDATE response only, project the selected candidate value
     * onto that segment so the editor-facing composition follows native focus.
     */
    private fun highlightedComposition(
        response: ProtoCommands.Command,
        candidateId: Int
    ): String? {
        val output = response.output
        if (!output.hasPreedit()) return null
        val segments = output.preedit.segmentList
        val highlightedIndex = segments.indexOfFirst {
            it.hasAnnotation() &&
                it.annotation == ProtoCommands.Preedit.Segment.Annotation.HIGHLIGHT
        }
        if (highlightedIndex == -1) return null

        val candidateWindow = output.takeIf { it.hasCandidateWindow() }?.candidateWindow
        val focusedValue = candidateWindow
            ?.takeIf { it.hasFocusedIndex() }
            ?.let { window ->
                window.candidateList.firstOrNull { it.index == window.focusedIndex }?.value
            }
        val requestedValue = candidateWindow?.candidateList
            ?.firstOrNull { it.id == candidateId }
            ?.value
            ?: output.takeIf { it.hasAllCandidateWords() }
                ?.allCandidateWords
                ?.candidatesList
                ?.firstOrNull { it.id == candidateId }
                ?.value
        val selectedValue = focusedValue ?: requestedValue ?: return null

        return segments.mapIndexed { index, segment ->
            if (index == highlightedIndex) selectedValue
            else if (segment.hasValue()) segment.value else ""
        }.joinToString("")
    }

    private fun sendKey(
        keyString: String? = null,
        specialKey: ProtoCommands.KeyEvent.SpecialKey? = null,
        keyCode: Int? = null,
        modifiers: List<ProtoCommands.KeyEvent.ModifierKey> = emptyList()
    ): State {
        ensureInitialized()
        val key = ProtoCommands.KeyEvent.newBuilder()
            .setInputStyle(ProtoCommands.KeyEvent.InputStyle.AS_IS)
            .apply {
                if (keyString != null) setKeyString(keyString)
                if (keyCode != null) setKeyCode(keyCode)
                if (specialKey != null) setSpecialKey(specialKey)
                if (modifiers.isNotEmpty()) addAllModifierKeys(modifiers)
            }.build()
        val input = ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_KEY)
            .setId(sessionId)
            .setKey(key)
            .build()
        val response = eval(ProtoCommands.Command.newBuilder().setInput(input).build())
        val state = toState(
            response,
            preserveActiveIfBackspaceOmitted = specialKey == ProtoCommands.KeyEvent.SpecialKey.BACKSPACE,
            preferAllCandidateWords = specialKey == ProtoCommands.KeyEvent.SpecialKey.SPACE
        )
        return state
    }

    private fun eval(command: ProtoCommands.Command): ProtoCommands.Command =
        ProtoCommands.Command.parseFrom(MozcBackend.evalCommand(command.toByteArray()))

    private fun toState(
        command: ProtoCommands.Command,
        preserveActiveIfBackspaceOmitted: Boolean = false,
        preferAllCandidateWords: Boolean = false
    ): State {
        val activeBefore = activeComposition
        val output = command.output
        val rawPreedit = if (output.hasPreedit()) {
            output.preedit.segmentList.joinToString(separator = "") { it.value }
        } else ""
        val candidateWindow = if (output.hasCandidateWindow()) output.candidateWindow else null
        val windowCandidates = candidateWindow?.candidateList.orEmpty()
        val focusedCandidate = candidateWindow?.takeIf { it.hasFocusedIndex() }
            ?.focusedIndex?.let { focusedIndex ->
                windowCandidates.firstOrNull { it.index == focusedIndex }?.value
            }
        // During conversion Mozc can clear preedit while exposing the focused
        // candidate in CandidateWindow. Keep that candidate composing so the
        // first Space does not visually erase the text.
        val preedit = rawPreedit.ifEmpty { focusedCandidate.orEmpty() }
        val windowItems = candidateWindow?.candidateList.orEmpty().map {
            CandidateItem(it.value, it.id, it.index)
        }
        val allWordItems = if (output.hasAllCandidateWords()) {
            output.allCandidateWords.candidatesList.map {
                CandidateItem(it.value, it.id, it.index)
            }
        } else emptyList()
        // HIGHLIGHT_CANDIDATE returns a partial CandidateWindow around the new
        // focus while allCandidateWords still contains the complete list.
        // Keep the window metadata for focus/projection, but expose the full
        // list to the Candidate Bar for this command only.
        val useAllCandidateWords = preferAllCandidateWords && allWordItems.isNotEmpty()
        val candidateItems = when {
            useAllCandidateWords -> allWordItems
            windowItems.isNotEmpty() -> windowItems
            else -> allWordItems
        }
        val candidates = candidateItems.map { it.value }
        val result = if (output.hasResult()) output.result.value else null
        activeComposition = when {
            result != null -> false
            rawPreedit.isNotEmpty() || windowCandidates.isNotEmpty() -> true
            output.hasPreedit() || output.hasCandidateWindow() -> false
            // A BACKSPACE response with no preedit, candidates, result, or
            // explicit fields is Mozc's terminal empty-composition response.
            // Do not retain the previous active state in this command context.
            preserveActiveIfBackspaceOmitted -> false
            else -> activeBefore
        }
        val preeditCursor = output.takeIf { it.hasPreedit() }
            ?.preedit
            ?.takeIf { it.hasCursor() }
            ?.cursor
        val preeditSegments = if (output.hasPreedit()) output.preedit.segmentList else emptyList()
        val segmentCount = preeditSegments.size
        val highlightAnnotation = ProtoCommands.Preedit.Segment.Annotation.HIGHLIGHT
        val underlineAnnotation = ProtoCommands.Preedit.Segment.Annotation.UNDERLINE
        val hasHighlight = preeditSegments.any {
            it.hasAnnotation() && it.annotation == highlightAnnotation
        }
        val hasUnderline = preeditSegments.any {
            it.hasAnnotation() && it.annotation == underlineAnnotation
        }
        val compositionKeyText = preeditSegments
            .takeIf { output.hasPreedit() && it.isNotEmpty() && it.all { segment -> segment.hasKey() } }
            ?.joinToString(separator = "") { it.key }
        val compositionKeyLength = compositionKeyText
            ?.codePointCount(0, compositionKeyText.length)
        val focusedSegmentKeyLength = preeditSegments
            .firstOrNull { it.hasAnnotation() && it.annotation == highlightAnnotation }
            ?.takeIf { it.hasKey() }
            ?.key
            ?.let { it.codePointCount(0, it.length) }
        val phase = when {
            !activeComposition -> Phase.INACTIVE
            !output.hasPreedit() -> Phase.UNKNOWN
            hasHighlight -> Phase.CONVERSION
            hasUnderline -> Phase.COMPOSITION
            else -> Phase.UNKNOWN
        }
        val state = State(
            composition = preedit,
            candidates = candidates,
            result = result,
            focusedCandidateIndex = candidateWindow?.focusedIndex ?: -1,
            candidateItems = candidateItems,
            preeditCursor = preeditCursor,
            consumed = output.hasConsumed() && output.consumed,
            phase = phase,
            compositionKeyLength = compositionKeyLength,
            focusedSegmentKeyLength = focusedSegmentKeyLength,
            compositionKeyText = compositionKeyText,
            segmentCount = segmentCount
        )
        return state
    }

    enum class Phase {
        INACTIVE,
        COMPOSITION,
        CONVERSION,
        UNKNOWN
    }

    data class State(
        val composition: String,
        val candidates: List<String>,
        val result: String?,
        val focusedCandidateIndex: Int = -1,
        val candidateItems: List<CandidateItem> = emptyList(),
        val preeditCursor: Int? = null,
        val consumed: Boolean = false,
        val phase: Phase = Phase.UNKNOWN,
        val compositionKeyLength: Int? = null,
        val focusedSegmentKeyLength: Int? = null,
        val compositionKeyText: String? = null,
        val segmentCount: Int = 0
    )

}
