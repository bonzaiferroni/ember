package ponder.ember.ui

import androidx.lifecycle.viewModelScope
import kabinet.clients.KokoroKmpClient
import kabinet.clients.OllamaClient
import kabinet.model.SpeechVoice
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pondui.WavePlayer
import pondui.ui.core.ModelState
import pondui.ui.core.StateModel

class ZenWriterModel(
    private val wavePlayer: WavePlayer = WavePlayer(),
    private val kokoro: KokoroKmpClient = KokoroKmpClient(),
    private val ollama: OllamaClient = OllamaClient(),
    // private val stableDiff: StableDiffClient = StableDiffClient()
): StateModel<WriterState>() {
    override val state = ModelState(WriterState())

    fun setContent(content: String) = setState { it.copy(content = content) }

    fun play() {
        ioLaunch {
            val bytes = kokoro.getMessage(stateNow.content, stateNow.voice)
            wavePlayer.play(bytes)
            println("finish")
        }
    }

    fun setVoice(voice: SpeechVoice) = setState { it.copy(voice = voice) }

    fun pause() {
        wavePlayer.pause()
    }

    fun prompt() {
        viewModelScope.launch {
//            val bytes = stableDiff.prompt(stateNow.content)?.images?.firstOrNull()?.let {
//                Base64.decode(it.substringAfter(","))
//            }
//             println(bytes?.size)
//             setState { it.copy(image = bytes) }
            val response = ollama.streamPrompt(stateNow.content) { chunk ->
                coroutineScope {
                    withMain {
                        setState { it.copy(response = it.response + chunk)}
                    }
                }
            }
            if (response != null) {
                val formattedMessage = stateNow.response.replace("*", "")
                val bytes = kokoro.getMessage(formattedMessage, stateNow.voice)
                wavePlayer.play(bytes)
            }
        }
    }

    fun onWord(word: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val bytes = kokoro.getCacheMessage(word)
//            wavePlayer.play(bytes)
//        }
    }
}

data class WriterState(
    val content: String = "",
    val voice: SpeechVoice = SpeechVoice.Sky,
    val response: String = "",
//    val image: ByteArray? = null
) {

}
