package com.github.xepozz.fs_info.actions

import com.intellij.openapi.actionSystem.ToggleOptionAction
import com.intellij.openapi.project.DumbAware

class ToggleBytesAction : DumbAware, ToggleOptionAction({
    object : AbstractToggleAction(it) {
        override val option = settings::showBytes
    }
})

class ToggleKBytesAction : DumbAware, ToggleOptionAction({
    object : AbstractToggleAction(it) {
        override val option = settings::showKBytes
    }
})

class ToggleMBytesAction : DumbAware, ToggleOptionAction({
    object : AbstractToggleAction(it) {
        override val option = settings::showMBytes
    }
})

class ToggleGBytesAction : DumbAware, ToggleOptionAction({
    object : AbstractToggleAction(it) {
        override val option = settings::showGBytes
    }
})

class ToggleLinesAction : DumbAware, ToggleOptionAction({
    object : AbstractToggleAction(it) {
        override val option = settings::showLines
    }
})

class ToggleDirectoryItemsAction : DumbAware, ToggleOptionAction({
    object : AbstractToggleAction(it) {
        override val option = settings::showDirectoryItemsAmount
    }
})

class ToggleDirectorySizeAction : DumbAware, ToggleOptionAction({
    object : AbstractToggleAction(it) {
        override val option = settings::showDirectorySize
    }
})
