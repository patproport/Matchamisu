package it.palsoftware.pastiera.inputmethod.japanese

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import android.util.Log
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands

/** Japanese mode adapter. Physical key/modifier handling remains in Pastiera. */
class JapaneseInputEngine(context: Context) {
    var active = false
    private val mozc = MozcBridge(context)
    private var latestCandidates: List<String> = emptyList()
    private var latestFocusedCandidateIndex: Int = -1
    private var latestCandidateItems: List<MozcBridge.CandidateItem> = emptyList()
    private var desiredConversionLength: Int? = null
    private var latestConfirmedPhase = MozcBridge.Phase.INACTIVE
    private var latestObservedPhase = MozcBridge.Phase.INACTIVE
    private var latestCompositionKeyLength: Int? = null
    private var latestCompositionKeyText: String? = null
    private var latestPreeditCursor: Int? = null
    private var latestSegmentCount: Int = 0
    private var latestPreeditPresent = false
    private var candidateContinuationPreedit = false
    private var editorOperationInProgress = false

    fun candidateStrings(): List<String> = latestCandidates
    fun focusedCandidateIndex(): Int = latestFocusedCandidateIndex
    fun candidateItemAt(index: Int): MozcBridge.CandidateItem? = latestCandidateItems.getOrNull(index)

    /** Read-only IME UI projection; it never changes the Mozc or editor cursor. */
    fun compositionRangePreviewText(): String? {
        if (latestConfirmedPhase != MozcBridge.Phase.COMPOSITION) return null
        val reading = latestCompositionKeyText?.takeIf { it.isNotEmpty() } ?: return null
        val totalLength = latestCompositionKeyLength?.takeIf { it > 0 } ?: return null
        if (reading.codePointCount(0, reading.length) != totalLength) return null
        val desiredLength = desiredConversionLength?.takeIf { it in 1..totalLength } ?: return null
        val boundaryOffset = reading.offsetByCodePoints(0, desiredLength)
        return "[${reading.substring(0, boundaryOffset)}]${reading.substring(boundaryOffset)}  " +
            "$desiredLength/$totalLength"
    }

    fun handleTrackpadCandidateSwipe(direction: Int, ic: InputConnection) {
        val oldFocusedCandidateIndex = latestFocusedCandidateIndex
        if (!mozc.hasActiveComposition()) return
        if (latestConfirmedPhase == MozcBridge.Phase.COMPOSITION) {
            val compositionLength = latestCompositionKeyLength
            if (compositionLength == null || compositionLength < 1) return
            val currentLength = (desiredConversionLength ?: compositionLength)
                .coerceIn(1, compositionLength)
            desiredConversionLength = (currentLength + if (direction > 0) 1 else -1)
                .coerceIn(1, compositionLength)
            return
        }
        if (latestConfirmedPhase != MozcBridge.Phase.CONVERSION) return
        if (latestCandidateItems.isEmpty()) return

        val currentPosition = latestCandidateItems.indexOfFirst {
            it.index == oldFocusedCandidateIndex
        }
        val targetPosition = if (currentPosition == -1) {
            if (direction > 0) 0 else latestCandidateItems.lastIndex
        } else {
            (currentPosition + if (direction > 0) 1 else -1)
                .coerceIn(0, latestCandidateItems.lastIndex)
        }
        val target = latestCandidateItems[targetPosition]
        if (currentPosition == targetPosition) return

        val state = try {
            mozc.highlightCandidate(target.id)
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to highlight Mozc candidate", error)
            return
        }
        if (!state.consumed) return

        recordMozcState(state)
        if (mozc.hasActiveComposition() &&
            state.phase == MozcBridge.Phase.CONVERSION &&
            state.composition.isNotEmpty()
        ) {
            ic.setComposingText(state.composition, 1)
        }
        latestCandidates = state.candidates
        latestFocusedCandidateIndex = state.focusedCandidateIndex
        latestCandidateItems = state.candidateItems
    }

