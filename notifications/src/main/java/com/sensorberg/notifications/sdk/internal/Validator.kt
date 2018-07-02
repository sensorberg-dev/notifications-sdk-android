package com.sensorberg.notifications.sdk.internal

import java.util.regex.Pattern

object Validator {
	private const val VALID_INPUT_REG = "^[a-zA-Z0-9_]+"

	fun isInputValid(attributes: Map<String, String>): Boolean {
		val pattern = Pattern.compile(VALID_INPUT_REG)
		for (key in attributes.keys) {
			if (!pattern.matcher(key).matches()) {
				return false
			}
		}
		for (`val` in attributes.values) {
			if (!pattern.matcher(`val`).matches()) {
				return false
			}
		}
		return true
	}
}