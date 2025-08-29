package ponder.ember.ui

import kabinet.clients.KokoroKmpClient
import kabinet.model.SpeechVoice
import pondui.WavePlayer
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel

class WriterModel(
    private val wavePlayer: WavePlayer = WavePlayer(),
    private val kokoro: KokoroKmpClient = KokoroKmpClient()
): StateModel<WriterState>() {
    override val state = ModelState(WriterState())

    fun setContent(content: String) = setState { it.copy(content = content) }

    fun play() {
        ioLaunch {
            val bytes = kokoro.getMessage(stateNow.content)
            wavePlayer.play(bytes)
            println("finish")
        }
    }

    fun setVoice(voice: SpeechVoice) = setState { it.copy(voice = voice) }
}

data class WriterState(
    val content: String = "",
    val voice: SpeechVoice = SpeechVoice.Sky
)
