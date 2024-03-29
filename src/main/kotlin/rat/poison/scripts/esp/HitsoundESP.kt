package rat.poison.scripts.esp

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import rat.poison.game.CSGO.csgoEXE
import rat.poison.game.me
import rat.poison.game.netvars.NetVarOffsets.m_totalHitsOnServer
import rat.poison.utils.every
import rat.poison.curSettings
import rat.poison.strToBool

var totalHits = 0
var opened = false
lateinit var hitSound : Sound

fun hitSoundEsp() = every(4) {
    if (!curSettings["ENABLE_HITSOUND"]!!.strToBool()) return@every

    val curHits = csgoEXE.int(me + m_totalHitsOnServer)

    if (!opened) {
        try {
            hitSound = Gdx.audio.newSound(Gdx.files.internal("settings\\hitsound.mp3"))
            opened = true
            totalHits = curHits
        } catch (ex: NullPointerException){}
    }
    else if (curHits == 0) {
        totalHits = 0
    }
    else if (totalHits != curHits)
    {
        hitSound.play(curSettings["HITSOUND_VOLUME"]!!.toDouble().toFloat())
        totalHits = curHits
    }
}