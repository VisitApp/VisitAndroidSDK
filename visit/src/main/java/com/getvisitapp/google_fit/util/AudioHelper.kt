package com.getvisitapp.google_fit.util

import androidx.annotation.Keep
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch

@Keep
class AudioHelper {

    companion object {
        fun selectDevice(audioSwitch: AudioSwitch, devices: List<AudioDevice>) {
            devices.find { it is AudioDevice.Speakerphone }?.let { audioSwitch.selectDevice(it) }
        }
    }
}