    fun selectCandidateAt(index: Int, ic: InputConnection): Boolean {
        editorOperationInProgress = true
        return try {
            selectCandidateAtInternal(index, ic)
        } finally {
            editorOperationInProgress = false
        }
    }

    private fun selectCandidateAtInternal(index: Int, ic: InputConnection): Boolean {
        val item = latestCandidateItems.getOrNull(index) ?: return false
        val state = mozc.submitCandidate(item.id)
        latestCandidates = state.candidates
        latestFocusedCandidateIndex = state.focusedCandidateIndex
        latestCandidateItems = state.candidateItems
        recordMozcState(state)
        // submitCandidate restores active state only for a raw remaining preedit.
        candidateContinuationPreedit = mozc.hasActiveComposition() &&
            !state.result.isNullOrEmpty() && state.composition.isNotEmpty()
        if (!state.result.isNullOrEmpty() && state.composition.isNotEmpty()) {
            ic.beginBatchEdit()
            try {
                ic.commitText(checkNotNull(state.result), 1)
                ic.setComposingText(state.composition, 1)
            } finally {
                ic.endBatchEdit()
            }
        } else if (!state.result.isNullOrEmpty()) {
            ic.commitText(state.result, 1)
        } else if (state.composition.isNotEmpty()) {
            ic.setComposingText(state.composition, 1)
        } else {
            ic.finishComposingText()
        }
        return true
    }

    /** Prepare the Mozc session before the first printable key arrives. */
    fun prepare() {
        if (active) runCatching { mozc.ensureInitialized() }
    }

    fun hasActiveComposition(): Boolean = mozc.hasActiveComposition()
    fun isEditorOperationInProgress(): Boolean = editorOperationInProgress

    /** A confirmed remaining preedit that may outlive an editor cleared by LINE. */
    fun hasPreeditForClearedEditor(): Boolean = mozc.hasActiveComposition() &&
        latestPreeditPresent &&
        (latestObservedPhase == MozcBridge.Phase.COMPOSITION ||
            latestObservedPhase == MozcBridge.Phase.CONVERSION ||
            candidateContinuationPreedit)

    /** Exact latest Mozc phase used only for guarding vertical editor navigation. */
    fun isCompositionOrConversionPhase(): Boolean =
        latestObservedPhase == MozcBridge.Phase.COMPOSITION ||
            latestObservedPhase == MozcBridge.Phase.CONVERSION

    /** True only for the exact latest INACTIVE phase; UNKNOWN is fail-safe false. */
    fun isInactivePhase(): Boolean = latestObservedPhase == MozcBridge.Phase.INACTIVE

    fun prepareForExternalCommit(ic: InputConnection): Boolean {
        editorOperationInProgress = true
        return try {
            prepareForExternalCommitInternal(ic)
        } finally {
            editorOperationInProgress = false
        }
    }

    private fun prepareForExternalCommitInternal(ic: InputConnection): Boolean {
        val activeBefore = mozc.hasActiveComposition()
        if (!active || !activeBefore) return false
        val state = mozc.sendSpecial(ProtoCommands.KeyEvent.SpecialKey.ENTER)
        latestCandidates = state.candidates
        latestFocusedCandidateIndex = state.focusedCandidateIndex
        latestCandidateItems = state.candidateItems
        resetConversionLengthState()
        if (!state.result.isNullOrEmpty()) {
            ic.commitText(state.result, 1)
        } else {
            ic.setComposingText(state.composition, 1)
        }
        return true
    }

    fun resetForEditorSelection(ic: InputConnection) {
        mozc.resetContext()
        resetConversionLengthState()
        ic.finishComposingText()
    }

    /** Discard a preedit after the editor has independently cleared its text. */
    fun discardClearedEditorComposition() {
        mozc.resetContext()
        latestCandidates = emptyList()
        latestFocusedCandidateIndex = -1
        latestCandidateItems = emptyList()
        resetConversionLengthState()
    }

