package rat.poison.scripts

import org.jire.arrowhead.keyPressed
import rat.poison.curSettings
import rat.poison.game.CSGO
import rat.poison.game.angle
import rat.poison.game.clientState
import rat.poison.game.entity.onGround
import rat.poison.game.me
import rat.poison.game.offsets.ClientOffsets
import rat.poison.strToBool
import rat.poison.utils.every
import java.awt.event.KeyEvent

var lastAngY = 0.0

fun autoStrafe() = every(4) {
    if (curSettings["AUTO_STRAFE"]!!.strToBool()) {
        val curAngY = clientState.angle().y

        if (!me.onGround()) {
            if (!keyPressed(KeyEvent.VK_A) && !keyPressed(KeyEvent.VK_D)) {
                if (curAngY > lastAngY) {
                    CSGO.clientDLL[ClientOffsets.dwForceLeft] = 6
                } else if (curAngY < lastAngY) {
                    CSGO.clientDLL[ClientOffsets.dwForceRight] = 6
                }
            }
        }
        lastAngY = curAngY
    }
}