/*
 * Charlatano is a premium CS:GO cheat ran on the JVM.
 * Copyright (C) 2016 Thomas Nappo, Jonathan Beaudoin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.charlatano.scripts

import co.paralleluniverse.strands.Strand
import com.charlatano.*
import com.charlatano.game.CSGO.scaleFormDLL
import com.charlatano.game.angle
import com.charlatano.game.clientState
import com.charlatano.game.entities
import com.charlatano.game.entity.*
import com.charlatano.game.me
import com.charlatano.game.offsets.ScaleFormOffsets
import com.charlatano.utils.*
import org.jire.arrowhead.keyPressed
import java.lang.Math.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val target = AtomicLong(-1)
val perfect = AtomicBoolean(false)

val bone = AtomicInteger(AIM_BONE)

fun fovAim() = every(AIM_DURATION) {
	val aim = keyPressed(1)
	val forceAim = keyPressed(FORCE_AIM_KEY)
	val pressed = aim or forceAim
	var currentTarget = target.get()
	
	if (!pressed || scaleFormDLL.boolean(ScaleFormOffsets.bCursorEnabled)) {
		target.set(-1L)
		return@every
	}
	
	val weapon = me.weapon()
	if (!weapon.pistol && !weapon.automatic && !weapon.shotgun) {
		target.set(-1L)
		return@every
	}
	
	val currentAngle = clientState.angle()
	
	val position = me.position()
	if (currentTarget < 0) {
		currentTarget = findTarget(position, currentAngle, aim)
		if (currentTarget < 0)
			return@every
		target.set(currentTarget)
	}
	
	if (me.dead() || currentTarget.dead() || currentTarget.dormant()
			|| !currentTarget.spotted() || currentTarget.team() == me.team()) {
		target.set(-1L)
		Strand.sleep(200 + nextLong(350))
		return@every
	}
	
	if (!currentTarget.onGround() || !me.onGround()) return@every
	
	val boneID = bone.get()
	val bonePosition = Vector(
			currentTarget.bone(0xC, boneID),
			currentTarget.bone(0x1C, boneID),
			currentTarget.bone(0x2C, boneID))
	
	val dest = calculateAngle(me, bonePosition)
	if (AIM_ASSIST_MODE) dest.finalize(currentAngle, AIM_ASSIST_STRICTNESS / 100.0)

	val distance = position.distanceTo(bonePosition)
	var sensMultiplier = AIM_STRICTNESS
	
	if (distance > AIM_STRICTNESS_BASELINE_DISTANCE) {
		val amountOver = AIM_STRICTNESS_BASELINE_DISTANCE / distance
		sensMultiplier *= (amountOver * AIM_STRICTNESS_BASELINE_MODIFIER)
	}
	
	val aimSpeed = AIM_SPEED_MIN + nextInt(AIM_SPEED_MAX - AIM_SPEED_MIN)
	aim(currentAngle, dest, aimSpeed, sensMultiplier = sensMultiplier, perfect = perfect.getAndSet(false))
}

private fun findTarget(position: Angle, angle: Angle, allowPerfect: Boolean, lockFOV: Int = AIM_FOV): Player {
	var closestDelta = Double.MAX_VALUE
	var closetPlayer: Player? = null
	
	var closestFOV = Double.MAX_VALUE
	
	entities(EntityType.CCSPlayer) {
		val entity = it.entity
		if (entity <= 0) return@entities
		if (entity == me || entity.team() == me.team()) return@entities
		
		if (me.dead() || entity.dead() || !entity.spotted() || entity.dormant()) return@entities
		
		val ePos: Angle = Vector(entity.bone(0xC), entity.bone(0x1C), entity.bone(0x2C))
		val distance = position.distanceTo(ePos)
		
		val dest = calculateAngle(me, ePos)
		
		val pitchDiff = abs(angle.x - dest.x)
		val yawDiff = abs(angle.y - dest.y)
		val delta = abs(sin(toRadians(yawDiff)) * distance)
		val fovDelta = abs((sin(toRadians(pitchDiff)) + sin(toRadians(yawDiff))) * distance)
		
		if (delta <= lockFOV && delta < closestDelta) {
			closestDelta = delta
			closetPlayer = entity
			closestFOV = fovDelta
		}
	}
	
	if (closestDelta == Double.MAX_VALUE) return -1
	
	if (closetPlayer != null) {
		
		if (PERFECT_AIM && allowPerfect && closestFOV <= PERFECT_AIM_FOV &&
				nextInt(100 + 1) <= PERFECT_AIM_CHANCE)
			perfect.set(true)
		
		return closetPlayer!!
	}
	
	return -1
}