    fun reset(ic: InputConnection?) {
        if (active) runCatching { mozc.sendSpecial(ProtoCommands.KeyEvent.SpecialKey.ENTER) }
        ic?.finishComposingText()
        mozc.reset()
        resetConversionLengthState()
    }

    fun handleKey(
        key: Char?,
        keyCode: Int,
        event: KeyEvent?,
        ic: InputConnection,
        altActive: Boolean = event?.isAltPressed == true
    ): Boolean {
        editorOperationInProgress = true
        return try {
            handleKeyInternal(key, keyCode, event, ic, altActive)
        } finally {
            editorOperationInProgress = false
        }
    }

    private fun handleKeyInternal(
        key: Char?,
        keyCode: Int,
        event: KeyEvent?,
        ic: InputConnection,
        altActive: Boolean
    ): Boolean {
        if (!active || event?.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount != 0 && keyCode != KeyEvent.KEYCODE_DEL) {
            return false
        }
        if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DEL) &&
            !mozc.hasActiveComposition()) return false
        val wasActive = mozc.hasActiveComposition()
        val printableInput = keyCode != KeyEvent.KEYCODE_DEL &&
            keyCode != KeyEvent.KEYCODE_SPACE &&
            keyCode != KeyEvent.KEYCODE_ENTER &&
            keyCode != KeyEvent.KEYCODE_ESCAPE &&
            key != null && !key.isISOControl() && key != ' '
        if (keyCode == KeyEvent.KEYCODE_ENTER &&
            latestConfirmedPhase == MozcBridge.Phase.CONVERSION &&
            latestSegmentCount >= 2) {
            return handlePartialConversionCommit(ic)
        }
        if (printableInput && !moveCursorBeforePrintableIfNeeded()) {
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_SPACE && !altActive && !mozc.hasActiveComposition()) {
            ic.commitText(" ", 1)
            return true
        }
        var state = when (keyCode) {
            KeyEvent.KEYCODE_DEL -> mozc.sendSpecial(ProtoCommands.KeyEvent.SpecialKey.BACKSPACE)
            KeyEvent.KEYCODE_SPACE -> mozc.sendSpecial(ProtoCommands.KeyEvent.SpecialKey.SPACE)
            KeyEvent.KEYCODE_ENTER -> mozc.sendSpecial(ProtoCommands.KeyEvent.SpecialKey.ENTER)
            KeyEvent.KEYCODE_ESCAPE -> mozc.sendSpecial(ProtoCommands.KeyEvent.SpecialKey.ESCAPE)
            else -> if (key != null && !key.isISOControl() && key != ' ') {
                val modifiers = if (altActive) {
                    listOf(ProtoCommands.KeyEvent.ModifierKey.ALT)
                } else emptyList()
                mozc.sendText(key.toString(), modifiers)
            } else return false
        }
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            state = shrinkConversionToDesiredLength(state)
        }
        updateDesiredLengthAfterKey(state, keyCode, printableInput)
        recordMozcState(state)
        latestCandidates = state.candidates
        latestFocusedCandidateIndex = state.focusedCandidateIndex
        latestCandidateItems = state.candidateItems
        if (printableInput && !state.result.isNullOrEmpty() && state.composition.isNotEmpty()) {
            ic.beginBatchEdit()
            try {
                ic.commitText(checkNotNull(state.result), 1)
                ic.setComposingText(state.composition, 1)
            } finally {
                ic.endBatchEdit()
            }
        } else if (!state.result.isNullOrEmpty()) {
            ic.commitText(state.result, 1)
        }
        else if (keyCode == KeyEvent.KEYCODE_DEL &&
            wasActive && state.composition.isEmpty() && !mozc.hasActiveComposition()) {
            // Mozc has deleted the final preedit character. Clear the
            // editor-side composing span before ending it; finishing a
            // non-empty span would commit the deleted character.
            ic.setComposingText("", 1)
            ic.finishComposingText()
            mozc.resetContext()
            resetConversionLengthState()
        } else {
            ic.setComposingText(state.composition, 1)
        }
        return true
    }

    private fun handlePartialConversionCommit(ic: InputConnection): Boolean {
        val segmentCountBefore = latestSegmentCount
        val keyTextBefore = latestCompositionKeyText
        if (!active || !mozc.hasActiveComposition()) {
            return true
        }

        val outcome = mozc.commitFirstSegment(
            expectedKeyText = keyTextBefore,
            expectedSegmentCount = segmentCountBefore
        )
        if (!outcome.success) {
            Log.w(
                TAG,
                "Mozc partial commit rejected: reason=${outcome.failureReason ?: "unknown"} " +
                    "recoveryAttempted=${outcome.recoveryAttempted} " +
                    "recoverySucceeded=${outcome.recoverySucceeded}"
            )
            return true
        }

        val committedText = outcome.committedText ?: run {
            Log.w(TAG, "Mozc partial commit returned no committed text")
            return true
        }
        val finalState = outcome.finalState ?: run {
            Log.w(TAG, "Mozc partial commit returned no final state")
            return true
        }
        val compositionLength = finalState.compositionKeyLength
        val finalCursor = finalState.preeditCursor
        desiredConversionLength = when {
            compositionLength == null || compositionLength < 1 -> null
            finalCursor != null -> finalCursor.coerceIn(1, compositionLength)
            else -> compositionLength
        }

        recordMozcState(finalState)
        latestCandidates = finalState.candidates
        latestFocusedCandidateIndex = finalState.focusedCandidateIndex
        latestCandidateItems = finalState.candidateItems
        ic.beginBatchEdit()
        try {
            ic.commitText(committedText, 1)
            ic.setComposingText(finalState.composition, 1)
        } finally {
            ic.endBatchEdit()
        }
        return true
    }

    private fun moveCursorBeforePrintableIfNeeded(): Boolean {
        if (latestConfirmedPhase != MozcBridge.Phase.COMPOSITION) return true
        val desired = desiredConversionLength ?: return true
        val cursorBefore = latestPreeditCursor
        val compositionLength = latestCompositionKeyLength
        val compositionText = latestCompositionKeyText

        if (cursorBefore == null || compositionLength == null) {
            logMoveCursorProductionAbort(
                reason = "missing_state",
                desired = desired,
                cursorBefore = cursorBefore,
                cursorAfter = null,
                phase = latestConfirmedPhase,
                compositionLength = compositionLength
            )
            return false
        }
        if (desired !in 0..compositionLength) {
            logMoveCursorProductionAbort(
                reason = "desired_out_of_range",
                desired = desired,
                cursorBefore = cursorBefore,
                cursorAfter = null,
                phase = latestConfirmedPhase,
                compositionLength = compositionLength
            )
            return false
        }
        if (desired == cursorBefore) return true

        val moveState = mozc.sendMoveCursor(desired)
        val activeAfter = mozc.hasActiveComposition()
        val moveSucceeded = moveState.consumed &&
            moveState.phase == MozcBridge.Phase.COMPOSITION &&
            moveState.preeditCursor == desired &&
            moveState.compositionKeyLength == compositionLength &&
            moveState.compositionKeyText == compositionText &&
            activeAfter
        if (!moveSucceeded) {
            val reason = when {
                !moveState.consumed -> "not_consumed"
                moveState.phase != MozcBridge.Phase.COMPOSITION -> "phase_changed"
                moveState.preeditCursor != desired -> "cursor_mismatch"
                moveState.compositionKeyLength != compositionLength -> "length_changed"
                moveState.compositionKeyText != compositionText -> "text_changed"
                !activeAfter -> "inactive"
                else -> "unknown"
            }
            logMoveCursorProductionAbort(
                reason = reason,
                desired = desired,
                cursorBefore = cursorBefore,
                cursorAfter = moveState.preeditCursor,
                phase = moveState.phase,
                compositionLength = moveState.compositionKeyLength
            )
            return false
        }

        recordMozcState(moveState)
        return true
    }

    private fun logMoveCursorProductionAbort(
        reason: String,
        desired: Int,
        cursorBefore: Int?,
        cursorAfter: Int?,
        phase: MozcBridge.Phase,
        compositionLength: Int?
    ) {
        Log.w(
            TAG,
            "Mozc cursor move aborted: reason=$reason desired=$desired " +
                "cursorBefore=${cursorBefore ?: "unset"} cursorAfter=${cursorAfter ?: "unset"} " +
                "phase=$phase compositionKeyLength=${compositionLength ?: "unset"}"
        )
    }

    private fun shrinkConversionToDesiredLength(
        originalState: MozcBridge.State
    ): MozcBridge.State {
        if (originalState.phase != MozcBridge.Phase.CONVERSION) return originalState
        val originalLength = originalState.compositionKeyLength ?: return originalState
        if (originalLength < 1) return originalState
        val requestedLength = desiredConversionLength ?: return originalState
        val desiredLength = requestedLength.coerceIn(1, originalLength)
        desiredConversionLength = desiredLength
        var currentState = originalState
        var focusedLength = originalState.focusedSegmentKeyLength ?: return originalState
        val iterationLimit = (originalLength - 1).coerceAtLeast(0)
        var iteration = 0

        while (focusedLength > desiredLength && iteration < iterationLimit) {
            val nextState = mozc.sendSegmentResize(right = false)
            iteration++
            val nextFocusedLength = nextState.focusedSegmentKeyLength
            val madeProgress = nextState.consumed &&
                nextState.phase == MozcBridge.Phase.CONVERSION &&
                nextFocusedLength != null &&
                nextFocusedLength < focusedLength
            if (!madeProgress) break
            currentState = nextState
            focusedLength = checkNotNull(nextFocusedLength)
        }
        return currentState
    }

    private fun updateDesiredLengthAfterKey(
        state: MozcBridge.State,
        keyCode: Int,
        printableInput: Boolean
    ) {
        if (state.phase == MozcBridge.Phase.INACTIVE || state.result != null) {
            resetConversionLengthState()
            return
        }
        val compositionLength = state.compositionKeyLength
        when {
            printableInput && state.phase == MozcBridge.Phase.COMPOSITION &&
                compositionLength != null && compositionLength > 0 -> {
                desiredConversionLength = state.preeditCursor
                    ?.coerceIn(0, compositionLength)
                    ?: (desiredConversionLength ?: compositionLength)
                        .coerceIn(1, compositionLength)
            }
            keyCode == KeyEvent.KEYCODE_DEL &&
                state.phase == MozcBridge.Phase.COMPOSITION &&
                compositionLength != null && compositionLength > 0 -> {
                desiredConversionLength = (desiredConversionLength ?: compositionLength)
                    .coerceIn(1, compositionLength)
            }
        }
    }

    private fun recordMozcState(state: MozcBridge.State) {
        candidateContinuationPreedit = false
        latestPreeditPresent = state.composition.isNotEmpty()
        latestObservedPhase = state.phase
        when (state.phase) {
            MozcBridge.Phase.INACTIVE -> resetConversionLengthState()
            MozcBridge.Phase.COMPOSITION,
            MozcBridge.Phase.CONVERSION -> {
                latestConfirmedPhase = state.phase
                state.preeditCursor?.let { latestPreeditCursor = it }
                latestSegmentCount = state.segmentCount
                state.compositionKeyLength?.let { latestCompositionKeyLength = it }
                latestCompositionKeyText = state.compositionKeyText
            }
            MozcBridge.Phase.UNKNOWN -> latestCompositionKeyText = null
        }
    }

    private fun resetConversionLengthState() {
        candidateContinuationPreedit = false
        latestPreeditPresent = false
        desiredConversionLength = null
        latestConfirmedPhase = MozcBridge.Phase.INACTIVE
        latestObservedPhase = MozcBridge.Phase.INACTIVE
        latestCompositionKeyLength = null
        latestCompositionKeyText = null
        latestPreeditCursor = null
        latestSegmentCount = 0
    }

    companion object { private const val TAG = "PastieraTrace" }
}

